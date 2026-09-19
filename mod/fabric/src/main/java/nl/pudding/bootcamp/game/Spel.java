package nl.pudding.bootcamp.game;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.rad.RadSpel;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * De spelstatus: huidige ronde, timer, per speler een rol en vlaggen. De mod is de bron van
 * waarheid; de scoreboard-tags ({@code king}, {@code hunter}, {@code kijker}, {@code uitverkoren},
 * {@code ticket}) zijn read-only spiegels die elke seconde worden bijgezet.
 */
public final class Spel {
	public static final Random RANDOM = new Random();
	private static final List<String> SPIEGELTAGS = List.of("king", "hunter", "kijker", "uitverkoren", "ticket");

	private static final Map<UUID, SpelerStatus> SPELERS = new LinkedHashMap<>();
	private static Ronde ronde = Ronde.BASISKAMP;
	private static RondeLogica actief;
	private static int timer;
	private static int timerStart;
	private static boolean timerLoopt;
	private static UUID finalist1;
	private static UUID finalist2;

	private Spel() {
	}

	public static void init() {
		// Als eerste in het register: eerst de ronde stoppen, dan pas de rest opruimen.
		Reset.REGISTER.registreer("ronde stoppen en vlaggen wissen", server -> {
			stop(server);
			ronde = Ronde.BASISKAMP;
			finalist1 = null;
			finalist2 = null;
			SPELERS.values().forEach(SpelerStatus::wis);
		});
	}

	// Status

	public static Ronde ronde() {
		return ronde;
	}

	/** De lopende ronde, of {@code null} tussen twee rondes in. */
	public static RondeLogica actief() {
		return actief;
	}

	public static boolean loopt() {
		return actief != null;
	}

	public static SpelerStatus status(ServerPlayer speler) {
		SpelerStatus s = SPELERS.computeIfAbsent(speler.getUUID(), id -> new SpelerStatus(id, Mc.naam(speler)));
		s.naam = Mc.naam(speler);
		return s;
	}

	/** De status van iemand die misschien offline is, of {@code null} als de mod hem niet kent. */
	public static SpelerStatus status(UUID id) {
		return SPELERS.get(id);
	}

	public static Collection<SpelerStatus> alleStatussen() {
		return SPELERS.values();
	}

	public static Rol rol(ServerPlayer speler) {
		return Mc.isStaff(speler) ? Rol.STAFF : status(speler).rol;
	}

	/** Zet de rol en het team dat erbij hoort. */
	public static void zetRol(MinecraftServer server, ServerPlayer speler, Rol rol) {
		status(speler).rol = rol;
		// Wie kijkt ziet geen border (en dus geen rood scherm); wie meedoet wel.
		if (rol == Rol.KIJKER || rol == Rol.FINALIST) {
			Border.verberg(speler);
		} else {
			Border.toonEcht(speler);
		}
		switch (rol) {
			case SPELER -> Teams.zet(server, speler, Teams.SPELERS);
			case HUNTER -> Teams.zet(server, speler, Teams.HUNTERS);
			case KING, FINALIST -> Teams.zet(server, speler, Teams.KING);
			case KIJKER -> Teams.zet(server, speler, Teams.OUT);
			// Ronde 5: iedereen uit zijn team, dus alles is PvP.
			case FFA -> Teams.uitTeam(server, speler);
			case STAFF -> {
			}
		}
	}

