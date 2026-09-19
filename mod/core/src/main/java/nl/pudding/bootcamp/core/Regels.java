package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * De spelregels uit docs/02 en docs/03 die rekenen of beslissen, als pure functies. De nummers
 * verwijzen naar {@code REGELS.md}.
 */
public final class Regels {
	/** R1.2: de eerste vijf uit het doolhof krijgen het voorsprongkistje. */
	public static final int VOORSPRONG_PLEKKEN = 5;
	/** R1.3: hint na zeven minuten, dus met drie minuten op de klok. */
	public static final int DOOLHOF_HINT_BIJ = 3 * 60;
	/** R3.3 en R3.4: beacon met vijf minuten op de klok, vuurpijl met drie. */
	public static final int EI_BEACON_BIJ = 5 * 60;
	public static final int EI_VUURPIJL_BIJ = 3 * 60;
	/** R4.2 en R4.6: voorsprong van de koning bij de start, en de opstelling na een kroonwissel. */
	public static final int OPSTELLING_START = 30;
	public static final int OPSTELLING_WISSEL = 10;
	/** R4.9: zo lang wacht de mod op een koning die uitlogt. */
	public static final int KONING_UITLOG_WACHT = 30;
	/** R4.5: Resistance II voor de nieuwe koning. */
	public static final int KONING_RESISTANCE = 15;
	/** R5.3: na vijf minuten krimpt de border in twee minuten naar tien. */
	public static final int FFA_KRIMP_NA = 5 * 60;
	public static final int FFA_KRIMP_DUUR = 2 * 60;
	public static final int FFA_KRIMP_NAAR = 10;
	public static final int FFA_COUNTDOWN = 10;
	/** R5.6: rust tussen de FFA en de finale. */
	public static final int RUST = 2 * 60;
	/** R6.1 en R6.3. */
	public static final int FINALE_BORDER = 20;
	public static final int FINALE_KRIMP_NA = 3 * 60;
	public static final int FINALE_KRIMP_DUUR = 30;
	public static final int FINALE_KRIMP_NAAR = 6;
	public static final int FINALE_COUNTDOWN = 5;
	public static final int FINALE_WINST = 2;
	public static final int KRONING_VUURWERK = 20;

	private Regels() {
	}

	// Ronde 1

	/** R1.2: krijgt de speler die als {@code aankomst}-de (vanaf 1) uit het doolhof komt een kistje? */
	public static boolean krijgtVoorsprong(int aankomst) {
		return aankomst >= 1 && aankomst <= VOORSPRONG_PLEKKEN;
	}

	// Ronde 4

