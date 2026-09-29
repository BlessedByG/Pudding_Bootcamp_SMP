package nl.pudding.bootcamp.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.EiBlok;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.TeamKeuze;
import nl.pudding.bootcamp.core.Tijd;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.rad.KroonRad;
import nl.pudding.bootcamp.teams.Teams;
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
 * waarheid; de scoreboard-tags ({@code kroon}, {@code jager}, {@code ffa}, {@code kijker},
 * {@code uitverkoren}) zijn read-only spiegels die elke seconde worden bijgezet.
 */
public final class Spel {
	public static final Random RANDOM = new Random();
	private static final List<String> SPIEGELTAGS = List.of("kroon", "jager", "ffa", "kijker", "uitverkoren");

	private static final Map<UUID, SpelerStatus> SPELERS = new LinkedHashMap<>();
	private static Ronde ronde = Ronde.BASISKAMP;
	private static RondeLogica actief;
	private static int timer;
	private static int totaal;
	private static boolean timerLoopt;

	private Spel() {
	}

	public static void init() {
		// Als eerste in het register: eerst de ronde stoppen, dan pas de rest opruimen.
		Reset.REGISTER.registreer("ronde stoppen en vlaggen wissen", server -> {
			KroonRad.stop(server);
			stop(server);
			ronde = Ronde.BASISKAMP;
			SPELERS.values().forEach(SpelerStatus::wis);
		});
	}

	// Status

	/** De laatst gestarte ronde; {@link Ronde#BASISKAMP} na een reset. */
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

	/** De ronde die nu echt loopt, of het basiskamp tussen twee rondes in: voor de PvP-regel. */
	public static Ronde lopendeRonde() {
		return actief == null ? Ronde.BASISKAMP : actief.ronde();
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
		if (rol == Rol.KIJKER) {
			Border.verberg(speler);
		} else {
			Border.toonEcht(speler);
		}
		switch (rol) {
			// Ronde 1 t/m 4 in je teamkleur, daarna wit.
			case SPELER -> Teams.zet(server, speler, Teams.teamVoorSpeler(speler, metKleuren()));
			case JAGER -> Teams.zet(server, speler, Teams.JAGERS);
			case KROON -> Teams.zet(server, speler, Teams.KROON);
			case KIJKER -> Teams.zet(server, speler, Teams.OUT);
			case FFA -> Teams.uitTeam(server, speler);
			case STAFF -> {
			}
		}
	}

