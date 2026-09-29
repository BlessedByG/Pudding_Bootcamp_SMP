package nl.pudding.bootcamp.tribune;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Doodteksten;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.PvpRegel;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.rad.KroonRad;
import nl.pudding.bootcamp.teams.Teams;

import java.util.List;

/**
 * Kijkers: wie af is. Geen spectator mode, geen tp-items, geen vliegen. De mod laat spelers nooit
 * echt doodgaan: een dodelijke klap wordt geannuleerd en de ronde beslist wat er gebeurt. Een
 * kijker staat in adventure op de tribune (of in de kooi), krijgt geen schade, komt het veld of de
 * vloer niet op en staat niet op de locator bar.
 *
 * <p>Hier zit ook de PvP-regel: één check in {@code ALLOW_DAMAGE}, niet via teams.
 */
public final class Tribune {
	/** Wat er met de inventory gebeurt van wie kijker wordt. */
	public enum Spullen {
		/** Je houdt wat je hebt. */
		HOUDEN,
		/** Leeg: wie af is speelt niet meer. */
		LEGEN
	}

	private static final int TERUGZET_ELKE_TICKS = 10;
	/** Een veld van de mob arena telt tot zoveel blokken boven de selectie: het balkon is geen veld. */
	public static final int VELD_HOOGTE = 3;
	private static int volgendeTribune;

