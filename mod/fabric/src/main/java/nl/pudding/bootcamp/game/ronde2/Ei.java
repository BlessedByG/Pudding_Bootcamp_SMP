package nl.pudding.bootcamp.game.ronde2;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.EiBlok;
import nl.pudding.bootcamp.core.EiVerdeling;
import nl.pudding.bootcamp.core.Klassement;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.game.ronde1.Doolhof;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.schrik.Schrik;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ronde 2: het Ei. De mod zet het Ei terug zoals het is vastgelegd en strooit de puntenblokken op de
 * gewone deepslate. Iedereen begint aan het buiteneinde van een ketting, survival, met een diamond
 * pickaxe erbij. Alleen het Ei is te breken, alles zonder drop; neerzetten kan nergens. De meeste
 * punten wint.
 */
public final class Ei extends RondeLogica {
	/** Twee seconden staat er in de actionbar wat je net kreeg. */
	private static final int PLUS_TICKS = 40;
	private static final int PAARS = 0xAA00AA;

	private final Klassement stand = new Klassement();
	private final Map<UUID, Integer> plus = new HashMap<>();
	private final Map<UUID, Integer> plusTot = new HashMap<>();
	private final Map<UUID, Integer> hasteTot = new HashMap<>();
	/** Wie de lopende bevriezing veroorzaakte; alle anderen staan stil tot {@link #bevrorenTot}. */
	private UUID bevriezer;
	private int bevrorenTot;
	private boolean bevriezingLoopt;
	private boolean timerGestart;
	private int volgendeSpawn;

