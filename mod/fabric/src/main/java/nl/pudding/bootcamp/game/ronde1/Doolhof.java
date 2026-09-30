package nl.pudding.bootcamp.game.ronde1;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.KitDef;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Kompas;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.LootTabel;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.TeamKeuze;
import nl.pudding.bootcamp.core.TeamOverzicht;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.schrik.Schrik;
import nl.pudding.bootcamp.teams.Teammenu;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ronde 1: de doolhof. Iedereen start in de startruimte met de basiskit; de kisten zijn gevuld uit
 * de loot-tabel, de trapped chests zijn valkisten ({@link Valkisten}). Drie gangen eindigen in een
 * nep-uitgang, de vierde bij de poort, die na {@code /doolhof poort} minuten opengaat. Daarachter ligt
 * een afgesloten ruimte met de finishlijn (regio {@code doolhof_uit}): wie eroverheen loopt kiest in
 * het teammenu een kleur en mag daarna gewoon terug het doolhof in, om anderen te helpen of meer loot
 * te zoeken. Heeft iedereen een team, of is de timer op, dan gaat iedereen naar {@code v2}; wie dan
 * nog geen team heeft gaat naar het kleinste team.
 */
public final class Doolhof extends RondeLogica {
	public static final String POORT = "doolhof";
	/** De openingen van de startruimte: dicht tijdens de countdown, stil open bij de start van de timer. */
	public static final String START_POORT = "start";
	private static final int CHECK_ELKE_TICKS = 5;
	/** Na zoveel ticks opent het teammenu opnieuw voor wie hem zonder keuze sloot. */
	private static final int MENU_OPNIEUW_NA = 40;

	private boolean poortOpen;
	/** Het ingestelde moment van de poort is geweest (dan opent hij niet nog eens vanzelf). */
	private boolean poortMoment;
	private boolean hintGegeven;
	private boolean timerGestart;
	private int spelersBijStart;
	/** Wie de basiskit al kreeg; wie later binnenkomt krijgt hem bij het inloggen. */
	private final java.util.Set<java.util.UUID> gestart = new java.util.HashSet<>();
	private final Valkisten valkisten = new Valkisten();
	/** Wat de sidebar nu laat zien, zodat hij alleen bij een verandering opnieuw gaat. */
	private String sidebarNu;
	/** De timer is op: regio {@code doolhof_gif} is giftig tot iedereen een team heeft. */
	/** Na het doolhof verzamelt iedereen in de finishruimte; pas na {@code /doolhof naarei} is {@code v2} het verzamelpunt. */
	private static boolean naarEi;
	private boolean gifLoopt;
	private int gifSeconden;
	/** Wie zelf over de finish kwam; wie in het gif doodging en daarna koos, telt niet mee. */
	private final java.util.Set<java.util.UUID> gevonden = new java.util.HashSet<>();
	private final java.util.Set<java.util.UUID> vergiftigd = new java.util.HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.DOOLHOF;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		String fout = Kits.controleer(server, "basis");
		if (fout != null) {
			return fout;
		}
		try {
			leesLoot();
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
		fout = Spel.buitenRegio("doolhof", List.of("doolhof_start", "doolhof_finish"));
		if (fout != null) {
			return fout;
		}
		Regio uit = Spel.regio("doolhof_uit");
		if (!Spel.regio("doolhof").omhullende().bevat(uit.centerX(), uit.centerZ())) {
			return "regio doolhof_uit ligt buiten regio doolhof: niemand kan er dan komen, want de border staat om doolhof";
		}
		Punt finish = Spel.punt("doolhof_finish");
		if (Spel.regio("doolhof_gif").bevat(finish.x(), finish.z())) {
			return "punt doolhof_finish ligt in regio doolhof_gif: wie in het gif doodgaat, staat er dan meteen weer in."
					+ " Zet het punt in de finishruimte, of selecteer het gif zonder de finishruimte";
		}
		return null;
	}