	/** Staan de teamkleuren nog aan? Tot en met de quiz. */
	public static boolean metKleuren() {
		return ronde.nummer() <= Ronde.QUIZ.nummer();
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

	/** De uitverkorene als hij online is, anders {@code null}. */
	public static ServerPlayer clown(MinecraftServer server) {
		String naam = ConfigStore.get().uitverkoren();
		return naam == null ? null : server.getPlayerList().getPlayerByName(naam);
	}

	public static boolean isClown(ServerPlayer speler) {
		return ConfigStore.get().isUitverkoren(Mc.naam(speler));
	}

	/** De presentator van de quiz als hij online is, anders {@code null}. */
	public static ServerPlayer presentator(MinecraftServer server) {
		String naam = ConfigStore.get().presentator();
		return naam == null ? null : server.getPlayerList().getPlayerByName(naam);
	}

	public static boolean isPresentator(ServerPlayer speler) {
		return ConfigStore.get().isPresentator(Mc.naam(speler));
	}

	public static Instellingen instellingen() {
		return ConfigStore.get().instellingen();
	}

	/** Een melding vooraan in de actionbar, zoveel seconden lang. De ronde toont hem elke seconde. */
	public static void melding(ServerPlayer speler, Component tekst, int seconden) {
		SpelerStatus st = status(speler);
		st.melding = tekst;
		st.meldingTot = speler.level().getServer().getTickCount() + seconden * 20;
	}

	/** De melding van deze speler als die nog loopt, anders {@code null}. */
	public static Component melding(ServerPlayer speler) {
		SpelerStatus st = status(speler);
		if (st.melding != null && speler.level().getServer().getTickCount() < st.meldingTot) {
			return st.melding;
		}
		st.melding = null;
		return null;
	}

	// Config

	public static Punt punt(String naam) {
		return ConfigStore.get().punten().get(naam);
	}

	public static Regio regio(String naam) {
		return ConfigStore.get().regios().get(naam);
	}

	/** De punten van een genummerde reeks: {@code jager_1}, {@code jager_2}, ... tot het eerste gat. */
	public static List<String> reeks(String prefix) {
		return Ronde.reeks(prefix, ConfigStore.get().punten().keySet());
	}

	/**
	 * Voor {@code magStarten}: wie buiten de border wordt neergezet krijgt schade, dus een startpunt
	 * hoort binnen de regio van de ronde te liggen.
	 *
	 * @return {@code null} als het klopt, anders de melding
	 */
	public static String buitenRegio(String regioNaam, List<String> puntNamen) {
		Regio r = regio(regioNaam);
		if (r == null) {
			return null;
		}
		Regio.Doos o = r.omhullende();
		List<String> buiten = new ArrayList<>();
		for (String naam : puntNamen) {
			Punt p = punt(naam);
			if (p != null && !o.bevat(p.x(), p.z())) {
				buiten.add(naam);
			}
		}
		return buiten.isEmpty() ? null
				: "deze punten liggen buiten regio " + regioNaam + " (en dus buiten de border): " + String.join(", ", buiten);
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

	/**
	 * Hoeveel seconden er van de lopende timer al gespeeld zijn: de duur min wat er op de klok staat.
	 * {@code /<ronde> resterend} schuift dus ook de poort en de hint mee.
	 */
	public static int gespeeld() {
		return Math.max(0, totaal - timer);
	}

	public static boolean timerLoopt() {
		return timerLoopt;
	}

	public static void startTimer(int seconden) {
		timer = seconden;
		totaal = seconden;
		timerLoopt = true;
	}

	/** {@code /<ronde> resterend}: de klok op zoveel seconden. */
	public static void zetTimer(int seconden) {
		timer = Math.max(0, seconden);
	}

	/** Een nieuwe {@code /<ronde> timer} terwijl de ronde loopt: wat al gespeeld is blijft gespeeld. */
	public static void zetTotaal(int seconden) {
		int al = gespeeld();
		totaal = seconden;
		timer = Math.max(0, seconden - al);
	}

	public static void stopTimer() {
		timerLoopt = false;
	}

	/** Hoeveel van de tijd er nog over is, van 1 naar 0, voor de vulling van de bossbar. */
	public static float timerDeel() {
		return totaal <= 0 ? 0f : Math.min(1f, (float) timer / totaal);
	}

	// Rondes starten en stoppen

	/**
	 * {@code /<ronde> start}: weigert met één regel als er iets ontbreekt en verandert dan niets.
	 * Een lopende ronde wordt eerst afgebroken.
	 *
	 * @return {@code null} als de ronde gestart is, anders waarom niet
	 */
	public static String start(MinecraftServer server, Ronde nieuw) {
		return start(server, Rondes.maak(nieuw));
	}

	public static String start(MinecraftServer server, RondeLogica logica) {
		String bezwaar = controleer(server, logica);
		if (bezwaar != null) {
			return bezwaar;
		}
		if (actief != null) {
			stop(server);
		}
		// Een draaiend Rad mag de nieuwe ronde straks niet onderuit halen.
		KroonRad.stop(server);
		Planner.wisAlles();
		Aftelling.stop();
		ronde = logica.ronde();
		actief = logica;
		SPELERS.values().forEach(SpelerStatus::nieuweRonde);
		Bootcamp.LOG.info("Ronde {} ({}) start", ronde.nummer(), ronde.naam());
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
	 * {@code /<ronde> stop}, en het gewone einde van een ronde: timer stil, geplande dingen weg,
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

	/**
	 * Het gewone einde van een ronde: stoppen, maar wat de ronde daarna nog wil laten zien (tien
	 * seconden vieren) mag blijven lopen. Geplande taken worden niet gewist.
	 */
	public static void einde(MinecraftServer server) {
		RondeLogica was = actief;
		actief = null;
		timerLoopt = false;
		Aftelling.stop();
		Opstelling.losIedereen(server);
		if (was != null) {
			try {
				was.end(server);
			} catch (RuntimeException e) {
				Bootcamp.LOG.error("Opruimen van ronde {} faalde", was.ronde().nummer(), e);
			}
			Bootcamp.LOG.info("Ronde {} voorbij", was.ronde().nummer());
		}
		Border.weg(server);
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
			KroonRad.tick(server);
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
			KroonRad.stop(server);
			stop(server);
		} catch (RuntimeException nogEen) {
			Bootcamp.LOG.error("Ook het afbreken faalde", nogEen);
			actief = null;
		}
		String cmd = ronde.commando() == null ? "bc" : ronde.commando();
		Mc.chatAllen(server, Mc.tekst("[bootcamp] Er ging iets mis in ronde " + ronde.nummer()
				+ "; de ronde is afgebroken. Kijk in de console en start hem opnieuw met /" + cmd + " start.",
				ChatFormatting.RED));
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
		if (actief == null) {
			geenHonger(server);
		}
		Bossbar.iedereenErbij(server);
	}

	/** In de lobby en tussen de rondes: de honger blijft vol, ook zonder eten. */
	private static void geenHonger(MinecraftServer server) {
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s.getFoodData().getFoodLevel() < 20) {
				s.getFoodData().setFoodLevel(20);
			}
			if (s.getFoodData().getSaturationLevel() < 5f) {
				s.getFoodData().setSaturation(5f);
			}
		}
	}

