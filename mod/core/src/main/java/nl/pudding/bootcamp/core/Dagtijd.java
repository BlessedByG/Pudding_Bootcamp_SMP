package nl.pudding.bootcamp.core;

/**
 * R1.10: rekenen met de tijd van de dag, voor een vloeiende overgang naar de nacht (het gif van het
 * doolhof) en terug naar de dag. Een dag is 24000 ticks; 6000 is middag, 18000 middernacht.
 */
public final class Dagtijd {
	public static final int DAG = 24000;
	public static final int MIDDAG = 6000;
	public static final int MIDDERNACHT = 18000;

	private Dagtijd() {
	}

	/** Hoeveel ticks vooruit tot de volgende keer dat het deze tijd van de dag is; 0 als het al zo is. */
	public static long totDagtijd(long totaal, int dagtijd) {
		return Math.floorMod(dagtijd - totaal, (long) DAG);
	}

	/** Hoeveel ticks de klok per servertick vooruit moet om die afstand in zoveel ticks af te leggen. */
	public static int stap(long afstand, int ticks) {
		return (int) Math.max(1, Math.ceil((double) afstand / Math.max(1, ticks)));
	}
}
