package nl.pudding.bootcamp.game.ronde4;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Regeerperiodes;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.rad.RadSpel;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;
import nl.pudding.bootcamp.visuals.Zweefkroon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 4: King of the SMP. Eén leven voor iedereen. Kill de koning en je krijgt de kroon; elke
 * kroonwissel is een reset van de jacht. Wie de kroon heeft als de timer afloopt, of als er geen
 * levende hunter meer is, is finalist 1. De regels staan in docs/03.
 */
public final class King extends RondeLogica {
	private static final int AANTAL_STARTPUNTEN = 4;
	/** De hele Arena inclusief tribunes, als die regio er is; anders de vloer. */
	private static final String REGIO_BORDER = "colosseum";
	private static final int GOUD = 0xFFD700;

	private final Regeerperiodes regeerperiodes = new Regeerperiodes();
	/** De koning van dit moment; blijft staan als hij uitlogt, tot de kroon doorgaat. */
	private UUID koning;
	/** Seconden tot de kroon van een uitgelogde koning doorgaat; -1 als hij er gewoon is. */
	private int koningWeg = -1;
	private boolean timerGestart;
	/** Wie deze tick doodging: die houdt zijn doodtekst in beeld in plaats van de title voor iedereen. */
	private final Set<UUID> netDood = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.KING;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		ServerPlayer clown = Spel.clown(server);
		if (clown == null) {
			return "de uitverkorene is niet online of niet gezet (/bc uitverkoren <speler>)";
		}
		if (Mc.isStaff(clown)) {
			return "de uitverkorene staat in creative of spectator en doet dus niet mee";
		}
		return Spel.buitenRegio("vloer", "troon", "hunter_1", "hunter_2", "hunter_3", "hunter_4");
	}

	@Override
	public void start(MinecraftServer server) {
		ServerPlayer clown = Spel.clown(server);
		Spelregels.locatorBar(server, true);
		Kroon.wisHits();

		List<ServerPlayer> hunters = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			s.setGameMode(GameType.ADVENTURE);
			Mc.heal(s);
			if (s != clown) {
				Spel.zetRol(server, s, Rol.HUNTER);
				Kroon.toonOpLocator(s, false);
				// De loot uit ronde 3, of de basiskit voor wie niks heeft.
				if (s.getInventory().isEmpty()) {
					Kits.geefAan(server, "basis", List.of(s));
				}
				hunters.add(s);
			}
		}

		// Clown naar het midden: bosskit, kroon, Glowing.
		Kits.geefAan(server, "boss", List.of(clown));
		wordKoning(server, clown);
		Spel.naarPunt(clown, "troon");

		naarStartpunten(hunters);
		Regio border = Spel.regio(REGIO_BORDER) != null ? Spel.regio(REGIO_BORDER) : Spel.regio("vloer");
		Border.zet(server, border);
		toonSidebar(server);

		// Dertig seconden voorsprong: de hunters staan bevroren, de koning is vrij.
		Opstelling.start(server, hunters, Regels.OPSTELLING_START, "De jacht begint over", this::startTimer);
	}

	private void startTimer() {
		if (!timerGestart) {
			timerGestart = true;
			Spel.startTimer(ronde().duurSeconden());
		}
	}

	private void wordKoning(MinecraftServer server, ServerPlayer speler) {
		koning = speler.getUUID();
		koningWeg = -1;
		Kroon.geef(server, speler);
		regeerperiodes.nieuweKoning(Mc.naam(speler));
	}

	private static void naarStartpunten(List<ServerPlayer> hunters) {
		for (int i = 0; i < hunters.size(); i++) {
			Spel.naarPunt(hunters.get(i), "hunter_" + Regels.startpunt(i, AANTAL_STARTPUNTEN));
		}
	}

	// Dood

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		Rol rol = Spel.status(speler).rol;
		netDood.add(speler.getUUID());
		try {
			if (rol == Rol.KING) {
				UUID killer = bron.getEntity() instanceof ServerPlayer p && p != speler ? p.getUUID() : null;
				kroonDoor(server, killer, true);
			} else if (rol == Rol.HUNTER) {
				Spel.status(speler).dood = true;
				Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
				controleerEinde(server);
			}
		} finally {
			netDood.clear();
		}
	}

	/**
	 * De koning is weg (dood, of te lang uitgelogd): de kroon gaat naar de killer, anders de
	 * laatste hit, anders een willekeurige levende hunter. Is er geen hunter meer, dan blijft hij
	 * koning en is de ronde voorbij.
	 */
	private void kroonDoor(MinecraftServer server, UUID killer, boolean doodtekst) {
		List<UUID> levend = Spel.levend(server, Rol.HUNTER).stream().map(ServerPlayer::getUUID).toList();
		Optional<UUID> opvolger = Regels.kroonOpvolger(killer, Kroon.laatsteHit(koning), levend, Spel.RANDOM);
		if (opvolger.isEmpty()) {
			einde(server);
			return;
		}
		wissel(server, server.getPlayerList().getPlayer(opvolger.get()), doodtekst);
	}

	/** Elke kroonwissel is een reset: nieuwe koning in het midden, hunters terug naar de rand, doden blijven dood. */
	private void wissel(MinecraftServer server, ServerPlayer nieuw, boolean doodtekst) {
		// De ex-koning (en iedereen die door een bug ook een kroon draagt) gaat de tribune op.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s != nieuw && Spel.status(s).rol == Rol.KING) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, doodtekst);
			}
		}
		if (koning != null && server.getPlayerList().getPlayer(koning) == null) {
			// Uitgelogd: hij is uit de ronde; komt hij terug, dan als kijker op de tribune.
			SpelerStatus weg = Spel.status(koning);
			if (weg != null) {
				weg.dood = true;
				weg.rol = Rol.KIJKER;
			}
			Zweefkroon.uit(koning);
		}
		Kroon.wisHits();

		SpelerStatus st = Spel.status(nieuw);
		st.dood = false;
		st.tribunepunt = null;
		nieuw.setGameMode(GameType.ADVENTURE);
		wordKoning(server, nieuw);
		Spel.naarPunt(nieuw, "troon");
		Mc.heal(nieuw);
		repareer(nieuw);
		Kits.geefAan(server, "kroonpakket", List.of(nieuw));
		Mc.effect(nieuw, MobEffects.RESISTANCE, Regels.KONING_RESISTANCE, 1);

		List<ServerPlayer> hunters = Spel.levend(server, Rol.HUNTER);
		for (ServerPlayer h : hunters) {
			Mc.heal(h);
		}
		naarStartpunten(hunters);

		String naam = Mc.naam(nieuw);
		Mc.titleAllenBehalve(server, netDood, Mc.tekst("NIEUWE KONING", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(naam, ChatFormatting.YELLOW, ChatFormatting.BOLD));
		// Donder zonder bliksem: echte bliksem zet dingen in de fik.
		Mc.geluidAllen(server, SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 1f);
		Mc.particles(Mc.wereld(server), ColorParticleOption.create(ParticleTypes.FLASH, GOUD), nieuw.getX(), nieuw.getY() + 1, nieuw.getZ(), 1, 0, 0);
		toonSidebar(server);

		if (hunters.isEmpty()) {
			// De killer was de laatste hunter: hij is koning en er is niemand meer om te jagen.
			einde(server);
			return;
		}
		// Tien seconden waarin niemand van zijn plek kan, ook de koning niet. De timer loopt door.
		List<ServerPlayer> bevriezen = new ArrayList<>(hunters);
		bevriezen.add(nieuw);
		// Een geforceerde wissel tijdens de voorsprong van dertig seconden mag de timer niet laten verdwijnen.
		Opstelling.start(server, bevriezen, Regels.OPSTELLING_WISSEL, "De jacht gaat verder over", this::startTimer);
	}

	/** Alleen de koning krijgt zijn spullen gerepareerd; hunters slijten door de ronde heen. */
	private static void repareer(ServerPlayer speler) {
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isDamageableItem() && stack.isDamaged()) {
				stack.setDamageValue(0);
			}
		}
		speler.inventoryMenu.broadcastChanges();
	}

	/** {@code /bc kroon <speler>}: de noodknop van de ref. */
	public String forceer(MinecraftServer server, ServerPlayer nieuw) {
		if (Mc.isStaff(nieuw)) {
			return Mc.naam(nieuw) + " staat in creative of spectator en doet niet mee";
		}
		if (nieuw.getUUID().equals(koning) && Kroon.draagtKroon(nieuw)) {
			return Mc.naam(nieuw) + " is al koning";
		}
		wissel(server, nieuw, false);
		return null;
	}

	// Uitloggen en terugkomen

	@Override
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		if (Spel.status(speler).rol == Rol.KING && speler.getUUID().equals(koning)) {
			// De mod wacht dertig seconden, zichtbaar in de bossbar.
			koningWeg = Regels.KONING_UITLOG_WACHT;
			return;
		}
		super.onQuit(server, speler);
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		controleerEinde(server);
	}

	@Override
	protected boolean koningWachtLoopt(ServerPlayer speler) {
		return koningWeg >= 0 && speler.getUUID().equals(koning);
	}

	@Override
	protected void koningTerug(MinecraftServer server, ServerPlayer speler) {
		koningWeg = -1;
		Kroon.geef(server, speler);
	}

	// Klok

	@Override
	public void seconde(MinecraftServer server) {
		String naam = koning == null ? "?" : Spel.naamVan(koning);
		if (koningWeg >= 0) {
			Bossbar.zet(BossbarTekst.koningWeg(naam, koningWeg), BossEvent.BossBarColor.RED, (float) koningWeg / Regels.KONING_UITLOG_WACHT);
			if (koningWeg == 0) {
				koningWeg = -1;
				// Dezelfde kroonwissel als bij een val-dood: laatste hit, anders willekeurig.
				kroonDoor(server, null, false);
				return;
			}
			koningWeg--;
		} else {
			int tijd = Spel.timerLoopt() ? Spel.timer() : ronde().duurSeconden();
			Bossbar.zet(BossbarTekst.king(naam, tijd), BossEvent.BossBarColor.YELLOW, (float) tijd / ronde().duurSeconden());
		}
		if (Spel.timerLoopt()) {
			regeerperiodes.seconde();
			toonSidebar(server);
		}
	}

	private void toonSidebar(MinecraftServer server) {
		Sidebar.toon(server, "Regeerperiodes", regeerperiodes.periodes().stream().map(Regeerperiodes.Periode::tekst).toList());
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server);
	}

	private void controleerEinde(MinecraftServer server) {
		if (Spel.loopt() && Regels.ronde4Voorbij(Spel.timerLoopt() ? Spel.timer() : 1, Spel.levend(server, Rol.HUNTER).size())) {
			einde(server);
		}
	}

	// Einde

	private void einde(MinecraftServer server) {
		UUID finalist = koning;
		Regeerperiodes.Periode langste = regeerperiodes.langste();
		Spel.stop(server);
		Spel.zetFinalist1(finalist);

		// Wie nog op de vloer staat doet elkaar niks meer tot de commander de FFA start.
		for (ServerPlayer s : Spel.levend(server, Rol.HUNTER)) {
			Spel.zetRol(server, s, Rol.SPELER);
			Mc.heal(s);
		}

		String naam = finalist == null ? "?" : Spel.naamVan(finalist);
		ServerPlayer speler = finalist == null ? null : server.getPlayerList().getPlayer(finalist);
		if (speler != null) {
			// Finalist 1 gaat met zijn kroon de tribune op en kijkt naar de FFA.
			Kroon.maakFinalist(server, speler);
			Mc.heal(speler);
			Tribune.naarTribune(speler);
			Vuurwerk.goud(Mc.wereld(server), speler.getX(), speler.getY() + 2, speler.getZ());
		} else if (finalist != null && Spel.status(finalist) != null) {
			// Uitgelogd op het moment dat de ronde afliep: hij blijft finalist 1.
			Spel.status(finalist).rol = Rol.FINALIST;
		}
		Mc.titleAllenBehalve(server, netDood, Mc.tekst("DE KONING STAAT", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(naam + " is finalist 1", ChatFormatting.YELLOW));
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		if (langste != null) {
			server.getPlayerList().broadcastSystemMessage(
					Mc.tekst("Langste regeerperiode: " + langste.tekst(), ChatFormatting.GOLD), false);
		}
	}

	@Override
	public void end(MinecraftServer server) {
		Sidebar.weg(server);
		RadSpel.lampenUit(server);
	}
}