	private static void spiegelTags(MinecraftServer server) {
		for (ServerPlayer s : Mc.spelers(server)) {
			SpelerStatus st = status(s);
			String rolTag = Mc.isStaff(s) ? null : st.rol.tag();
			for (String tag : SPIEGELTAGS) {
				boolean hoort = tag.equals("uitverkoren") ? isClown(s) : tag.equals(rolTag);
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
		// Spullen die alleen in één ronde horen, kwijt voor wie ze buiten die ronde nog heeft.
		if (!(actief != null && actief.ronde() == Ronde.EI)) {
			Items26.haalWeg(speler, Items26.EI_TAG);
		}
		if (!(actief != null && actief.ronde() == Ronde.QUIZ)) {
			Items26.haalWeg(speler, Items26.QUIZ_TAG);
		}
		if (actief != null) {
			actief.onJoin(server, speler);
		} else {
			herstelTussenRondes(server, speler, st);
			welkom(speler);
		}
	}

	/** Wie joint terwijl er geen ronde loopt krijgt de welkomsttitle, alleen voor hem. */
	private static void welkom(ServerPlayer speler) {
		Mc.title(speler, Mc.tekst("PUDDING BOOTCAMP", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst("Welkom, " + Mc.naam(speler), ChatFormatting.YELLOW), 10, 70, 20);
		Mc.geluid(speler, SoundEvents.NOTE_BLOCK_CHIME, 1f, 1f);
	}

	/**
	 * Iemand logt in terwijl er geen ronde loopt. De ronde waarin hij wegviel is zonder hem
	 * afgelopen, dus wat het einde van die ronde met iedereen deed krijgt hij alsnog.
	 */
	private static void herstelTussenRondes(MinecraftServer server, ServerPlayer speler, SpelerStatus st) {
		speler.setGameMode(GameType.ADVENTURE);
		// Een kroon die is doorgegeven terwijl hij weg was zit nog in zijn spelerdata.
		if (Kroon.draagtKroon(speler) && !Kroon.isKing(speler)) {
			Kroon.neemAf(speler);
		}
		switch (ronde) {
			case BASISKAMP -> zetRol(server, speler, Rol.SPELER);
			case DOOLHOF -> {
				// Het doolhof is zonder hem afgelopen: dan het kleinste team, zoals iedereen zonder team.
				Teams.zorgVoorTeam(server, speler);
				zetRol(server, speler, Rol.SPELER);
				naarPunt(speler, "v2");
			}
			case EI -> {
				Teams.zorgVoorTeam(server, speler);
				zetRol(server, speler, Rol.SPELER);
				naarPunt(speler, "v3");
			}
			case MOBARENA -> {
				// Na de mob arena levert iedereen alles in.
				speler.getInventory().clearContent();
				Teams.zorgVoorTeam(server, speler);
				zetRol(server, speler, Rol.SPELER);
				Tribune.naarVerzamelpunt(speler);
			}
			case QUIZ, CLOWN, FFA -> {
				if (ronde == Ronde.QUIZ) {
					speler.getInventory().clearContent();
				}
				if (Kroon.isKing(speler)) {
					// De King na de kroning: hij houdt zijn kroon.
					zetRol(server, speler, Rol.KROON);
					return;
				}
				st.dood = true;
				Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
			}
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
			sb.append(", timer ").append(Tijd.mmss(timer)).append(" (gespeeld ").append(Tijd.mmss(gespeeld())).append(")");
		}
		if (actief != null) {
			String extra = actief.statusRegel(server);
			if (extra != null) {
				sb.append("\n  ").append(extra);
			}
		}
		for (ServerPlayer s : Mc.spelers(server)) {
			SpelerStatus st = status(s);
			sb.append("\n  ").append(st.naam).append(": ").append(rol(s));
			Kleur k = Teams.keuze(s);
			if (k != null) {
				sb.append(" ").append(k.id());
			}
			if (st.dood) {
				sb.append(" dood");
			}
			if (st.klaar) {
				sb.append(" klaar");
			}
			if (st.bevroren) {
				sb.append(" bevroren");
			}
			if (st.arena > 0) {
				sb.append(" arena ").append(st.arena);
			}
			if (st.kooi > 0) {
				sb.append(" kooi ").append(st.kooi);
			}
			if (isClown(s)) {
				sb.append(" uitverkoren");
			}
			if (isPresentator(s)) {
				sb.append(" presentator");
			}
		}
		Instellingen i = instellingen();
		sb.append("\n  doolhof: timer ").append(i.doolhofTimer()).append(" min, poort ").append(i.doolhofPoort())
				.append(" min, hint ").append(i.doolhofHint()).append(" min, hinttekst ")
				.append(i.hinttekst() == null ? "(windrichting)" : "'" + i.hinttekst() + "'");
		sb.append("\n  ei: timer ").append(i.eiTimer()).append(" min, blokken");
		for (EiBlok b : EiBlok.values()) {
			sb.append(" ").append(b.id()).append(" ").append(i.eiBlokken(b));
		}
		sb.append("\n  mob arena: aftekst '").append(i.aftekst()).append("', punten ").append(i.mobPunten())
				.append(", elk ander type ").append(Instellingen.MOB_PUNTEN_ANDER);
		sb.append("\n  wachttekst: clown '").append(i.clownWachttekst()).append("', ffa '").append(i.ffaWachttekst()).append("'");
		return sb.toString();
	}

	public static String naamVan(UUID id) {
		SpelerStatus st = SPELERS.get(id);
		return st == null ? id.toString() : st.naam;
	}
}
