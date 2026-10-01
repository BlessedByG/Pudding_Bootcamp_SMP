package nl.pudding.bootcamp.core;

import java.util.random.RandomGenerator;

/**
 * R4.3: een draai van het quiz-rad op de maat van het geluid {@code bootcamp:rad} (spinwheel.wav,
 * in het pack {@code rad.ogg}). Het geluid start op tick 0 en het rad volgt een vaste tijdlijn:
 * <ol>
 * <li>van 0,05 s tot 0,75 s komt het rad op snelheid, en tot het eerste gevolgde tikje draait het
 * zo snel als het scherm het netjes kan laten zien (20 beelden per seconde; nog sneller en het
 * rad lijkt achteruit te draaien);</li>
 * <li>bij elk van de laatste 20 tikjes ({@link #TIKJES}, 4,48 s tot 9,18 s) gaat er precies een
 * vakgrens onder het pijltje door;</li>
 * <li>na het laatste tikje schuift het rad af remmend naar het midden van het laatste vak en staat
 * stil ({@link #STIL}, rond 9,68 s);</li>
 * <li>bij het plingeltje ({@link #PLING}, 10,17 s) komt de uitslag.</li>
 * </ol>
 * Het doelvak is willekeurig (elk team 25%); de start volgt uit het doel, zodat het rad er altijd
 * precies op landt.
 */
public final class QuizDraai {
	public static final int TICKS_PER_SECONDE = 20;
	/** Het geluid begint: het rad komt in beweging. In seconden vanaf de start van het geluid. */
	static final double BEGIN = 0.05;
	/** Vanaf hier draait het rad op topsnelheid. */
	static final double OP_SNELHEID = 0.75;
	/**
	 * De tikjes die het rad volgt, gemeten in spinwheel.wav: bij elk tikje gaat er een vakgrens onder
	 * het pijltje door. Daarvoor tikt het geluid sneller dan 20 beelden per seconde kunnen volgen.
	 */
	static final double[] TIKJES = {
			4.478, 4.606, 4.746, 4.884, 5.034, 5.188, 5.354, 5.526, 5.712, 5.908,
			6.118, 6.342, 6.584, 6.848, 7.136, 7.452, 7.808, 8.210, 8.674, 9.176};
	/** Het plingeltje: dan komt de uitslag. */
	public static final double PLING = 10.174;
	/** Afgelegde standen bij het eerste gevolgde tikje: een vakgrens (2 voorbij een vakmidden). */
	static final int VOOR_DE_TIKJES = 126;
	/** Zoveel standen draait een draai in totaal: van een vakmidden naar een vakmidden. */
	public static final int STANDEN_TOTAAL = VOOR_DE_TIKJES + QuizRad.PER_VAK * (TIKJES.length - 1) + QuizRad.PER_VAK / 2;

	/** Topsnelheid in standen per seconde, zo dat het rad bij het eerste tikje precies op een vakgrens staat. */
	static final double TOP = VOOR_DE_TIKJES / ((OP_SNELHEID - BEGIN) / 2 + (TIKJES[0] - OP_SNELHEID));
	/** De snelheid bij het laatste tikje, en hoe lang het daarna nog remt over het halve vak. */
	private static final double LAATSTE_SNELHEID = QuizRad.PER_VAK / (TIKJES[TIKJES.length - 1] - TIKJES[TIKJES.length - 2]);
	private static final double REMTIJD = 2.0 * (QuizRad.PER_VAK / 2.0) / LAATSTE_SNELHEID;
	/** Het rad staat stil op het midden van het laatste vak. */
	public static final double STIL = TIKJES[TIKJES.length - 1] + REMTIJD;
	/** De tick van de uitslag, gerekend vanaf de draai: de tick die het dichtst bij het plingeltje ligt. */
	public static final int UITSLAG_TICK = (int) Math.round(PLING * TICKS_PER_SECONDE);
	/** Vanaf zoveel seconden na de draai komt de uitslag (de tick van het plingeltje). */
	public static final double UITSLAG = (double) UITSLAG_TICK / TICKS_PER_SECONDE;

	private final int vak;
	private final int start;

	/** Een draai die landt op het midden van dit vak. */
	public QuizDraai(int vak) {
		if (vak < 0 || vak >= QuizRad.VAKKEN.size()) {
			throw new IllegalArgumentException("geen vak: " + vak);
		}
		this.vak = vak;
		this.start = Math.floorMod(QuizRad.standVanVak(vak) - STANDEN_TOTAAL, QuizRad.STANDEN);
	}

	/** Een draai naar een willekeurig vak: elk team 25%. */
	public static QuizDraai willekeurig(RandomGenerator random) {
		return new QuizDraai(random.nextInt(QuizRad.VAKKEN.size()));
	}

	/** Hoeveel standen het rad is opgeschoven, t seconden na de start van het geluid (niet afgerond). */
	static double afstand(double t) {
		if (t <= BEGIN) {
			return 0;
		}
		if (t < OP_SNELHEID) {
			double a = TOP / (OP_SNELHEID - BEGIN);
			return a * (t - BEGIN) * (t - BEGIN) / 2;
		}
		if (t < TIKJES[0]) {
			return TOP * (OP_SNELHEID - BEGIN) / 2 + TOP * (t - OP_SNELHEID);
		}
		int laatste = TIKJES.length - 1;
		for (int k = 0; k < laatste; k++) {
			if (t < TIKJES[k + 1]) {
				double deel = (t - TIKJES[k]) / (TIKJES[k + 1] - TIKJES[k]);
				return VOOR_DE_TIKJES + QuizRad.PER_VAK * (k + deel);
			}
		}
		// Na het laatste tikje: gelijkmatig afremmen tot stilstand op het vakmidden.
		double na = Math.min(t - TIKJES[laatste], REMTIJD);
		double rem = LAATSTE_SNELHEID / REMTIJD;
		return VOOR_DE_TIKJES + QuizRad.PER_VAK * laatste + LAATSTE_SNELHEID * na - rem * na * na / 2;
	}

	/**
	 * De stand van het plaatje, zoveel seconden na de draai. In seconden en niet in ticks: hapert de
	 * server, dan loopt het rad toch gelijk met het geluid, dat bij de spelers gewoon doorspeelt.
	 */
	public int stand(double seconden) {
		return Math.floorMod(start + (int) Math.floor(afstand(seconden) + 0.5), QuizRad.STANDEN);
	}

	/** Staat het rad zoveel seconden na de draai stil? */
	public boolean stil(double seconden) {
		return seconden >= STIL;
	}

	/** Is het zoveel seconden na de draai tijd voor de uitslag (het plingeltje)? */
	public boolean uitslag(double seconden) {
		return seconden >= UITSLAG - 1e-9;
	}

	public int vak() {
		return vak;
	}

	public Kleur kleur() {
		return QuizRad.VAKKEN.get(vak);
	}
}
