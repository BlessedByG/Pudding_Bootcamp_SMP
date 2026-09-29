package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Een regio uit {@code bootcamp.json}: één of meer dozen uit de wand-selectie, of een staande
 * cilinder ({@code /clown vloer <diameter>}).
 *
 * <p>Voor spelers telt een doos als kolom: {@link #bevat(double, double)} kijkt alleen naar x en
 * z. Zo werkt een selectie van twee hoeken op de grond ook voor wie erop staat. Een plek hoort bij
 * de regio als hij in één van de delen ligt; delen mogen elkaar overlappen (een T-vormig veld is
 * een balk plus een poot). Bij een cilinder telt de hoogte wel: {@link #bevatSpeler} kijkt of je
 * binnen de cirkel én binnen zijn hoogte staat.
 *
 * <p>Waar de mod één doos nodig heeft (worldborder, poort, kistenscan, het Ei) gebruikt hij
 * {@link #omhullende()}: de doos om alle delen heen, of het vierkant om de cirkel.
 */
public final class Regio {

	/** Eén doos, genormaliseerd naar {@code min} en {@code max} (beide inclusief). */
	public record Doos(BlokPos min, BlokPos max) {
		public static Doos van(BlokPos a, BlokPos b) {
			return new Doos(
					new BlokPos(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
					new BlokPos(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
		}

		/** Staat deze x/z binnen de kolom van de doos? */
		public boolean bevat(double x, double z) {
			return x >= min.x() && x < max.x() + 1 && z >= min.z() && z < max.z() + 1;
		}

		/** Staat dit punt binnen de doos, dus ook op hoogte? */
		public boolean bevatDoos(double x, double y, double z) {
			return bevat(x, z) && y >= min.y() && y < max.y() + 1;
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

		public long aantalBlokken() {
			return (long) breedteX() * hoogte() * breedteZ();
		}
	}

	/**
	 * Een staande cilinder: midden op (x, z), onderkant op blokhoogte y, doorsnede en hoogte in
	 * blokken.
	 */
	public record Cilinder(double x, double z, int y, int diameter, int hoogte) {
		public double straal() {
			return diameter / 2.0;
		}

		public boolean bevat(double px, double pz) {
			double dx = px - x;
			double dz = pz - z;
			return dx * dx + dz * dz <= straal() * straal();
		}

		public boolean bevatPlek(double px, double py, double pz) {
			return bevat(px, pz) && py >= y && py < y + hoogte;
		}

		/** Het vierkant om de cirkel, in hele blokken. */
		public Doos omhullende() {
			double r = straal();
			return new Doos(
					new BlokPos((int) Math.floor(x - r), y, (int) Math.floor(z - r)),
					new BlokPos((int) Math.ceil(x + r) - 1, y + hoogte - 1, (int) Math.ceil(z + r) - 1));
		}
	}

	private final List<Doos> delen;
	private final Cilinder cilinder;

	private Regio(List<Doos> delen, Cilinder cilinder) {
		this.delen = List.copyOf(delen);
		this.cilinder = cilinder;
	}

	/** Een regio van één doos met deze twee hoeken. */
	public static Regio van(BlokPos a, BlokPos b) {
		return new Regio(List.of(Doos.van(a, b)), null);
	}

	/** Een regio uit deze dozen; minstens één. */
	public static Regio uitDelen(List<Doos> delen) {
		if (delen == null || delen.isEmpty()) {
			throw new IllegalArgumentException("een regio heeft minstens één deel");
		}
		return new Regio(delen, null);
	}

	public static Regio cilinder(double x, double z, int y, int diameter, int hoogte) {
		if (diameter < 1 || hoogte < 1) {
			throw new IllegalArgumentException("doorsnede en hoogte moeten minstens 1 zijn");
		}
		return new Regio(List.of(), new Cilinder(x, z, y, diameter, hoogte));
	}

	/** Deze regio met één doos erbij ({@code /bc region add}). Een cilinder krijgt geen delen. */
	public Regio metDeel(BlokPos a, BlokPos b) {
		if (cilinder != null) {
			throw new IllegalArgumentException("een cirkel krijgt geen extra delen; maak hem opnieuw");
		}
		List<Doos> nieuw = new ArrayList<>(delen);
		nieuw.add(Doos.van(a, b));
		return new Regio(nieuw, null);
	}

	/** De dozen; leeg bij een cilinder. */
	public List<Doos> delen() {
		return delen;
	}

	/** De cilinder, of {@code null} als dit dozen zijn. */
	public Cilinder cilinder() {
		return cilinder;
	}

	public boolean isCilinder() {
		return cilinder != null;
	}

	public int aantalDelen() {
		return cilinder != null ? 1 : delen.size();
	}

	/** De doos om alle delen heen; bij een cilinder het vierkant om de cirkel. */
	public Doos omhullende() {
		if (cilinder != null) {
			return cilinder.omhullende();
		}
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (Doos d : delen) {
			minX = Math.min(minX, d.min().x());
			minY = Math.min(minY, d.min().y());
			minZ = Math.min(minZ, d.min().z());
			maxX = Math.max(maxX, d.max().x());
			maxY = Math.max(maxY, d.max().y());
			maxZ = Math.max(maxZ, d.max().z());
		}
		return new Doos(new BlokPos(minX, minY, minZ), new BlokPos(maxX, maxY, maxZ));
	}

	public BlokPos min() {
		return omhullende().min();
	}

	public BlokPos max() {
		return omhullende().max();
	}

	/** Midden op de x-as, in wereldcoördinaten (blokken lopen van n tot n + 1). */
	public double centerX() {
		if (cilinder != null) {
			return cilinder.x();
		}
		Doos o = omhullende();
		return (o.min().x() + o.max().x() + 1) / 2.0;
	}

	public double centerZ() {
		if (cilinder != null) {
			return cilinder.z();
		}
		Doos o = omhullende();
		return (o.min().z() + o.max().z() + 1) / 2.0;
	}

	public int breedteX() {
		return omhullende().breedteX();
	}

	public int breedteZ() {
		return omhullende().breedteZ();
	}

	public int hoogte() {
		return omhullende().hoogte();
	}

	/** Grootte voor de worldborder: de border is vierkant, dus de langste zijde (of de doorsnede). */
	public int grootte() {
		if (cilinder != null) {
			return cilinder.diameter();
		}
		return Math.max(breedteX(), breedteZ());
	}

	/** Staat deze x/z binnen de kolom van één van de delen (of binnen de cirkel)? */
	public boolean bevat(double x, double z) {
		if (cilinder != null) {
			return cilinder.bevat(x, z);
		}
		for (Doos d : delen) {
			if (d.bevat(x, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Staat een speler op deze plek in de regio? Voor dozen telt de kolom, voor een cilinder ook de
	 * hoogte: wie hoger staat, bijvoorbeeld op de tribune, is er niet in.
	 */
	public boolean bevatSpeler(double x, double y, double z) {
		if (cilinder != null) {
			return cilinder.bevatPlek(x, y, z);
		}
		return bevat(x, z);
	}

	/**
	 * Staat een speler in de regio, waarbij een doos niet hoger telt dan {@code extra} blokken boven
	 * de selectie? Voor een veld met een balkon erboven: wie op het balkon staat is niet in het veld.
	 */
	public boolean bevatSpelerTot(double x, double y, double z, int extra) {
		if (cilinder != null) {
			return cilinder.bevatPlek(x, y, z);
		}
		for (Doos d : delen) {
			if (d.bevat(x, z) && y >= d.min().y() - 1 && y < d.max().y() + 1 + extra) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Staat een speler op deze regio, met de selectie als vloer? Zijn voeten zitten niet lager dan
	 * de selectie en niet hoger dan {@code extra} blokken erboven. Voor een tribune die over een
	 * veld hangt: wie eronder op het veld staat, staat er niet op.
	 */
	public boolean staatOp(double x, double y, double z, int extra) {
		if (cilinder != null) {
			return cilinder.bevatPlek(x, y, z);
		}
		for (Doos d : delen) {
			if (d.bevat(x, z) && y >= d.min().y() && y < d.max().y() + 1 + extra) {
				return true;
			}
		}
		return false;
	}

	/** Staat dit punt binnen één van de dozen, dus ook op hoogte? */
	public boolean bevatDoos(double x, double y, double z) {
		if (cilinder != null) {
			return cilinder.bevatPlek(x, y, z);
		}
		for (Doos d : delen) {
			if (d.bevatDoos(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/** Het aantal blokken in de omhullende doos. */
	public long aantalBlokken() {
		return omhullende().aantalBlokken();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Regio r && delen.equals(r.delen) && Objects.equals(cilinder, r.cilinder);
	}

	@Override
	public int hashCode() {
		return Objects.hash(delen, cilinder);
	}

	@Override
	public String toString() {
		return cilinder != null ? "Regio" + cilinder : "Regio" + delen;
	}
}
