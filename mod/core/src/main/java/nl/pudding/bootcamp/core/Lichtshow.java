package nl.pudding.bootcamp.core;

/**
 * R4.6: de lichtshow van de decorlampen in de quizhal: alle redstone lampen in regio {@code quiz}
 * behalve die van de banken. Er lopen {@link #BALKEN} lichtbalken met de klok mee rond het midden
 * van de hal. Tijdens de quiz rustig; draait het rad, dan draaien ze mee met het rad; op het
 * plingeltje knipperen ze mee met het gekozen vak.
 *
 * <p>Alles in rondjes: een hoek van 0 tot 1, met de klok mee vanaf het noorden, van boven gezien.
 */
public final class Lichtshow {
	/** Zoveel lichtbalken lopen er tegelijk rond. */
	public static final int BALKEN = 4;
	/** Zo'n deel van elke balk brandt. */
	public static final double AAN = 0.4;
	/** Rustig rond tijdens de quiz: zoveel rondjes per seconde (één rondje in 16 seconden). */
	public static final double RUST = 1.0 / 16;

	private Lichtshow() {
	}

	/**
	 * Waar een lamp staat ten opzichte van het midden, met de klok mee vanaf het noorden, in rondjes.
	 * Minecraft: x is oost, z is zuid; noorden is dus -z.
	 */
	public static double hoek(double dx, double dz) {
		double h = Math.atan2(dx, -dz) / (2 * Math.PI);
		return h < 0 ? h + 1 : h;
	}

	/** Brandt een lamp op deze hoek als de show zo ver rond is? De balken schuiven met de klok mee. */
	public static boolean aan(double hoek, double rond) {
		double d = (hoek - rond) * BALKEN;
		return d - Math.floor(d) < AAN;
	}
}
