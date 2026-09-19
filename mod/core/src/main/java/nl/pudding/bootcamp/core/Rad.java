package nl.pudding.bootcamp.core;

import java.util.random.RandomGenerator;

/**
 * Het Rad: een lampje dat de kring rondloopt, steeds langzamer, en altijd op het doelslot stopt.
 * De startpositie en het aantal rondes zijn echt willekeurig, de landing niet.
 *
 * <p>Tick-gestuurd: roep elke servertick {@link #tick()} aan.
 */
public final class Rad {
	public static final int SNELSTE_STAP = 2;
	public static final int TRAAGSTE_STAP = 30;
	/** Vanaf zoveel resterende stappen begint het rad te vertragen. */
	public static final int VERTRAAG_VANAF = 25;

	public enum Gebeurtenis {
		/** Deze tick gebeurt er niks. */
		NIKS,
		/** Het lampje is een pilaar opgeschoven: {@link #vorigePos()} uit, {@link #pos()} aan. */
		STAP,
		/** Laatste stap: het lampje staat op het doel en blijft daar. */
		GELAND
	}

	private final int slots;
	private final int doel;
	private int pos;
	private int vorigePos;
	private int rest;
	private int wacht;
	private boolean geland;

	/**
	 * @param doel     de pilaar van de uitverkorene
	 * @param startPos waar het lampje begint
	 * @param rondes   hoeveel volle rondes het eerst loopt (2 of 3)
	 */
	public Rad(int slots, int doel, int startPos, int rondes) {
		if (slots <= 0 || doel < 0 || doel >= slots || startPos < 0 || startPos >= slots || rondes < 0) {
			throw new IllegalArgumentException("ongeldig rad: slots=" + slots + " doel=" + doel
					+ " start=" + startPos + " rondes=" + rondes);
		}
		this.slots = slots;
		this.doel = doel;
		this.pos = startPos;
		this.vorigePos = startPos;
		this.rest = Math.floorMod(doel - startPos, slots) + slots * rondes;
		this.wacht = wachttijd(rest);
		this.geland = rest == 0;
	}

	/** Een rad met een willekeurige start en twee of drie rondes. */
	public static Rad willekeurig(int slots, int doel, RandomGenerator random) {
		return new Rad(slots, doel, random.nextInt(slots), 2 + random.nextInt(2));
	}

	/** Ticks tot de volgende stap: 2 zolang het rad op snelheid is, oplopend naar 30 aan het eind. */
	public static int wachttijd(int rest) {
		if (rest >= VERTRAAG_VANAF) {
			return SNELSTE_STAP;
		}
		double t = 1.0 - (double) Math.max(rest, 1) / VERTRAAG_VANAF;
		// Kwadratisch: eerst ongemerkt trager, de laatste paar stappen tergend langzaam.
		double eind = 1.0 - 1.0 / VERTRAAG_VANAF;
		double f = (t / eind) * (t / eind);
		return (int) Math.round(SNELSTE_STAP + (TRAAGSTE_STAP - SNELSTE_STAP) * f);
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
		wacht = wachttijd(rest);
		return Gebeurtenis.STAP;
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
}
