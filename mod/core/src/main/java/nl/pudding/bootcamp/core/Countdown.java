package nl.pudding.bootcamp.core;

/**
 * Een aftelling als tick-state: geen threads, geen sleeps. Roep elke servertick {@link #tick()}
 * aan; die geeft op elke hele seconde het aantal resterende seconden terug.
 */
public final class Countdown {
	/** Geen hele seconde verstreken deze tick. */
	public static final int NIKS = -1;

	private int ticksOver;

	public Countdown(int seconden) {
		this.ticksOver = Math.max(0, seconden) * Tijd.TICKS_PER_SECONDE;
	}

	/**
	 * @return het aantal resterende seconden als er net een seconde om is (0 = klaar, precies één
	 *         keer), anders {@link #NIKS}
	 */
	public int tick() {
		if (ticksOver <= 0) {
			return NIKS;
		}
		ticksOver--;
		return ticksOver % Tijd.TICKS_PER_SECONDE == 0 ? ticksOver / Tijd.TICKS_PER_SECONDE : NIKS;
	}

	public boolean klaar() {
		return ticksOver <= 0;
	}

	public int secondenOver() {
		return (ticksOver + Tijd.TICKS_PER_SECONDE - 1) / Tijd.TICKS_PER_SECONDE;
	}
}