	/** Online, geen staff, met deze rol en nog in de ronde. */
	public static List<ServerPlayer> levend(MinecraftServer server, Rol rol) {
		List<ServerPlayer> uit = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = status(s);
			if (st.rol == rol && !st.dood) {
				uit.add(s);
			}
		}
		return uit;
	}

	public static UUID finalist1() {
		return finalist1;
	}

	public static UUID finalist2() {
		return finalist2;
	}

	public static void zetFinalist1(UUID id) {
		finalist1 = id;
	}

	public static void zetFinalist2(UUID id) {
		finalist2 = id;
	}

	/** De uitverkorene als hij online is, anders {@code null}. */
	public static ServerPlayer clown(MinecraftServer server) {
		String naam = ConfigStore.get().uitverkoren();
		return naam == null ? null : server.getPlayerList().getPlayerByName(naam);
	}

	public static boolean isClown(ServerPlayer speler) {
		return ConfigStore.get().isUitverkoren(Mc.naam(speler));
	}

	// Config

	public static Punt punt(String naam) {
		return ConfigStore.get().punten().get(naam);
	}

	public static Regio regio(String naam) {
		return ConfigStore.get().regios().get(naam);
	}

	/**
	 * Voor {@code magStarten}: wie buiten de border wordt neergezet krijgt schade, dus een startpunt
	 * hoort binnen de regio van de ronde te liggen.
	 *
	 * @return {@code null} als het klopt, anders de melding
	 */
	public static String buitenRegio(String regioNaam, String... puntNamen) {
		Regio r = regio(regioNaam);
		if (r == null) {
			return null;
		}
		List<String> buiten = new ArrayList<>();
		for (String naam : puntNamen) {
			Punt p = punt(naam);
			if (p != null && !r.bevat(p.x(), p.z())) {
				buiten.add(naam);
			}
		}
		return buiten.isEmpty() ? null
				: "deze punten liggen buiten regio " + regioNaam + " (en dus buiten de border): " + String.join(", ", buiten);
	}

	/**
	 * Het omgekeerde van {@link #buitenRegio}: een tribunepunt binnen de regio waar kijkers af
	 * moeten blijven zou ze elke halve seconde terugzetten.
	 */
	public static String binnenRegio(String regioNaam, String... puntNamen) {
		Regio r = regio(regioNaam);
		if (r == null) {
			return null;
		}
		List<String> binnen = new ArrayList<>();
		for (String naam : puntNamen) {
			Punt p = punt(naam);
			if (p != null && r.bevatRond(p.x(), p.z())) {
				binnen.add(naam);
			}
		}
		return binnen.isEmpty() ? null
				: "deze tribunepunten liggen op de vloer van regio " + regioNaam + " (de cirkel binnen de selectie), waar kijkers juist af moeten blijven: "
				+ String.join(", ", binnen) + ". Verplaats de punten of selecteer " + regioNaam + " krapper";
	}

	public static void naarPunt(ServerPlayer speler, String puntNaam) {
		Punt p = punt(puntNaam);
		if (p != null) {
			Mc.teleport(speler, p);
		} else {
			Bootcamp.LOG.warn("Punt {} bestaat niet; {} blijft staan", puntNaam, Mc.naam(speler));
		}
	}

	// Timer

	public static int timer() {
		return timer;
	}

	public static boolean timerLoopt() {
		return timerLoopt;
	}

	public static void startTimer(int seconden) {
		timer = seconden;
		timerStart = Math.max(1, seconden);
		timerLoopt = true;
	}

	/** {@code /bc timer}: de resterende tijd bijstellen. */
	public static void zetTimer(int seconden) {
		timer = seconden;
		timerStart = Math.max(timerStart, Math.max(1, seconden));
	}

	public static void stopTimer() {
		timerLoopt = false;
	}

	/** Hoeveel van de tijd er nog over is, van 1 naar 0, voor de vulling van de bossbar. */
	public static float timerDeel() {
		return timerStart <= 0 ? 0f : (float) timer / timerStart;
	}

	// Rondes starten en stoppen

	/**
	 * {@code /bc start}: weigert met één regel als er iets ontbreekt en verandert dan niets.
	 *
	 * @return {@code null} als de ronde gestart is, anders waarom niet
	 */
	public static String start(MinecraftServer server, Ronde nieuw) {
		RondeLogica logica = Rondes.maak(nieuw);
		String bezwaar = controleer(server, logica);
		if (bezwaar != null) {
			return bezwaar;
		}

		if (actief != null) {
			stop(server);
		}
		// Ook zonder lopende ronde: een draaiend rad of een klaarstaande start van ronde 4 mag deze
		// ronde straks niet onderuit halen.
		RadSpel.stopDraaien(server);
		Planner.wisAlles();
		Aftelling.stop();
		ronde = nieuw;
		actief = logica;
		SPELERS.values().forEach(SpelerStatus::nieuweRonde);
		Bootcamp.LOG.info("Ronde {} ({}) start", nieuw.nummer(), nieuw.naam());
		logica.start(server);
		return null;
	}

	/** Kan deze ronde nu starten? Verandert niets. Het rad vraagt dit voordat het gaat draaien. */
	public static String controleer(MinecraftServer server, Ronde ronde) {
		return controleer(server, Rondes.maak(ronde));
	}

	private static String controleer(MinecraftServer server, RondeLogica logica) {
		List<String> mist = Ronde.ontbreekt(logica.vereisteRegios(), logica.vereistePunten(),
				ConfigStore.get().regios().keySet(), ConfigStore.get().punten().keySet());
		if (!mist.isEmpty()) {
			return "ontbreekt: " + String.join(", ", mist);
		}
		return logica.magStarten(server);
	}

	/**
	 * {@code /bc stop}, en het gewone einde van een ronde: timer stil, geplande dingen weg,
	 * bevriezing eraf, border weg, bossbar terug. De ronde ruimt haar eigen spullen op in
	 * {@code end} (mobs, sidebar).
	 */
	public static void stop(MinecraftServer server) {
		RondeLogica was = actief;
		actief = null;
		timerLoopt = false;
		Planner.wisAlles();
		Aftelling.stop();
		Opstelling.losIedereen(server);
		if (was != null) {
			try {
				was.end(server);
			} catch (RuntimeException e) {
				Bootcamp.LOG.error("Opruimen van ronde {} faalde", was.ronde().nummer(), e);
			}
			Bootcamp.LOG.info("Ronde {} gestopt", was.ronde().nummer());
		}
		Border.weg(server);
		Bossbar.basiskamp();
	}

	/** Iedereen die meedoet wordt weer gewoon speler, in de goede gamemode, full hp. */
	public static void maakSpelers(MinecraftServer server, boolean survival) {
		for (ServerPlayer s : Mc.deelnemers(server)) {
			zetRol(server, s, Rol.SPELER);
			s.setGameMode(survival ? GameType.SURVIVAL : GameType.ADVENTURE);
			Mc.heal(s);
		}
	}

	// Tick

	/** Elke servertick. */
	public static void tick(MinecraftServer server) {
		try {
			Planner.tick();
			Aftelling.tick(server);
			RadSpel.tick(server);
			if (actief != null) {
				actief.tick(server);
			}
			if (server.getTickCount() % 20 == 0) {
				seconde(server);
			}
		} catch (RuntimeException e) {
			// Een fout hier zou de hele server stoppen, midden in het event. Liever de ronde afbreken.
			noodstop(server, e);
		}
	}

	private static void noodstop(MinecraftServer server, RuntimeException oorzaak) {
		Bootcamp.LOG.error("Fout in de tick van ronde {}; de ronde wordt afgebroken", ronde.nummer(), oorzaak);
		try {
			RadSpel.stop(server);
			stop(server);
		} catch (RuntimeException nogEen) {
			Bootcamp.LOG.error("Ook het afbreken faalde", nogEen);
			actief = null;
		}
		server.getPlayerList().broadcastSystemMessage(Mc.tekst("[bootcamp] Er ging iets mis in ronde " + ronde.nummer()
				+ "; de ronde is afgebroken. Kijk in de console en start hem opnieuw met /bc start " + ronde.nummer() + ".",
				ChatFormatting.RED), false);
	}

	private static void seconde(MinecraftServer server) {
		if (actief != null && timerLoopt) {
			timer = Math.max(0, timer - 1);
			if (timer == 0) {
				timerLoopt = false;
				actief.timerOp(server);
			}
		}
		// timerOp kan de ronde beëindigd hebben.
		if (actief != null) {
			actief.seconde(server);
		}
		spiegelTags(server);
		Bossbar.iedereenErbij(server);
	}

	private static void spiegelTags(MinecraftServer server) {
		for (ServerPlayer s : Mc.spelers(server)) {
			SpelerStatus st = status(s);
			String rolTag = Mc.isStaff(s) ? null : st.rol.tag();
			for (String tag : SPIEGELTAGS) {
				boolean hoort = switch (tag) {
					case "uitverkoren" -> isClown(s);
					case "ticket" -> st.ticket;
					default -> tag.equals(rolTag);
				};
				if (hoort) {
					s.addTag(tag);
				} else {
					s.removeTag(tag);
				}
			}
		}
	}

	// Join en quit

	public static void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = status(speler);
		Opstelling.herstelBijJoin(speler);
		if (Mc.isStaff(speler)) {
			return;
		}
		if (actief == null || ronde != Ronde.HORDE) {
			// Dood in de horde en daarna uitgelogd: de bewaarde spullen komen alsnog terug.
			Tribune.geefBewaardTerug(speler);
		}
		if (actief != null) {
			actief.onJoin(server, speler);
		} else {
			herstelTussenRondes(server, speler, st);
		}
	}

	/**
	 * Iemand logt in terwijl er geen ronde loopt. De ronde waarin hij wegviel is zonder hem
	 * afgelopen, dus wat het einde van die ronde met iedereen deed krijgt hij alsnog.
	 */
	private static void herstelTussenRondes(MinecraftServer server, ServerPlayer speler, SpelerStatus st) {
		speler.setGameMode(GameType.ADVENTURE);
		boolean finalist = speler.getUUID().equals(finalist1) || speler.getUUID().equals(finalist2);
		if (finalist && ronde.inArena()) {
			Kroon.maakFinalist(server, speler);
			Tribune.naarTribune(speler);
			return;
		}
		// Een kroon die is doorgegeven terwijl hij weg was zit nog in zijn spelerdata.
		if (Kroon.draagtKroon(speler) || st.rol == Rol.KING || st.rol == Rol.FINALIST) {
			Kroon.neemAf(speler);
		}
		if (ronde.inArena()) {
			// Na ronde 4 t/m 6: wie wegviel telde als dood en komt terug als kijker.
			if (st.rol != Rol.SPELER) {
				st.dood = true;
				Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
			} else {
				zetRol(server, speler, Rol.SPELER);
			}
			return;
		}
		if (ronde == Ronde.EI && !st.ticket && !st.klaar) {
			// Geen ticket is geen loot, ook niet voor wie op het eind uitlogde.
			speler.getInventory().clearContent();
			Kits.geefAan(server, "basis", List.of(speler));
		}
		zetRol(server, speler, Rol.SPELER);
		if (ronde != Ronde.BASISKAMP && !st.klaar) {
			st.klaar = true;
			naarPunt(speler, Tribune.verzamelpuntNa(ronde));
		}
	}

	public static void onQuit(MinecraftServer server, ServerPlayer speler) {
		if (actief != null && !Mc.isStaff(speler)) {
			actief.onQuit(server, speler);
		}
	}

	// /bc status

	public static String statusTekst(MinecraftServer server) {
		StringBuilder sb = new StringBuilder();
		sb.append("Ronde ").append(ronde.nummer()).append(" (").append(ronde.naam()).append(")")
				.append(actief != null ? ", loopt" : ", loopt niet");
		if (timerLoopt) {
			sb.append(", timer ").append(nl.pudding.bootcamp.core.Tijd.mmss(timer));
		}
		if (finalist1 != null) {
			sb.append("\n  finalist 1: ").append(naamVan(finalist1));
		}
		if (finalist2 != null) {
			sb.append("\n  finalist 2: ").append(naamVan(finalist2));
		}
		for (ServerPlayer s : Mc.spelers(server)) {
			SpelerStatus st = status(s);
			sb.append("\n  ").append(st.naam).append(": ").append(rol(s));
			if (st.dood) {
				sb.append(" dood");
			}
			if (st.klaar) {
				sb.append(" klaar");
			}
			if (st.ticket) {
				sb.append(" ticket");
			}
			if (st.bevroren) {
				sb.append(" bevroren");
			}
			if (isClown(s)) {
				sb.append(" uitverkoren");
			}
			int slot = ConfigStore.get().slotVan(st.naam);
			if (slot >= 0) {
				sb.append(" slot ").append(slot);
			}
		}
		return sb.toString();
	}

	public static String naamVan(UUID id) {
		SpelerStatus st = SPELERS.get(id);
		return st == null ? id.toString() : st.naam;
	}
}