	static LootTabel leesLoot() {
		Path pad = Standaardbestanden.map().resolve(LootTabel.BESTAND);
		try {
			return LootTabel.uitJson(Files.readString(pad, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalArgumentException(LootTabel.BESTAND + ": niet te lezen in " + pad + " (" + e.getMessage() + ")");
		}
	}

	@Override
	public void start(MinecraftServer server) {
		naarEi = false;
		Poorten.dichtAlsHijBestaat(server, POORT);
		// Eerst dicht, dan pas iedereen de startruimte in: niemand staat in een opening.
		Poorten.dichtAlsHijBestaat(server, START_POORT);
		Spelregels.locatorBar(server, false);
		// Het doolhof is de teamkeuze: een nieuw doolhof begint zonder teams.
		Teams.wisKeuzes();
		Spel.maakSpelers(server, false);
		List<ServerPlayer> spelers = Mc.deelnemers(server);
		spelersBijStart = spelers.size();
		spelers.forEach(s -> gestart.add(s.getUUID()));
		for (ServerPlayer s : spelers) {
			Spel.naarPunt(s, "doolhof_start");
		}
		Kits.geefAan(server, "basis", spelers);
		vulKisten(server);
		Valkisten.ruimOp(server);
		Border.zet(server, Spel.regio("doolhof"));
		toonSidebar(server);
		Aftelling.start(Regels.COUNTDOWN, "Het doolhof begint over", () -> {
			timerGestart = true;
			Poorten.openAlsHijBestaat(server, START_POORT, false);
			Spel.startTimer(Spel.instellingen().doolhofTimer() * 60);
			if (Spel.instellingen().doolhofPoort() == 0) {
				poortMoment = true;
				poortOpen(server);
			}
		});
	}

	/**
	 * Alle kisten in de doos van regio {@code doolhof}: leeg, dan gevuld uit de loot-tabel. Een
	 * trapped chest is een valkist: die blijft leeg.
	 */
	private static void vulKisten(MinecraftServer server) {
		LootTabel loot;
		try {
			loot = leesLoot();
		} catch (IllegalArgumentException e) {
			Bootcamp.LOG.error("Doolhof: {}", e.getMessage());
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		Regio.Doos doos = Spel.regio("doolhof").omhullende();
		int gevuld = 0;
		int leeg = 0;
		for (int cx = doos.min().x() >> 4; cx <= doos.max().x() >> 4; cx++) {
			for (int cz = doos.min().z() >> 4; cz <= doos.max().z() >> 4; cz++) {
				LevelChunk chunk = wereld.getChunk(cx, cz);
				for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
					BlockPos p = be.getBlockPos();
					if (!(be instanceof ChestBlockEntity kist) || !doos.bevatDoos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) {
						continue;
					}
					if (kist instanceof TrappedChestBlockEntity) {
						kist.clearContent();
						kist.setChanged();
						leeg++;
					} else {
						vul(server, kist, loot);
						gevuld++;
					}
				}
			}
		}
		Bootcamp.LOG.info("Doolhof: {} kisten gevuld, {} valkisten leeg", gevuld, leeg);
	}

	private static void vul(MinecraftServer server, ChestBlockEntity kist, LootTabel loot) {
		kist.clearContent();
		List<Integer> vrij = new ArrayList<>();
		for (int i = 0; i < kist.getContainerSize(); i++) {
			vrij.add(i);
		}
		for (KitDef.ItemRegel regel : loot.trekKist(Spel.RANDOM)) {
			if (vrij.isEmpty()) {
				break;
			}
			try {
				ItemStack stack = Items26.parse(server.registryAccess(), regel.spec(), regel.aantal());
				kist.setItem(vrij.remove(Spel.RANDOM.nextInt(vrij.size())), stack);
			} catch (CommandSyntaxException e) {
				Bootcamp.LOG.warn("Doolhof: '{}' uit {} is geen geldig item ({})", regel.spec(), LootTabel.BESTAND, e.getMessage());
			}
		}
		kist.setChanged();
	}

	// Elke tick de finishlijn, elke paar ticks de nep-uitgangen en schrikplekken

	@Override
	public void tick(MinecraftServer server) {
		if (!timerGestart) {
			return;
		}
		if (finishLijn(server)) {
			return;
		}
		if (server.getTickCount() % CHECK_ELKE_TICKS != 0) {
			return;
		}
		List<String> nep = Ronde.reeks("nep_", ConfigStore.get().regios().keySet());
		List<String> schrik = Ronde.reeks("schrik_", ConfigStore.get().regios().keySet());
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			// Ook wie al over de finish is doet gewoon mee: nep-uitgangen en schrikplekken werken ook voor hem.
			SpelerStatus st = Spel.status(s);
			for (String n : nep) {
				if (Spel.regio(n).bevat(s.getX(), s.getZ())) {
					nepUitgang(server, s);
					break;
				}
			}
			for (String n : schrik) {
				if (!st.schrikGehad.contains(n) && Spel.regio(n).bevat(s.getX(), s.getZ())) {
					st.schrikGehad.add(n);
					Schrik.op(s);
				}
			}
		}
	}

	/**
	 * Wie op de finishlijn (regio {@code doolhof_uit}) staat en nog niet klaar is, krijgt het
	 * teammenu. Elke tick, want over een lijn van één blok ren je anders heen tussen twee checks.
	 *
	 * @return {@code true} als het doolhof hiermee voorbij is
	 */
	private boolean finishLijn(MinecraftServer server) {
		Regio uit = Spel.regio("doolhof_uit");
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			SpelerStatus st = Spel.status(s);
			if (st.klaar || !uit.bevat(s.getX(), s.getZ())) {
				continue;
			}
			if (Teams.keuze(s) == null && !Teammenu.heeftOpen(s) && server.getTickCount() - st.menuDicht >= MENU_OPNIEUW_NA) {
				Teammenu.open(s, (speler, kleur) -> teamGekozen(server, speler, kleur));
			} else if (Teams.keuze(s) != null) {
				// Al een kleur (van /bc team, of van voor een herstart): meteen gefinisht.
				finish(s);
				if (iedereenKlaar(server)) {
					einde(server);
					return true;
				}
			}
		}
		return false;
	}

