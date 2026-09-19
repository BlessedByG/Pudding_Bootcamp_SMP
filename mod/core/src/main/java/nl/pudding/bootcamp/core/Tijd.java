package nl.pudding.bootcamp.core;

/** Tijd-hulpjes. Eén seconde is twintig ticks. */
public final class Tijd {
	public static final int TICKS_PER_SECONDE = 20;

	private Tijd() {
	}

	/** {@code 09:41}: twee cijfers voor de minuten, voor de bossbar. */
	public static String mmss(int seconden) {
		int s = Math.max(0, seconden);
		return String.format("%02d:%02d", s / 60, s % 60);
	}

	/** {@code 4:12}: voor de regeerperiodes in de sidebar. */
	public static String mss(int seconden) {
		int s = Math.max(0, seconden);
		return String.format("%d:%02d", s / 60, s % 60);
	}
}