	private Tribune() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, bron, schade) -> {
			if (!(entity instanceof ServerPlayer speler) || Mc.isStaff(speler)) {
				return true;
			}
			return !vangDoodAf(speler, bron);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, bron, schade) -> {
			if (!(entity instanceof ServerPlayer slachtoffer) || Mc.isStaff(slachtoffer)) {
				return true;
			}
			return magSchade(slachtoffer, bron);
		});
		Reset.REGISTER.registreer("kijkers weg", server -> {
			volgendeTribune = 0;
			// De rollen en vlaggen zijn al gewist; hier alleen wat aan de speler zelf hangt.
			for (ServerPlayer s : Mc.deelnemers(server)) {
				Kroon.toonOpLocator(s, true);
				s.removeEffect(MobEffects.GLOWING);
			}
		});
	}

	// Dood en schade

	/** @return {@code true} als de dood is afgevangen */
	private static boolean vangDoodAf(ServerPlayer speler, DamageSource bron) {
		MinecraftServer server = speler.level().getServer();
		// Wie de dood annuleert moet zelf healen, anders gaat de speler de volgende tick alsnog dood.
		Mc.heal(speler);
		speler.removeAllEffects();
		RondeLogica ronde = Spel.actief();
		try {
			if (ronde != null && !Spel.status(speler).dood && Spel.rol(speler) != Rol.KIJKER) {
				ronde.onDeath(server, speler, bron);
			} else if (Spel.status(speler).tribunepunt != null) {
				// Een kijker die toch "doodgaat" (de void in): terug naar zijn plek.
				Spel.naarPunt(speler, Spel.status(speler).tribunepunt);
			} else {
				naarVerzamelpunt(speler);
			}
		} catch (RuntimeException e) {
			Bootcamp.LOG.error("De dood van {} afhandelen faalde", Mc.naam(speler), e);
		}
		return true;
	}

	/** Loopt er een opstelling of countdown? Dan doet niemand elkaar iets. */
	public static boolean stil() {
		return Opstelling.actief() || Aftelling.loopt();
	}

	private static boolean magSchade(ServerPlayer slachtoffer, DamageSource bron) {
		boolean mag = magSchadeZonderVoid(slachtoffer, bron);
		if (!mag && bron.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			// Wie geen schade krijgt, gaat ook in de void niet dood: dan terug naar zijn plek.
			terugUitDeVoid(slachtoffer);
		}
		return mag;
	}

	private static void terugUitDeVoid(ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.tribunepunt != null) {
			Spel.naarPunt(speler, st.tribunepunt);
		} else if (Spel.actief() != null && Spel.actief().ronde() == Ronde.QUIZ) {
			naarVerzamelpuntQuiz(speler);
		} else {
			naarVerzamelpunt(speler);
		}
	}

	private static boolean magSchadeZonderVoid(ServerPlayer slachtoffer, DamageSource bron) {
		Rol rol = Spel.rol(slachtoffer);
		// Kijkers krijgen geen schade, ook niet van de border.
		if (rol == Rol.KIJKER) {
			return false;
		}
		// Wie in een opstelling bevroren staat, kan niks doen en krijgt dus ook niks (ook niet van een gekrompen border).
		if (Opstelling.actief() && Spel.status(slachtoffer).bevroren) {
			return false;
		}
		RondeLogica ronde = Spel.actief();
		if (ronde != null && !ronde.magSchade(slachtoffer, bron)) {
			return false;
		}
		ServerPlayer aanvaller = aanvaller(bron);
		if (aanvaller != null && aanvaller != slachtoffer) {
			Rol aanvalRol = Spel.rol(aanvaller);
			if (!PvpRegel.mag(Spel.lopendeRonde(), stil(), aanvalRol, rol)) {
				return false;
			}
			// De laatste hit: ook via een pijl, want de aanvaller is de schutter.
			if (rol == Rol.KROON && aanvalRol == Rol.JAGER && !Spel.status(aanvaller).dood) {
				Kroon.onthoudHit(slachtoffer, aanvaller);
			}
		}
		return true;
	}

	/** De speler achter een klap: de slaander, of de schutter van een pijl of andere projectile. */
	public static ServerPlayer aanvaller(DamageSource bron) {
		if (bron.getEntity() instanceof ServerPlayer p) {
			return p;
		}
		if (bron.getDirectEntity() instanceof Projectile pr && pr.getOwner() instanceof ServerPlayer p) {
			return p;
		}
		return null;
	}

	// Waar iedereen staat

	/** Waar iemand heen gaat tussen twee rondes, na het einde van de laatste ronde. */
	public static String verzamelpunt(ServerPlayer speler) {
		return switch (Spel.ronde()) {
			case BASISKAMP -> "basiskamp";
			case DOOLHOF -> "v2";
			case EI -> "v3";
			case MOBARENA -> {
				// Na de mob arena naar de quiz: bij de bank van je team, de presentator op het podium.
				if (Spel.isPresentator(speler) && Spel.punt("quiz_podium") != null) {
					yield "quiz_podium";
				}
				Kleur k = Teams.keuze(speler);
				yield k != null && Spel.punt("quiz_" + k.id()) != null ? "quiz_" + k.id() : "v3";
			}
			case QUIZ, CLOWN, FFA -> volgendTribunepunt(Ronde.CLOWN);
		};
	}

	public static void naarVerzamelpunt(ServerPlayer speler) {
		String punt = verzamelpunt(speler);
		if (punt.startsWith("tribune_")) {
			naarTribune(speler, Ronde.CLOWN);
		} else if (Spel.punt(punt) != null) {
			Spel.naarPunt(speler, punt);
		}
	}

	// Kijker worden

	/**
	 * Maakt van deze speler een kijker en zet hem op de tribune van dat moment.
	 *
	 * @param doodtekst groot in beeld een willekeurige doodtekst, alleen voor hem
	 */
	public static void maakKijker(MinecraftServer server, ServerPlayer speler, Spullen spullen, boolean doodtekst) {
		String punt = volgendTribunepunt(Spel.ronde());
		maakKijkerOp(server, speler, punt, spullen, doodtekst);
		Spel.status(speler).tribunepunt = punt;
	}

	/** Kijker op een bepaalde plek: de tribune, of de kooi van een arena. */
	public static void maakKijkerOp(MinecraftServer server, ServerPlayer speler, String puntNaam, Spullen spullen, boolean doodtekst) {
		SpelerStatus st = Spel.status(speler);
		if (Kroon.draagtKroon(speler)) {
			Kroon.neemAf(speler);
		}
		if (spullen == Spullen.LEGEN) {
			speler.getInventory().clearContent();
			speler.inventoryMenu.broadcastChanges();
		}
		Spel.zetRol(server, speler, Rol.KIJKER);
		st.tribunepunt = puntNaam;
		st.arena = 0;
		speler.setGameMode(GameType.ADVENTURE);
		speler.removeEffect(MobEffects.GLOWING);
		Kroon.toonOpLocator(speler, false);
		Opstelling.ontdooi(speler);
		Mc.heal(speler);
		Spel.naarPunt(speler, puntNaam);
		if (doodtekst) {
			doodtekst(speler);
		}
	}

	/** Groot in beeld een willekeurige doodtekst, alleen voor de dode. */
	public static void doodtekst(ServerPlayer speler) {
		String tekst = Doodteksten.kies(ConfigStore.get().doodteksten(), Spel.RANDOM);
		Mc.title(speler, Mc.tekst(tekst, ChatFormatting.RED, ChatFormatting.BOLD), null, 5, 70, 20);
	}

	/** Zet iemand op de tribune zonder er een kijker van te maken (voor het Rad, of in de mob arena). */
	public static void naarTribune(ServerPlayer speler, Ronde ronde) {
		String punt = volgendTribunepunt(ronde);
		Spel.status(speler).tribunepunt = punt;
		Spel.naarPunt(speler, punt);
	}

	/** Verdeelt mensen om en om over de tribunepunten van die ronde. */
	public static String volgendTribunepunt(Ronde ronde) {
		String prefix = ronde == Ronde.MOBARENA ? "tribune_mob_" : "tribune_";
		List<String> bestaand = Spel.reeks(prefix);
		if (bestaand.isEmpty()) {
			return prefix + 1;
		}
		return bestaand.get(Math.floorMod(volgendeTribune++, bestaand.size()));
	}

	// Op de tribune houden

	/**
	 * Elke servertick: een kijker die toch het veld of de vloer op komt, gaat terug naar zijn
	 * tribunepunt (of zijn kooi).
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % TERUGZET_ELKE_TICKS != 0) {
			return;
		}
		Ronde ronde = Spel.ronde();
		if (ronde == Ronde.MOBARENA && Spel.loopt()) {
			houdVanDeVelden(server);
		} else if (ronde.inArena() || KroonRad.draait()) {
			houdVanDeVloer(server);
		}
	}

	private static void houdVanDeVelden(MinecraftServer server) {
		for (int arena = 1; arena <= 2; arena++) {
			Regio veld = Spel.regio("veld_" + arena);
			if (veld == null) {
				continue;
			}
			for (ServerPlayer s : Mc.deelnemers(server)) {
				SpelerStatus st = Spel.status(s);
				// Wie aan de beurt is hoort erin; wie in de kooi van dit veld zit ook (de tralies houden hem binnen).
				if (st.arena > 0 || st.kooi == arena) {
					continue;
				}
				if (veld.bevatSpelerTot(s.getX(), s.getY(), s.getZ(), VELD_HOOGTE)) {
					terug(s, st, Ronde.MOBARENA);
				}
			}
		}
	}

	private static void houdVanDeVloer(MinecraftServer server) {
		Regio vloer = Spel.regio("vloer");
		if (vloer == null) {
			return;
		}
		boolean radDraait = KroonRad.draait();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			boolean hoortOpTribune = st.rol == Rol.KIJKER || (radDraait && st.tribunepunt != null);
			if (hoortOpTribune && vloer.bevatSpeler(s.getX(), s.getY(), s.getZ())) {
				terug(s, st, Ronde.CLOWN);
			}
		}
	}

	private static void terug(ServerPlayer s, SpelerStatus st, Ronde ronde) {
		if (st.tribunepunt == null) {
			st.tribunepunt = volgendTribunepunt(ronde);
		}
		Spel.naarPunt(s, st.tribunepunt);
		Mc.title(s, Mc.tekst("Terug naar de tribune", ChatFormatting.GRAY), null, 0, 30, 10);
	}

	// /bc kijker

	/** Noodknop: iemand met de hand op de tribune zetten. */
	public static String handmatigAan(MinecraftServer server, ServerPlayer speler) {
		if (Mc.isStaff(speler)) {
			return Mc.naam(speler) + " staat in creative of spectator; de mod blijft van staff af";
		}
		Spel.status(speler).dood = true;
		maakKijker(server, speler, Spel.ronde() == Ronde.DOOLHOF || Spel.ronde() == Ronde.EI ? Spullen.HOUDEN : Spullen.LEGEN, false);
		return null;
	}

	/** Noodknop: iemand van de tribune halen en weer mee laten doen. */
	public static String handmatigUit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.rol != Rol.KIJKER) {
			return Mc.naam(speler) + " is geen kijker";
		}
		st.dood = false;
		st.kooi = 0;
		Ronde ronde = Spel.ronde();
		boolean loopt = Spel.loopt();
		Rol rol = !loopt ? Rol.SPELER : switch (ronde) {
			case CLOWN -> Rol.JAGER;
			case FFA -> Rol.FFA;
			default -> Rol.SPELER;
		};
		Spel.zetRol(server, speler, rol);
		speler.setGameMode(ronde.survival() && loopt ? GameType.SURVIVAL : GameType.ADVENTURE);
		if (!loopt) {
			st.tribunepunt = null;
			return null;
		}
		switch (ronde) {
			case DOOLHOF -> {
				st.tribunepunt = null;
				Spel.naarPunt(speler, "doolhof_start");
			}
			case EI -> {
				st.tribunepunt = null;
				Spel.naarPunt(speler, st.eiSpawn != null ? st.eiSpawn : "ei_spawn_1");
			}
			case MOBARENA -> {
				// Weer wachtend op de tribune: hij mag een volgende beurt spelen.
				naarTribune(speler, Ronde.MOBARENA);
			}
			case QUIZ -> {
				st.tribunepunt = null;
				Tribune.naarVerzamelpuntQuiz(speler);
			}
			case CLOWN, FFA -> {
				st.tribunepunt = null;
				List<String> plekken = Spel.reeks("jager_");
				Spel.naarPunt(speler, plekken.isEmpty() ? "jager_1" : plekken.get(Spel.RANDOM.nextInt(plekken.size())));
			}
			case BASISKAMP -> {
			}
		}
		return null;
	}

	/** Naar de bank van zijn team, of het podium voor de presentator. */
	public static void naarVerzamelpuntQuiz(ServerPlayer speler) {
		if (Spel.isPresentator(speler)) {
			Spel.naarPunt(speler, "quiz_podium");
			return;
		}
		Kleur k = Teams.keuze(speler);
		if (k != null) {
			Spel.naarPunt(speler, "quiz_" + k.id());
		}
	}
}
