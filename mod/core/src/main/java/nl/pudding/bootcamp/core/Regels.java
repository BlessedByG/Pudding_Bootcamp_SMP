package nl.pudding.bootcamp.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * De spelregels uit docs/02 en docs/03 die rekenen of beslissen, als pure functies. De nummers
 * verwijzen naar {@code REGELS.md}.
 */
public final class Regels {
	/** R0.9: de countdown bij de start van een ronde en van een beurt. */
	public static final int COUNTDOWN = 5;
	/** R5.4: de countdown na {@code /clown go}, {@code /ffa go} en na elke kroonwissel. */
	public static final int OPSTELLING = 10;
	/** R5.7: zo lang wacht de mod op een kroonhouder die uitlogt. */
	public static final int KROON_UITLOG_WACHT = 30;
	/** R1.6 en R2.9: de laatste minuut van het doolhof en het Ei: bossbar rood. */
	public static final int LAATSTE_MINUUT = 60;
	/** R1.6 en R2.9: de laatste tien seconden groot in beeld. */
	public static final int LAATSTE_TELLEN = 10;
	/** R1.7: na de timer is regio {@code doolhof_gif} giftig: elke zoveel seconden zoveel halve harten, plus Poison. */
	public static final int DOOLHOF_GIF_ELKE = 2;
	public static final int DOOLHOF_GIF_SCHADE = 2;
	/** R1.8: na elke minuut in het gif verdubbelt de klap, tot hooguit zoveel keer. */
	public static final int DOOLHOF_GIF_MAX_KEER = 16;

	/** R1.8: hoe vaak zo hard als de eerste minuut: 1, 2, 4, 8, 16, en dan blijft het 16. */
	public static int gifKeer(int gifSeconden) {
		return Math.min(DOOLHOF_GIF_MAX_KEER, 1 << Math.min(30, Math.max(0, gifSeconden) / 60));
	}

	/** R1.8: de klap van het gif in halve harten, na zoveel seconden gif. */
	public static int gifSchade(int gifSeconden) {
		return DOOLHOF_GIF_SCHADE * gifKeer(gifSeconden);
	}
	/** R2.5: redstone-gok. */
	public static final int EI_HASTE = 15;
	public static final int EI_BEVRIEZING = 15;
	/** R2.10: zo groot is het gat van een TNT, in blokken vanaf het midden. */
	public static final int EI_TNT_STRAAL = 3;
	/** R2.11: zo lang Efficiency V en Haste II na glowstone. */
	public static final int EI_GLOWSTONE = 10;
	/** R2.12: zo lang Nausea voor de anderen na slime. */
	public static final int EI_MISSELIJK = 15;
	/** R3.6: na elke beurt tien seconden vieren, dan pas naar de tribune. */
	public static final int BEURT_VIEREN = 10;
	/** R3.8, R4.5: na de winnaar van de mob arena en de quiz tien seconden vieren. */
	public static final int VIEREN = 10;
	/** R5.9 en R6.3: de standaardduur van {@code /clown krimp} en {@code /ffa krimp}. */
	public static final int KRIMP_SECONDEN = 60;
	/** R6.4. */
	public static final int KRONING_VUURWERK = 20;
	/** R5.1: drie seconden na DE KROON gaat iedereen de vloer op. */
	public static final int NA_HET_RAD = 3;

	private Regels() {
	}

	// Verdelen over startplekken

	/** Om en om: index 0 op punt 1, index 1 op punt 2, enzovoort. */
	public static int startpunt(int index, int aantalPunten) {
		return Math.floorMod(index, aantalPunten) + 1;
	}

	/**
	 * R5.2: willekeurig over de plekken, één per plek; zijn er meer spelers dan plekken, dan delen
	 * ze om en om. Geeft per speler het plek-nummer vanaf 1.
	 */
	public static int[] verdeelWillekeurig(int spelers, int plekken, RandomGenerator random) {
		if (plekken <= 0) {
			throw new IllegalArgumentException("geen plekken");
		}
		int[] volgorde = new int[plekken];
		for (int i = 0; i < plekken; i++) {
			volgorde[i] = i + 1;
		}
		for (int i = plekken - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int t = volgorde[i];
			volgorde[i] = volgorde[j];
			volgorde[j] = t;
		}
		int[] uit = new int[Math.max(0, spelers)];
		for (int s = 0; s < uit.length; s++) {
			uit[s] = volgorde[s % plekken];
		}
		return uit;
	}