	private static void nepUitgang(MinecraftServer server, ServerPlayer speler) {
		ServerLevel wereld = Mc.wereld(server);
		Mc.particles(wereld, ParticleTypes.EXPLOSION_EMITTER, speler.getX(), speler.getY() + 1, speler.getZ(), 1, 0, 0);
		Mc.geluid(speler, SoundEvents.CREEPER_PRIMED, 1f, 1f);
		Mc.geluid(speler, SoundEvents.GENERIC_EXPLODE, 1f, 1f);
		List<String> grapjes = ConfigStore.get().grapjes();
		String grap = grapjes.isEmpty() ? "BOEM." : grapjes.get(Spel.RANDOM.nextInt(grapjes.size()));
		Mc.title(speler, Mc.tekst(grap, ChatFormatting.RED, ChatFormatting.BOLD), null, 0, 50, 15);
		Spel.naarPunt(speler, "doolhof_start");
	}

	/** Uit het teammenu: in dat team, chatregel voor iedereen, gefinisht. */
	private void teamGekozen(MinecraftServer server, ServerPlayer speler, Kleur kleur) {
		if (Spel.actief() != this || Spel.status(speler).klaar) {
			return;
		}
		// Opnieuw kijken: twee spelers kunnen in dezelfde tick de laatste plek kiezen.
		if (Teams.vol(server, kleur)) {
			Mc.actionbar(speler, Mc.tekst(kleur.naam() + " is net vol", ChatFormatting.RED));
			Spel.status(speler).menuDicht = -1000;
			return;
		}
		Teams.kies(server, speler, kleur);
		Spel.zetRol(server, speler, Rol.SPELER);
		Mc.chatAllen(server, Component.empty()
				.append(Mc.tekst(Mc.naam(speler), Mc.kleur(kleur)))
				.append(Mc.tekst(" zit in " + kleur.naam() + " (" + Teams.aantal(kleur) + "/" + Teams.maximum(server) + ")", ChatFormatting.GRAY)));
		finish(speler);
		toonSidebar(server);
		if (iedereenKlaar(server)) {
			einde(server);
		}
	}