	/**
	 * R4.3 en R4.4: wie krijgt de kroon als de koning doodgaat? De killer; anders de laatste die
	 * hem raakte; anders een willekeurige levende hunter. Alleen levende hunters komen in
	 * aanmerking. Leeg betekent: er is geen hunter meer, de ronde is voorbij.
	 */
	public static Optional<UUID> kroonOpvolger(UUID killer, UUID laatsteHit, List<UUID> levendeHunters,
			RandomGenerator random) {
		if (killer != null && levendeHunters.contains(killer)) {
			return Optional.of(killer);
		}
		if (laatsteHit != null && levendeHunters.contains(laatsteHit)) {
			return Optional.of(laatsteHit);
		}
		if (levendeHunters.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(levendeHunters.get(random.nextInt(levendeHunters.size())));
	}

	/** R4.7: ronde 4 is voorbij bij timer nul, of zodra er geen levende hunter meer is. */
	public static boolean ronde4Voorbij(int timerSeconden, int levendeHunters) {
		return timerSeconden <= 0 || levendeHunters <= 0;
	}

	/** R4.1: hunters (en FFA-spelers) om en om over de startpunten: index 0 op punt 1, enzovoort. */
	public static int startpunt(int index, int aantalPunten) {
		return Math.floorMod(index, aantalPunten) + 1;
	}

	// Ronde 5

	/**
	 * R5.1: iedereen behalve Clown en finalist 1 doet mee aan de FFA, ook wie in ronde 4 doodging.
	 * Clown doet nooit mee, ook niet als hij de kroon kwijt is.
	 */
	public static List<UUID> ffaDeelnemers(Collection<UUID> spelers, UUID clown, UUID finalist1) {
		List<UUID> uit = new ArrayList<>();
		for (UUID s : spelers) {
			if (!s.equals(clown) && !s.equals(finalist1)) {
				uit.add(s);
			}
		}
		return uit;
	}

	/** Stand van een FFA-speler die nog leeft als de tien minuten om zijn. */
	public record FfaStand(UUID speler, int kills, float hp) {
	}

	/**
	 * R5.5: staan er bij het harde maximum nog meerdere spelers, dan beslist het aantal kills.
	 * Aanname bij een gelijke stand (de docs zeggen er niks over): de meeste hp, daarna het lot.
	 */
	public static Optional<UUID> ffaTiebreak(List<FfaStand> levend, RandomGenerator random) {
		if (levend.isEmpty()) {
			return Optional.empty();
		}
		List<FfaStand> gesorteerd = new ArrayList<>(levend);
		gesorteerd.sort(Comparator.comparingInt(FfaStand::kills).reversed()
				.thenComparing(Comparator.comparingDouble(FfaStand::hp).reversed()));
		FfaStand beste = gesorteerd.get(0);
		List<FfaStand> gelijk = gesorteerd.stream()
				.filter(s -> s.kills() == beste.kills() && s.hp() == beste.hp())
				.toList();
		return Optional.of(gelijk.get(random.nextInt(gelijk.size())).speler());
	}

	// Uitloggen en terugkomen

	public enum QuitActie {
		/** Niks aan de hand; de speler mag later gewoon terugkomen. */
		NIKS,
		/** Telt als dood: uit de ronde. */
		DOOD,
		/** De koning is weg: dertig seconden aftellen, dan gaat de kroon door. */
		KONING_WACHT,
		/** Een finalist is weg tijdens de finale: het lopende potje gaat naar de tegenstander. */
		POTJE_VERLOREN
	}

	/** R7.1 t/m R7.3: wat gebeurt er als iemand met deze rol in deze ronde uitlogt? */
	public static QuitActie bijQuit(Ronde ronde, Rol rol) {
		return switch (ronde) {
			case KING -> switch (rol) {
				case HUNTER -> QuitActie.DOOD;
				case KING -> QuitActie.KONING_WACHT;
				default -> QuitActie.NIKS;
			};
			case FFA -> rol == Rol.FFA ? QuitActie.DOOD : QuitActie.NIKS;
			case FINALE -> rol == Rol.KING ? QuitActie.POTJE_VERLOREN : QuitActie.NIKS;
			case HORDE -> rol == Rol.SPELER ? QuitActie.DOOD : QuitActie.NIKS;
			default -> QuitActie.NIKS;
		};
	}

	public enum JoinActie {
		/** Laat de speler staan waar hij is. */
		NIKS,
		/** Naar het startpunt van de lopende ronde (doolhof-ingang, bosrand). */
		STARTPUNT,
		/** Was al klaar met de ronde: naar het volgende verzamelpunt. */
		VOLGEND_VERZAMELPUNT,
		/** Kijker op de tribune van dat moment. */
		KIJKER_TRIBUNE,
		/** De koning is binnen de dertig seconden terug: hij blijft koning. */
		KONING_TERUG,
		/** Een finalist komt terug tijdens de finale of de rust: hij blijft finalist. */
		FINALIST_TERUG
	}

	/**
	 * R7.4 en R7.5: wat gebeurt er met iemand die inlogt terwijl een ronde loopt?
	 *
	 * @param rol             de rol die de mod nog van hem kent ({@code null} = nooit gezien)
	 * @param klaarMetRonde   hij was al uit het doolhof, of had zijn ticket al
	 * @param koningWachtLoopt de dertig seconden voor deze koning lopen nog
	 */
	public static JoinActie bijJoin(Ronde ronde, Rol rol, boolean klaarMetRonde, boolean koningWachtLoopt) {
		if (rol == Rol.STAFF) {
			return JoinActie.NIKS;
		}
		return switch (ronde) {
			case BASISKAMP -> JoinActie.NIKS;
			case DOOLHOF, EI -> klaarMetRonde ? JoinActie.VOLGEND_VERZAMELPUNT : JoinActie.STARTPUNT;
			case HORDE -> JoinActie.KIJKER_TRIBUNE;
			case KING -> rol == Rol.KING && koningWachtLoopt ? JoinActie.KONING_TERUG : JoinActie.KIJKER_TRIBUNE;
			case FFA -> rol == Rol.FINALIST ? JoinActie.FINALIST_TERUG : JoinActie.KIJKER_TRIBUNE;
			case FINALE -> rol == Rol.KING || rol == Rol.FINALIST ? JoinActie.FINALIST_TERUG : JoinActie.KIJKER_TRIBUNE;
		};
	}

	// Horde

	/**
	 * R2.1: grofweg één mob per speler per wave. Het aantal uit {@code waves.json} geldt voor twintig
	 * spelers en schaalt mee; een wave die niet schaalt (de boss wave) blijft vast.
	 */
	public static int schaalMobs(int aantalBij20, int spelers, boolean schaal) {
		if (!schaal) {
			return aantalBij20;
		}
		return Math.max(1, (int) Math.ceil(aantalBij20 * Math.max(1, spelers) / 20.0));
	}
}