	// Ronde 1

	public enum Val {
		/** De jumpscare met een willekeurige foto, voor wie hem opent. */
		SCHRIK,
		/** De 8D-klop, alleen voor wie hem opent. */
		KLOP,
		/** Husks en silverfish om hem heen. */
		MOBS
	}

	/**
	 * R1.4: wat een valkist in het doolhof doet: 25% de jumpscare, 25% de 8D-klop, 50% mobs. Staan
	 * de mobs uit ({@code /doolhof valmobs 0}), dan de jumpscare of de klop, 50/50.
	 */
	public static Val valkistGok(RandomGenerator random, boolean metMobs) {
		int gok = random.nextInt(metMobs ? 4 : 2);
		return gok == 0 ? Val.SCHRIK : gok == 1 ? Val.KLOP : Val.MOBS;
	}

	/** R1.6: zoveel jumpscare-foto's zitten er in het resource pack (font {@code schrik_1} t/m {@code schrik_5}). */
	public static final int SCHRIK_FOTOS = 5;

	/**
	 * R1.6: welke foto een jumpscare laat zien.
	 *
	 * @param ingesteld 1 t/m {@link #SCHRIK_FOTOS} voor een vaste foto, 0 voor willekeurig
	 */
	public static int schrikFoto(int ingesteld, RandomGenerator random) {
		return ingesteld >= 1 && ingesteld <= SCHRIK_FOTOS ? ingesteld : 1 + random.nextInt(SCHRIK_FOTOS);
	}

	// Ronde 2

	public enum Gok {
		/** Haste II voor de hakker. */
		HASTE,
		/** Iedereen behalve de hakker bevroren. */
		BEVRIEZING
	}

	/** R2.5: de redstone-gok, 50/50. */
	public static Gok redstoneGok(RandomGenerator random) {
		return random.nextBoolean() ? Gok.HASTE : Gok.BEVRIEZING;
	}

	// Ronde 5

