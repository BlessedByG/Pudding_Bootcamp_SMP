package nl.pudding.bootcamp.core;

import java.util.ArrayList;
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
	/** R2.5: redstone-gok. */
	public static final int EI_HASTE = 10;
	public static final int EI_BEVRIEZING = 15;
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

	/** R5.8 en R6.2: geen timer, de ronde is voorbij zodra er nog één (of niemand) leeft. */
	public static boolean laatsteOver(int levend) {
		return levend <= 1;
	}

	/** R6.1: iedereen behalve de uitverkorene doet mee aan de FFA, ook wie in ronde 5 af was. */
	public static List<UUID> ffaDeelnemers(List<UUID> spelers, UUID uitverkorene) {
		List<UUID> uit = new ArrayList<>();
		for (UUID s : spelers) {
			if (!s.equals(uitverkorene)) {
				uit.add(s);
			}
		}
		return uit;
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
		KROON_WACHT
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
			default -> QuitActie.NIKS;
		};
	}

	public enum JoinActie {
		/** Laat de speler staan waar hij is. */
		NIKS,
		/** Doolhof: terug naar de startruimte. */
		DOOLHOF_START,
		/** Doolhof: had al een team, dus naar {@code v2}. */
		NAAR_V2,
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
			case DOOLHOF -> heeftTeam ? JoinActie.NAAR_V2 : JoinActie.DOOLHOF_START;
			case EI -> JoinActie.EIGEN_EI_SPAWN;
			case MOBARENA -> JoinActie.MOB_TRIBUNE;
			case QUIZ -> JoinActie.QUIZ_BANK;
			case CLOWN -> rol == Rol.KROON && kroonWachtLoopt ? JoinActie.KROON_TERUG : JoinActie.KIJKER_TRIBUNE;
			case FFA -> JoinActie.KIJKER_TRIBUNE;
		};
	}
}
