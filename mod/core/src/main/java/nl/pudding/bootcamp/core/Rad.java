package nl.pudding.bootcamp.core;

import java.util.random.RandomGenerator;

/**
 * Een rad dat rondloopt, steeds langzamer, en altijd op het doelslot stopt. De startpositie en het
 * aantal rondes zijn echt willekeurig; de landing ligt vast. Het Rad van ronde 5 loopt over de
 * deelnemers (rigged op de uitverkorene). Het quiz-rad loopt op de maat van zijn geluid: zie
 * {@link QuizDraai}.
 *
 * <p>Tick-gestuurd: roep elke servertick {@link #tick()} aan.
 */
public final class Rad {
	/** Hoe snel het rad loopt: van {@code snelste} ticks per stap naar {@code traagste}. */
	public record Ritme(int snelste, int traagste, int vertraagVanaf) {
		public Ritme {
			if (snelste < 1 || traagste < snelste || vertraagVanaf < 1) {
				throw new IllegalArgumentException("ongeldig ritme");
			}
		}
	}

	/** Het Rad van ronde 5: 2 ticks per stap, de laatste 25 stappen oplopend naar 30. */
	public static final Ritme KROON = new Ritme(2, 30, 25);

	public static final int SNELSTE_STAP = KROON.snelste();
	public static final int TRAAGSTE_STAP = KROON.traagste();
	public static final int VERTRAAG_VANAF = KROON.vertraagVanaf();

	public enum Gebeurtenis {
		/** Deze tick gebeurt er niks. */
		NIKS,
		/** Het rad is een slot opgeschoven: van {@link #vorigePos()} naar {@link #pos()}. */
		STAP,
		/** Laatste stap: het rad staat op het doel en blijft daar. */
		GELAND
	}

	private final int slots;
	private final int doel;
	private final Ritme ritme;
	private int pos;
	private int vorigePos;
	private int rest;
	private int wacht;
	private boolean geland;

	public Rad(int slots, int doel, int startPos, int rondes) {
		this(slots, doel, startPos, rondes, KROON);
	}

	/**
	 * @param doel     het slot waar het rad op landt
	 * @param startPos waar het rad begint
	 * @param rondes   hoeveel volle rondes het eerst loopt (2 of 3)
	 */
	public Rad(int slots, int doel, int startPos, int rondes, Ritme ritme) {
		if (slots <= 0 || doel < 0 || doel >= slots || startPos < 0 || startPos >= slots || rondes < 0) {
			throw new IllegalArgumentException("ongeldig rad: slots=" + slots + " doel=" + doel
					+ " start=" + startPos + " rondes=" + rondes);
		}
		this.slots = slots;
		this.doel = doel;
		this.ritme = ritme;
		this.pos = startPos;
		this.vorigePos = startPos;
		this.rest = Math.floorMod(doel - startPos, slots) + slots * rondes;
		this.wacht = wachttijd(rest, ritme);
		this.geland = rest == 0;
	}

	/** Een rad met een willekeurige start en twee of drie rondes. */
	public static Rad willekeurig(int slots, int doel, RandomGenerator random) {
		return willekeurig(slots, doel, random, KROON);
	}

	public static Rad willekeurig(int slots, int doel, RandomGenerator random, Ritme ritme) {
		return new Rad(slots, doel, random.nextInt(slots), 2 + random.nextInt(2), ritme);
	}

	/** Ticks tot de volgende stap met het ritme van het Rad van ronde 5. */
	public static int wachttijd(int rest) {
		return wachttijd(rest, KROON);
	}

	/** Ticks tot de volgende stap: snel zolang het rad op snelheid is, oplopend aan het eind. */
	public static int wachttijd(int rest, Ritme ritme) {
		if (rest >= ritme.vertraagVanaf()) {
			return ritme.snelste();
		}
		double t = 1.0 - (double) Math.max(rest, 1) / ritme.vertraagVanaf();
		// Kwadratisch: eerst ongemerkt trager, de laatste paar stappen tergend langzaam.
		double eind = 1.0 - 1.0 / ritme.vertraagVanaf();
		double f = eind <= 0 ? 1.0 : (t / eind) * (t / eind);
		return (int) Math.round(ritme.snelste() + (ritme.traagste() - ritme.snelste()) * f);
	}

	public Gebeurtenis tick() {
		if (geland) {
			return Gebeurtenis.NIKS;
		}
		if (--wacht > 0) {
			return Gebeurtenis.NIKS;
		}
		vorigePos = pos;
		pos = (pos + 1) % slots;
		rest--;
		if (rest == 0) {
			geland = true;
			return Gebeurtenis.GELAND;
		}
		wacht = wachttijd(rest, ritme);
		return Gebeurtenis.STAP;
	}

	public int slots() {
		return slots;
	}

	public int pos() {
		return pos;
	}

	public int vorigePos() {
		return vorigePos;
	}

	public int rest() {
		return rest;
	}

	public int doel() {
		return doel;
	}

	public boolean geland() {
		return geland;
	}

	/** Ticks tot de volgende stap. */
	public int wacht() {
		return wacht;
	}
}