	/** Over de finish met een team: hij blijft waar hij is en mag terug het doolhof in. */
	private void finish(ServerPlayer speler) {
		Spel.status(speler).klaar = true;
		if (!vergiftigd.contains(speler.getUUID())) {
			gevonden.add(speler.getUUID());
		}
		Kleur kleur = Teams.keuze(speler);
		Mc.geluid(speler, SoundEvents.PLAYER_LEVELUP, 1f, 1f);
		Mc.title(speler, Mc.tekst("GEFINISHT", ChatFormatting.GREEN, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("Je zit in " + kleur.naam(), Mc.kleur(kleur)))
						.append(Mc.tekst(" · je mag terug het doolhof in", ChatFormatting.WHITE)), 0, 60, 15);
	}

	private static boolean iedereenKlaar(MinecraftServer server) {
		List<ServerPlayer> spelers = Spel.levend(server, Rol.SPELER);
		return !spelers.isEmpty() && spelers.stream().allMatch(s -> Spel.status(s).klaar);
	}

	/**
	 * Een speler opent een trapped chest in het doolhof. {@code true}: de val ging af en de kist
	 * gaat niet open.
	 */
	boolean valkist(ServerPlayer speler, BlockPos pos, BlockState state) {
		SpelerStatus st = Spel.status(speler);
		if (!timerGestart || st.rol != Rol.SPELER) {
			return false;
		}
		return valkisten.open(speler, pos, state);
	}

	// Elke seconde: poort, hint, bossbar

	@Override
	public void seconde(MinecraftServer server) {
		Teammenu.ververs(server);
		// Ook /bc team en wie in- of uitlogt (hoofd erbij of eraf).
		toonSidebar(server);
		if (gifLoopt) {
			gif(server);
			return;
		}
		if (!timerGestart || !Spel.timerLoopt()) {
			return;
		}
		Instellingen i = Spel.instellingen();
		int gespeeld = Spel.gespeeld();
		if (!poortMoment && gespeeld >= i.doolhofPoort() * 60) {
			poortMoment = true;
			if (!poortOpen) {
				poortOpen(server);
			}
		}
		if (!hintGegeven && gespeeld >= i.doolhofHint() * 60) {
			hint(server);
		}
		int timer = Spel.timer();
		// Alleen de totale tijd: niemand hoeft te weten wanneer de uitgang opengaat.
		BossEvent.BossBarColor kleur = timer <= Regels.LAATSTE_MINUUT ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN;
		Bossbar.zet(BossbarTekst.doolhof(timer), kleur, Spel.timerDeel());
		laatsteTellen(server, timer);
	}

	/** De laatste tien seconden groot in beeld in rood, met een tik per tel. */
	public static void laatsteTellen(MinecraftServer server, int timer) {
		if (timer > 0 && timer <= Regels.LAATSTE_TELLEN) {
			for (ServerPlayer s : Mc.spelers(server)) {
				Mc.title(s, Mc.tekst(String.valueOf(timer), ChatFormatting.RED, ChatFormatting.BOLD), null, 0, 22, 3);
				Mc.geluid(s, SoundEvents.NOTE_BLOCK_HAT, 1f, 1f);
			}
		}
	}

