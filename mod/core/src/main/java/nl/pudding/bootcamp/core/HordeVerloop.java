package nl.pudding.bootcamp.core;

/**
 * Het verloop van de horde: welke wave loopt, wanneer de volgende start en wanneer de ronde
 * voorbij is. Roep elke seconde {@link #seconde(int, int)} aan.
 */
public final class HordeVerloop {
	/** R2.2: een wave start als de vorige dood is, of na twee minuten. */
	public static final int MAX_WAVE_SECONDEN = 120;

	public enum Gebeurtenis {
		NIKS,
		/** Spawn wave {@link #wave()}. */
		START_WAVE,
		/** R2.4: de laatste wave is dood. */
		GEWONNEN,
		/** R2.4: iedereen is dood. */
		IEDEREEN_DOOD
	}

	private final int aantalWaves;
	private int wave;
	private int secondenInWave;
	private boolean voorbij;

	public HordeVerloop(int aantalWaves) {
		this.aantalWaves = aantalWaves;
	}

	/**
	 * @param mobsOver        levende horde-mobs
	 * @param levendeSpelers  spelers die nog op de vloer staan
	 */
	public Gebeurtenis seconde(int mobsOver, int levendeSpelers) {
		if (voorbij) {
			return Gebeurtenis.NIKS;
		}
		if (levendeSpelers <= 0) {
			voorbij = true;
			return Gebeurtenis.IEDEREEN_DOOD;
		}
		if (wave == 0) {
			return volgende();
		}
		secondenInWave++;
		if (mobsOver <= 0) {
			if (wave >= aantalWaves) {
				voorbij = true;
				return Gebeurtenis.GEWONNEN;
			}
			return volgende();
		}
		if (secondenInWave >= MAX_WAVE_SECONDEN && wave < aantalWaves) {
			return volgende();
		}
		return Gebeurtenis.NIKS;
	}

	private Gebeurtenis volgende() {
		wave++;
		secondenInWave = 0;
		return Gebeurtenis.START_WAVE;
	}

	/** De lopende wave, vanaf 1; 0 als er nog geen gestart is. */
	public int wave() {
		return wave;
	}

	public boolean voorbij() {
		return voorbij;
	}
}
