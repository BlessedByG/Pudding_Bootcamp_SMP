package nl.pudding.bootcamp.core;

/**
 * Een regio uit de wand-selectie: twee hoeken, genormaliseerd naar {@code min} en {@code max}
 * (beide inclusief).
 *
 * <p>Voor spelers telt een regio als kolom: {@link #bevat(double, double)} kijkt alleen naar x en
 * z. Zo werkt een selectie van twee hoeken op de grond ook voor wie erop staat, en voor een vloer
 * met hoogteverschil. Poorten gebruiken wel de hele doos ({@link #min()} t/m {@link #max()}).
 */
public record Regio(BlokPos min, BlokPos max) {

	public static Regio van(BlokPos a, BlokPos b) {
		return new Regio(
				new BlokPos(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
				new BlokPos(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
	}

	/** Midden van de regio op de x-as, in wereldcoördinaten (blokken lopen van n tot n + 1). */
	public double centerX() {
		return (min.x() + max.x() + 1) / 2.0;
	}

	public double centerZ() {
		return (min.z() + max.z() + 1) / 2.0;
	}

	public int breedteX() {
		return max.x() - min.x() + 1;
	}

	public int breedteZ() {
		return max.z() - min.z() + 1;
	}

	public int hoogte() {
		return max.y() - min.y() + 1;
	}

	/** Grootte voor de worldborder: de border is vierkant, dus de langste zijde. */
	public int grootte() {
		return Math.max(breedteX(), breedteZ());
	}

	/** Staat deze x/z binnen de kolom van de regio? */
	public boolean bevat(double x, double z) {
		return x >= min.x() && x < max.x() + 1 && z >= min.z() && z < max.z() + 1;
	}

	/** Staat dit punt binnen de doos, dus ook op hoogte? */
	public boolean bevatDoos(double x, double y, double z) {
		return bevat(x, z) && y >= min.y() && y < max.y() + 1;
	}

	public long aantalBlokken() {
		return (long) breedteX() * hoogte() * breedteZ();
	}
}