	/**
	 * {@code /doolhof poort open}, en vanzelf na de ingestelde minuten. Hoorn en title alleen met
	 * {@code /doolhof poortmelding aan}.
	 */
	public void poortOpen(MinecraftServer server) {
		poortOpen = true;
		boolean melding = Spel.instellingen().poortMelding();
		Poorten.openAlsHijBestaat(server, POORT, melding);
		if (melding) {
			Mc.titleAllen(server, Mc.tekst("DE UITGANG IS OPEN", ChatFormatting.GREEN, ChatFormatting.BOLD), null);
		}
	}

	/** {@code /doolhof poort dicht}. Voor het ingestelde moment gaat hij dan alsnog vanzelf open. */
	public void poortDicht(MinecraftServer server) {
		poortOpen = false;
		Poorten.dichtAlsHijBestaat(server, POORT);
	}

	private void hint(MinecraftServer server) {
		hintGegeven = true;
		String tekst = Spel.instellingen().hinttekst();
		if (tekst == null) {
			Regio doolhof = Spel.regio("doolhof");
			Regio uit = Spel.regio("doolhof_uit");
			String kant = Kompas.richting(uit.centerX() - doolhof.centerX(), uit.centerZ() - doolhof.centerZ());
			tekst = "De uitgang ligt aan de " + kant + "kant";
		}
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (!Spel.status(s).klaar) {
				Mc.title(s, Mc.tekst("HINT", ChatFormatting.YELLOW, ChatFormatting.BOLD), Mc.tekst(tekst, ChatFormatting.WHITE), 10, 80, 20);
			}
		}
	}

	/**
	 * De teams rechts in beeld: per kleur {@code Rood 3/5} en daaronder wie erin zit, met zijn hoofd
	 * als hij online is. Elke seconde bekeken, maar alleen opnieuw gestuurd als er iets veranderde.
	 */
	private void toonSidebar(MinecraftServer server) {
		int max = Teams.maximum(server);
		List<Component> regels = new ArrayList<>();
		StringBuilder sleutel = new StringBuilder().append(max);
		for (TeamOverzicht.Regel r : TeamOverzicht.regels(Teams.namen())) {
			Kleur k = r.kleur();
			if (r.kop()) {
				regels.add(Mc.tekst(k.naam() + " " + Teams.aantal(k) + "/" + max, Mc.kleur(k), ChatFormatting.BOLD));
				sleutel.append('|').append(k.id());
				continue;
			}
			MutableComponent regel = Component.literal(" ");
			for (String naam : r.namen()) {
				ServerPlayer p = server.getPlayerList().getPlayerByName(naam);
				regel.append(" ").append(p != null ? Mc.kopEnNaam(p, Mc.kleur(k)) : Mc.tekst(naam, Mc.kleur(k)));
				sleutel.append(',').append(naam).append(p != null ? '+' : '-');
			}
			regels.add(regel);
		}
		if (!sleutel.toString().equals(sidebarNu)) {
			sidebarNu = sleutel.toString();
			Sidebar.toonTekst(server, "Teams", regels);
		}
	}

	/**
	 * Timer op: het doolhof is niet meteen voorbij. Regio {@code doolhof_gif} wordt giftig, tot
	 * iedereen een team heeft (of {@code /doolhof einde}).
	 */
	@Override
	public void timerOp(MinecraftServer server) {
		if (iedereenKlaar(server)) {
			einde(server);
			return;
		}
		gifLoopt = true;
		Mc.titleAllen(server, Mc.tekst("DE TIJD IS OM", ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst("Het doolhof is giftig · ga naar de finish", ChatFormatting.YELLOW), 0, 70, 15);
		Mc.geluidAllen(server, SoundEvents.ELDER_GUARDIAN_CURSE, 1f, 1f);
		Bossbar.zet(BossbarTekst.doolhofGif(), BossEvent.BossBarColor.PURPLE, 1f);
	}

	/**
	 * Elke seconde van het gif: wie in regio {@code doolhof_gif} staat krijgt Poison, en om de
	 * {@link Regels#DOOLHOF_GIF_ELKE} seconden een klap die door armor heen gaat. Poison alleen doodt
	 * niet; de klap wel.
	 */
	private void gif(MinecraftServer server) {
		gifSeconden++;
		Bossbar.zet(BossbarTekst.doolhofGif(), BossEvent.BossBarColor.PURPLE, 1f);
		Regio gif = Spel.regio("doolhof_gif");
		if (gif == null) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		boolean klap = gifSeconden % Regels.DOOLHOF_GIF_ELKE == 0;
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (!gif.bevat(s.getX(), s.getZ())) {
				continue;
			}
			// Kort, en elke seconde opnieuw: wie het gif uit loopt, is er zo weer van af.
			Mc.effect(s, MobEffects.POISON, 3, 0);
			if (klap) {
				s.hurtServer(wereld, wereld.damageSources().magic(), Regels.DOOLHOF_GIF_SCHADE);
			}
		}
	}

	/**
	 * In het doolhof ga je niet dood; gebeurt het toch, dan geheald terug naar de startruimte. Ook wie
	 * al gefinisht is: zijn team houdt hij. Na de timer (het gif) anders: zie {@link #gifDood}.
	 */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		if (gifLoopt) {
			gifDood(server, speler);
			return;
		}
		Spel.naarPunt(speler, "doolhof_start");
	}

	/**
	 * Dood tijdens het gif: al zijn spullen weg behalve het eten uit de basiskit, de rest van de
	 * basiskit terug, en naar de finishruimte ({@code doolhof_finish}). Heeft hij nog geen team, dan
	 * krijgt hij het kleinste (bij gelijk willekeurig), net als bij het einde; hij telt niet als iemand
	 * die de uitgang vond. Was hij de laatste zonder team, dan is het doolhof voorbij.
	 */
	private void gifDood(MinecraftServer server, ServerPlayer speler) {
		vergiftigd.add(speler.getUUID());
		Kits.Kit basis = null;
		try {
			basis = Kits.laad(server, "basis");
		} catch (KitDef.KitFout e) {
			Bootcamp.LOG.error("Doolhof: basiskit na het gif: {}", e.getMessage());
		}
		Set<Item> eten = new HashSet<>();
		if (basis != null) {
			for (Kits.Gevuld g : basis.items()) {
				if (g.stack().has(DataComponents.FOOD)) {
					eten.add(g.stack().getItem());
				}
			}
		}
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (!eten.contains(inv.getItem(i).getItem())) {
				inv.setItem(i, ItemStack.EMPTY);
			}
		}
		speler.containerMenu.setCarried(ItemStack.EMPTY);
		if (basis != null) {
			Kits.vulAan(speler, basis, stack -> eten.contains(stack.getItem()));
		}
		Spel.naarPunt(speler, "doolhof_finish");
		if (Teammenu.heeftOpen(speler)) {
			speler.closeContainer();
		}
		Kleur kleur = Teams.keuze(speler);
		boolean ingedeeld = kleur == null;
		if (ingedeeld) {
			kleur = TeamKeuze.kleinste(Teams.aantallen(), Spel.RANDOM);
			Teams.kies(server, speler, kleur);
			Spel.zetRol(server, speler, Rol.SPELER);
		}
		Spel.status(speler).klaar = true;
		Mc.title(speler, Mc.tekst("VERGIFTIGD", ChatFormatting.DARK_GREEN, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("Je spullen zijn weg · je zit in ", ChatFormatting.WHITE))
						.append(Mc.tekst(kleur.naam(), Mc.kleur(kleur))), 0, 60, 15);
		MutableComponent regel = Component.empty().append(Mc.tekst(Mc.naam(speler), Mc.kleur(kleur)))
				.append(Mc.tekst(" bezweek aan het gif", ChatFormatting.DARK_GREEN));
		if (ingedeeld) {
			regel.append(Mc.tekst(" en zit nu in " + kleur.naam() + " (" + Teams.aantal(kleur) + "/" + Teams.maximum(server) + ")",
					ChatFormatting.GRAY));
		}
		Mc.chatAllen(server, regel);
		toonSidebar(server);
		// Niet midden in de dood-afhandeling iedereen wegteleporteren: een tick later.
		Planner.na(1, () -> {
			if (Spel.actief() == this && iedereenKlaar(server)) {
				einde(server);
			}
		});
	}

	/** {@code /doolhof einde}: de noodknop, bijvoorbeeld als iemand in de finish blijft staan zonder te kiezen. */
	public void eindeNu(MinecraftServer server) {
		einde(server);
	}

	/** Of iedereen al naar het Ei is ({@code /doolhof naarei}); tot dan is de finishruimte het verzamelpunt. */
	public static boolean naarEi() {
		return naarEi;
	}

	/**
	 * {@code /doolhof naarei}: iedereen die meedoet van de finishruimte naar {@code v2} bij het Ei.
	 *
	 * @return {@code null} als het gelukt is, anders waarom niet
	 */
	public static String naarHetEi(MinecraftServer server) {
		if (Spel.actief() != null) {
			return "er loopt nog een ronde (" + Spel.actief().ronde().naam() + ")";
		}
		naarEi = true;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.naarPunt(s, "v2");
		}
		Mc.titleAllen(server, Mc.tekst("OP NAAR HET EI", ChatFormatting.GOLD, ChatFormatting.BOLD), null, 10, 50, 15);
		return null;
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		Spel.zetRol(server, speler, Rol.SPELER);
		speler.setGameMode(GameType.ADVENTURE);
		if (st.klaar || Teams.keuze(speler) != null) {
			// Gefinisht: hij gaat verder waar hij uitlogde, binnen of buiten het doolhof.
			st.klaar = true;
		} else {
			Spel.naarPunt(speler, "doolhof_start");
		}
		if (gestart.add(speler.getUUID())) {
			Kits.geefAan(server, "basis", List.of(speler));
		}
	}

	private void einde(MinecraftServer server) {
		int gevonden = 0;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (this.gevonden.contains(s.getUUID())) {
				gevonden++;
			}
		}
		int totaal = Math.max(spelersBijStart, Mc.deelnemers(server).size());
		Spel.einde(server);
		Bossbar.basiskamp();
		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (Teams.keuze(s) == null) {
				Kleur k = TeamKeuze.kleinste(Teams.aantallen(), Spel.RANDOM);
				Teams.kies(server, s, k);
				Spel.zetRol(server, s, Rol.SPELER);
				Mc.actionbar(s, Mc.tekst("Je zit in " + k.naam(), Mc.kleur(k), ChatFormatting.BOLD));
			}
			// Iedereen verzamelt in de finishruimte, ook wie nog in het doolhof liep; naar het Ei pas
			// met /doolhof naarei.
			Spel.status(s).klaar = true;
			Spel.naarPunt(s, "doolhof_finish");
		}
		Mc.titleAllen(server, Mc.tekst("DOOLHOF VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(gevonden + " van de " + totaal + " vonden de uitgang", ChatFormatting.WHITE), 10, 80, 20);
		toonSidebar(server);
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		return "poort " + (poortOpen ? "open" : "dicht") + ", hint " + (hintGegeven ? "geweest" : "nog niet")
				+ (gifLoopt ? ", de tijd is om: het gif loopt" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		Poorten.dichtAlsHijBestaat(server, POORT);
		Valkisten.ruimOp(server);
		for (ServerPlayer s : Mc.spelers(server)) {
			if (Teammenu.heeftOpen(s)) {
				s.closeContainer();
			}
		}
		Sidebar.weg(server);
		// Het einde tekent de eindstand daarna opnieuw; dat mag niet als "niets veranderd" wegvallen.
		sidebarNu = null;
	}
}