	public static void init() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer speler) || Mc.isStaff(speler) || !(Spel.actief() instanceof Ei ei)) {
				return true;
			}
			return ei.breek(speler, pos, state);
		});
		// Neerzetten kan in het Ei nergens: geen blokken, geen emmers.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player instanceof ServerPlayer speler && !Mc.isStaff(speler) && Spel.actief() instanceof Ei
					&& isNeerzetten(player.getItemInHand(hand))) {
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer speler && !Mc.isStaff(speler) && Spel.actief() instanceof Ei
					&& player.getItemInHand(hand).getItem() instanceof BucketItem) {
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	private static boolean isNeerzetten(ItemStack stack) {
		return stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem;
	}

	@Override
	public Ronde ronde() {
		return Ronde.EI;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		int deepslate = EiOpslag.deepslate(server);
		if (deepslate < 0) {
			return "ontbreekt: /ei vastleggen";
		}
		int totaal = Spel.instellingen().eiTotaal();
		if (totaal > deepslate) {
			return "samen " + totaal + " puntenblokken, maar het Ei heeft maar " + deepslate + " deepslate-plekken (/ei blokken)";
		}
		String fout = Kits.controleer(server, "ei");
		if (fout != null) {
			return fout;
		}
		return Spel.buitenRegio("eigebied", Spel.reeks("ei_spawn_"));
	}

	@Override
	public void start(MinecraftServer server) {
		// Eerst het Ei terug en de puntenblokken erin; dat is klaar voordat de countdown afloopt.
		EiOpslag.herstel(server, strooi(server), null);
		Spelregels.locatorBar(server, false);
		Spel.maakSpelers(server, true);
		List<String> spawns = Spel.reeks("ei_spawn_");
		List<ServerPlayer> spelers = Mc.deelnemers(server);
		for (ServerPlayer s : spelers) {
			String spawn = spawns.get(Math.floorMod(volgendeSpawn++, spawns.size()));
			Spel.status(s).eiSpawn = spawn;
			Spel.naarPunt(s, spawn);
		}
		// De pickaxe erbij, gemarkeerd: aan het eind gaat hij weer weg.
		Kits.geefAan(server, "ei", spelers, stack -> Items26.markeer(stack, Items26.EI_TAG, null));
		// Binnenin het Ei wordt het snel donker: Night Vision voor iedereen die meedoet.
		for (ServerPlayer s : spelers) {
			nachtzicht(s);
		}
		Border.zet(server, Spel.regio("eigebied"));
		toonSidebar(server);
		Aftelling.start(Regels.COUNTDOWN, "Het Ei opent over", () -> {
			timerGestart = true;
			Spel.startTimer(Spel.instellingen().eiTimer() * 60);
		});
	}

	/** Uit de vastgelegde deepslate trekt de mod de plekken voor de puntenblokken. */
	private static Map<Integer, BlockState> strooi(MinecraftServer server) {
		EiOpslag.Vastlegging v = EiOpslag.get(server);
		Map<EiBlok, int[]> getrokken = EiVerdeling.trek(v.deepslate().length, Spel.instellingen().eiBlokken(), Spel.RANDOM);
		Map<Integer, BlockState> uit = new HashMap<>();
		getrokken.forEach((blok, plekken) -> {
			BlockState state = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(blok.blok())).defaultBlockState();
			for (int p : plekken) {
				uit.put(v.deepslate()[p], state);
			}
		});
		return uit;
	}

	// Breken

	/** @return {@code true} als vanilla het blok gewoon mag breken (nooit, in het Ei) */
	private boolean breek(ServerPlayer speler, BlockPos pos, BlockState state) {
		SpelerStatus st = Spel.status(speler);
		Regio ei = Spel.regio("ei");
		if (!timerGestart || !Spel.timerLoopt() || st.rol != Rol.SPELER || ei == null
				|| !ei.omhullende().bevatDoos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			return false;
		}
		// De kettingen zijn om naar het Ei te klimmen, ook waar ze in de doos van het Ei liggen.
		if (state.getBlock() instanceof ChainBlock) {
			return false;
		}
		// Alles zonder drop: er is niks om mee te nemen en je inventory loopt niet vol.
		speler.level().removeBlock(pos, false);
		EiBlok blok = EiBlok.vanBlok(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		if (blok == null) {
			return false;
		}
		MinecraftServer server = speler.level().getServer();
		switch (blok) {
			case NETHERITE, DIAMOND, GOLD -> punten(server, speler, blok);
			case REDSTONE -> redstone(server, speler);
			case EMERALD -> emerald(server, speler);
		}
		return false;
	}

	private void punten(MinecraftServer server, ServerPlayer speler, EiBlok blok) {
		int erbij = blok.punten();
		stand.voegToe(speler.getUUID(), erbij);
		plus.put(speler.getUUID(), erbij);
		plusTot.put(speler.getUUID(), server.getTickCount() + PLUS_TICKS);
		Mc.geluid(speler, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f);
		if (blok == EiBlok.NETHERITE) {
			Mc.chatAllen(server, Component.empty().append(naam(speler))
					.append(Mc.tekst(" hakte netherite (+" + erbij + ")", ChatFormatting.LIGHT_PURPLE)));
		}
		actionbar(server, speler);
		toonSidebar(server);
	}

	private void redstone(MinecraftServer server, ServerPlayer speler) {
		if (Regels.redstoneGok(Spel.RANDOM) == Regels.Gok.HASTE) {
			Mc.effect(speler, MobEffects.HASTE, Regels.EI_HASTE, 1);
			hasteTot.put(speler.getUUID(), server.getTickCount() + Regels.EI_HASTE * 20);
			Mc.geluid(speler, SoundEvents.BEACON_POWER_SELECT, 1f, 1f);
			Mc.title(speler, Mc.tekst("HASTE", ChatFormatting.GOLD, ChatFormatting.BOLD),
					Mc.tekst(Regels.EI_HASTE + " seconden sneller hakken", ChatFormatting.YELLOW), 0, 40, 10);
			actionbar(server, speler);
			return;
		}
		// Een nieuwe bevriezing vervangt een lopende: nu is deze hakker de enige die los is.
		bevriezer = speler.getUUID();
		bevrorenTot = server.getTickCount() + Regels.EI_BEVRIEZING * 20;
		bevriezingLoopt = true;
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (s == speler) {
				Opstelling.ontdooi(s);
				s.removeEffect(MobEffects.MINING_FATIGUE);
			} else {
				Opstelling.bevries(s);
				Mc.effect(s, MobEffects.MINING_FATIGUE, Regels.EI_BEVRIEZING, 4);
			}
		}
		Mc.titleAllen(server, Mc.tekst("BEVROREN", ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("door ", ChatFormatting.WHITE)).append(naam(speler)), 0, 50, 10);
		Mc.geluidAllen(server, SoundEvents.GLASS_BREAK, 1f, 1f);
	}

	private void emerald(MinecraftServer server, ServerPlayer speler) {
		List<ServerPlayer> anderen = new ArrayList<>(Spel.levend(server, Rol.SPELER));
		anderen.remove(speler);
		if (anderen.isEmpty()) {
			Spel.melding(speler, Mc.tekst("Niemand om te laten schrikken", ChatFormatting.GRAY), 2);
			actionbar(server, speler);
			return;
		}
		ServerPlayer ander = anderen.get(Spel.RANDOM.nextInt(anderen.size()));
		Schrik.op(ander);
		Spel.melding(speler, Component.empty().append(Mc.tekst("Jumpscare naar ", ChatFormatting.GREEN)).append(naam(ander)), 2);
		actionbar(server, speler);
		UUID anderId = ander.getUUID();
		String breker = Mc.naam(speler);
		Kleur kleur = Teams.keuze(speler);
		// Als de foto wegvaagt: van wie hij kwam.
		Planner.na(Schrik.IN_BEELD, () -> {
			ServerPlayer a = server.getPlayerList().getPlayer(anderId);
			if (a != null && Spel.actief() == this) {
				Spel.melding(a, Component.empty().append(Mc.tekst("Met dank aan ", ChatFormatting.GREEN))
						.append(Mc.tekst(breker, Mc.kleur(kleur))), 3);
				actionbar(server, a);
			}
		});
	}

	/** Night Vision zonder deeltjes, zolang het Ei duurt. */
	private static void nachtzicht(ServerPlayer speler) {
		Mc.effect(speler, MobEffects.NIGHT_VISION, -1, 0);
	}

	private static MutableComponent naam(ServerPlayer speler) {
		return Mc.tekst(Mc.naam(speler), Mc.kleur(Teams.keuze(speler)));
	}

	// Actionbar, sidebar, bossbar

	/** {@code Bevroren · 12 · +10 · 85 punten · #4}; een melding (jumpscare) staat er even in de plaats van. */
	private void actionbar(MinecraftServer server, ServerPlayer speler) {
		Component melding = Spel.melding(speler);
		if (melding != null) {
			Mc.actionbar(speler, melding);
			return;
		}
		int nu = server.getTickCount();
		UUID id = speler.getUUID();
		MutableComponent regel = Component.empty();
		if (bevriezingLoopt && !id.equals(bevriezer) && nu < bevrorenTot) {
			regel.append(Mc.tekst("Bevroren · " + secondenTot(bevrorenTot, nu) + " · ", ChatFormatting.AQUA));
		}
		Integer haste = hasteTot.get(id);
		if (haste != null && nu < haste) {
			regel.append(Mc.tekst("Haste · " + secondenTot(haste, nu) + " · ", ChatFormatting.GOLD));
		}
		Integer p = plus.get(id);
		Integer pTot = plusTot.get(id);
		if (p != null && pTot != null && nu < pTot) {
			if (p >= EiBlok.NETHERITE.punten()) {
				regel.append(Component.literal("+" + p).withStyle(s -> s.withColor(PAARS).withBold(true)));
			} else {
				regel.append(Mc.tekst("+" + p, ChatFormatting.GREEN, ChatFormatting.BOLD));
			}
			regel.append(Mc.tekst(" · ", ChatFormatting.WHITE));
		}
		int score = stand.score(id);
		int plek = stand.plek(id);
		regel.append(Mc.tekst(score + " punten" + (plek > 0 ? " · #" + plek : ""), ChatFormatting.WHITE));
		Mc.actionbar(speler, regel);
	}

	private static int secondenTot(int tot, int nu) {
		return (tot - nu + 19) / 20;
	}

	private void toonSidebar(MinecraftServer server) {
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Klassement.Regel r : stand.top(10)) {
			String naam = Spel.naamVan(r.speler());
			ServerPlayer s = server.getPlayerList().getPlayer(r.speler());
			Kleur k = s == null ? null : Teams.keuze(s);
			regels.add(new Sidebar.Regel(Mc.tekst(naam, Mc.kleur(k)), r.score()));
		}
		Sidebar.toonScores(server, "Het Ei · top 10", regels);
	}

	@Override
	public void seconde(MinecraftServer server) {
		int nu = server.getTickCount();
		// Haste en bevriezing voorbij: een pling.
		for (Map.Entry<UUID, Integer> e : new ArrayList<>(hasteTot.entrySet())) {
			if (nu >= e.getValue()) {
				hasteTot.remove(e.getKey());
				ServerPlayer s = server.getPlayerList().getPlayer(e.getKey());
				if (s != null) {
					Mc.geluid(s, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
				}
			}
		}
		if (bevriezingLoopt && nu >= bevrorenTot) {
			bevriezingLoopt = false;
			for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
				if (Spel.status(s).bevroren) {
					Opstelling.ontdooi(s);
					s.removeEffect(MobEffects.MINING_FATIGUE);
					Mc.geluid(s, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
				}
			}
		}
		if (!timerGestart) {
			return;
		}
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			actionbar(server, s);
		}
		int timer = Spel.timer();
		BossEvent.BossBarColor kleur = timer <= Regels.LAATSTE_MINUUT ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN;
		Bossbar.zet(BossbarTekst.ei(timer), kleur, Spel.timerDeel());
		Doolhof.laatsteTellen(server, timer);
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server);
	}

	private void einde(MinecraftServer server) {
		Klassement.Regel winnaar = stand.winnaar().orElse(null);
		Spel.einde(server);
		Bossbar.basiskamp();
		if (winnaar != null) {
			ServerPlayer w = server.getPlayerList().getPlayer(winnaar.speler());
			String naam = Spel.naamVan(winnaar.speler()).toUpperCase(java.util.Locale.ROOT);
			MutableComponent titel = Component.empty();
			if (w != null) {
				titel.append(Mc.kop(w)).append(Component.literal(" "));
			}
			titel.append(Mc.tekst(naam + " WINT HET EI", ChatFormatting.GOLD, ChatFormatting.BOLD));
			Mc.titleAllen(server, titel, Mc.tekst(winnaar.score() + " punten", ChatFormatting.YELLOW), 10, 80, 20);
			if (w != null) {
				Vuurwerk.goud(Mc.wereld(server), w.getX(), w.getY() + 2, w.getZ());
			}
		} else {
			Mc.titleAllen(server, Mc.tekst("HET EI IS VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD),
					Mc.tekst("niemand hakte punten", ChatFormatting.GRAY), 10, 80, 20);
		}
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.naarPunt(s, "v3");
		}
		toonSidebar(server);
	}

	/** Van het Ei of een ketting gevallen: geheald terug op je eigen startplek, met je punten en spullen. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		String spawn = Spel.status(speler).eiSpawn;
		Spel.naarPunt(speler, spawn != null ? spawn : "ei_spawn_1");
		// De heal bij een dood haalt alle effecten weg.
		nachtzicht(speler);
		if (bevriezingLoopt && !speler.getUUID().equals(bevriezer)) {
			// De heal haalt de effecten weg; de bevriezing blijft staan.
			Mc.effect(speler, MobEffects.MINING_FATIGUE, Math.max(1, (bevrorenTot - server.getTickCount()) / 20), 4);
		}
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		Teams.zorgVoorTeam(server, speler);
		Spel.zetRol(server, speler, Rol.SPELER);
		speler.setGameMode(GameType.SURVIVAL);
		if (st.eiSpawn == null) {
			List<String> spawns = Spel.reeks("ei_spawn_");
			st.eiSpawn = spawns.isEmpty() ? "ei_spawn_1" : spawns.get(Math.floorMod(volgendeSpawn++, spawns.size()));
			// Nieuw in deze ronde: de pickaxe alsnog.
			Kits.geefAan(server, "ei", List.of(speler), stack -> Items26.markeer(stack, Items26.EI_TAG, null));
		}
		Spel.naarPunt(speler, st.eiSpawn);
		nachtzicht(speler);
		// Een bevriezing die nog loopt geldt ook voor hem; een die voorbij is niet meer.
		int nu = server.getTickCount();
		if (bevriezingLoopt && nu < bevrorenTot && !speler.getUUID().equals(bevriezer)) {
			Opstelling.bevries(speler);
			Mc.effect(speler, MobEffects.MINING_FATIGUE, Math.max(1, (bevrorenTot - nu) / 20), 4);
		} else {
			speler.removeEffect(MobEffects.MINING_FATIGUE);
		}
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		Klassement.Regel w = stand.winnaar().orElse(null);
		return "Ei: " + stand.volgorde().size() + " spelers met punten" + (w == null ? "" : ", bovenaan " + Spel.naamVan(w.speler()) + " met " + w.score())
				+ (EiOpslag.bezig() ? ", het Ei wordt nog teruggezet" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		for (ServerPlayer s : Mc.deelnemers(server)) {
			// De pickaxe is alleen voor het Ei.
			Items26.haalWeg(s, Items26.EI_TAG);
			s.setGameMode(GameType.ADVENTURE);
			s.removeEffect(MobEffects.HASTE);
			s.removeEffect(MobEffects.NIGHT_VISION);
			s.removeEffect(MobEffects.MINING_FATIGUE);
		}
		Sidebar.weg(server);
	}
}