	/**
	 * R5.5 en R5.6: wie krijgt de kroon als de kroonhouder doodgaat? De killer; anders de laatste
	 * die hem raakte; anders een willekeurige levende jager. Alleen levende jagers komen in
	 * aanmerking. Leeg betekent: er is geen jager meer, de ronde is voorbij.
	 */
	public static Optional<UUID> kroonOpvolger(UUID killer, UUID laatsteHit, List<UUID> levendeJagers,
			RandomGenerator random) {
		if (killer != null && levendeJagers.contains(killer)) {
			return Optional.of(killer);
		}
		if (laatsteHit != null && levendeJagers.contains(laatsteHit)) {
			return Optional.of(laatsteHit);
		}
		if (levendeJagers.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(levendeJagers.get(random.nextInt(levendeJagers.size())));
	}

	/** R5.11: vanaf zoveel spelers over (de kroonhouder meegeteld) krijgt iedereen Strength I. */
	public static final int SLOTSTRIJD = 3;

	/**
	 * R5.11: de Strength in King of the Hill. De kroonhouder heeft Strength II, de jagers niets;
	 * zijn er nog {@link #SLOTSTRIJD} of minder over, dan heeft iedereen Strength I.
	 *
	 * @return het niveau (0 is Strength I, 1 is Strength II), of -1 voor geen Strength
	 */
	public static int kroonSterkte(int over, boolean kroonhouder) {
		if (over <= SLOTSTRIJD) {
			return 0;
		}
		return kroonhouder ? 1 : -1;
	}

	/** R5.8 en R6.2: geen timer, de ronde is voorbij zodra er nog één (of niemand) leeft. */
	public static boolean laatsteOver(int levend) {
		return levend <= 1;
	}

	/**
	 * R6.6: de finale is de winnaar van King of the Hill tegen de winnaar van de FFA. Won dezelfde
	 * speler allebei, dan speelt hij tegen de nummer twee van de FFA (wie als laatste afviel).
	 * Namen worden zonder hoofdletters vergeleken.
	 *
	 * @return de twee namen, King of the Hill eerst; leeg als er nog iemand ontbreekt
	 */
	public static List<String> finalisten(String kingOfTheHill, String ffa, String ffaTweede) {
		if (kingOfTheHill == null || ffa == null) {
			return List.of();
		}
		if (!kingOfTheHill.equalsIgnoreCase(ffa)) {
			return List.of(kingOfTheHill, ffa);
		}
		return ffaTweede == null || ffaTweede.equalsIgnoreCase(ffa) ? List.of() : List.of(ffa, ffaTweede);
	}

	/** R6.5: bij drie en bij twee over een title. */
	public static String aftelTitle(int over) {
		return switch (over) {
			case 3 -> "LAATSTE DRIE";
			case 2 -> "LAATSTE TWEE";
			default -> null;
		};
	}

	// Uitloggen en terugkomen

	public enum QuitActie {
		/** Niks aan de hand; de speler mag later gewoon terugkomen. */
		NIKS,
		/** Telt als dood: uit de ronde. */
		DOOD,
		/** De kroonhouder is weg: dertig seconden aftellen, dan gaat de kroon door. */
		KROON_WACHT,
		/** De finale pauzeert tot de commander kiest: combat log (de ander wint) of crash (opnieuw). */
		PAUZE
	}

	/**
	 * R7.1 t/m R7.3: wat gebeurt er als iemand met deze rol in deze ronde uitlogt?
	 *
	 * @param aanDeBeurt ronde 3: hij stond in een arena
	 */
	public static QuitActie bijQuit(Ronde ronde, Rol rol, boolean aanDeBeurt) {
		return switch (ronde) {
			case MOBARENA -> rol == Rol.SPELER && aanDeBeurt ? QuitActie.DOOD : QuitActie.NIKS;
			case CLOWN -> switch (rol) {
				case JAGER -> QuitActie.DOOD;
				case KROON -> QuitActie.KROON_WACHT;
				default -> QuitActie.NIKS;
			};
			case FFA -> rol == Rol.FFA ? QuitActie.DOOD : QuitActie.NIKS;
			case FINALE -> rol == Rol.FFA ? QuitActie.PAUZE : QuitActie.NIKS;
			default -> QuitActie.NIKS;
		};
	}

	public enum JoinActie {
		/** Laat de speler staan waar hij is. */
		NIKS,
		/** Doolhof: terug naar de startruimte. */
		DOOLHOF_START,
		/** Het Ei: terug op zijn eigen startplek, met zijn punten. */
		EIGEN_EI_SPAWN,
		/** Mob arena: op de tribune, hij mag nog spelen als hij niet af is. */
		MOB_TRIBUNE,
		/** Quiz: bij de bank van zijn team, de presentator op het podium. */
		QUIZ_BANK,
		/** Kijker op de tribune van dat moment. */
		KIJKER_TRIBUNE,
		/** De kroonhouder is binnen de dertig seconden terug: hij blijft kroonhouder. */
		KROON_TERUG
	}

	/**
	 * R7.4 en R7.5: wat gebeurt er met iemand die inlogt terwijl een ronde loopt?
	 *
	 * @param rol            de rol die de mod nog van hem kent ({@code null} = nooit gezien)
	 * @param heeftTeam      ronde 1: hij koos al een kleur
	 * @param kroonWachtLoopt de dertig seconden voor deze kroonhouder lopen nog
	 */
	public static JoinActie bijJoin(Ronde ronde, Rol rol, boolean heeftTeam, boolean kroonWachtLoopt) {
		if (rol == Rol.STAFF) {
			return JoinActie.NIKS;
		}
		return switch (ronde) {
			case BASISKAMP -> JoinActie.NIKS;
			// Met een team is hij al over de finish: verder waar hij uitlogde, ook terug in het doolhof.
			case DOOLHOF -> heeftTeam ? JoinActie.NIKS : JoinActie.DOOLHOF_START;
			case EI -> JoinActie.EIGEN_EI_SPAWN;
			case MOBARENA -> JoinActie.MOB_TRIBUNE;
			case QUIZ -> JoinActie.QUIZ_BANK;
			case CLOWN -> rol == Rol.KROON && kroonWachtLoopt ? JoinActie.KROON_TERUG : JoinActie.KIJKER_TRIBUNE;
			case FFA, FINALE -> JoinActie.KIJKER_TRIBUNE;
		};
	}
}
