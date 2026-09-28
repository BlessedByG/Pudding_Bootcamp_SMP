package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Het verloop van één beurt in de mob arena: twee arena's tegelijk, dezelfde waves. Een arena is
 * klaar met een wave als haar teller 0 is; zijn beide klaar, dan na 5 seconden de volgende wave.
 * Na 120 seconden telt een wave altijd als klaar. Een arena is klaar met de beurt na de laatste
 * wave, of zodra al haar spelers af zijn; de beurt is klaar als beide arena's klaar zijn.
 *
 * <p>Roep elke seconde {@link #seconde(int[], int[])} aan, na de countdown.
 */
public final class MobVerloop {
	public static final int PAUZE = 5;
	public static final int MAX_WAVE_SECONDEN = 120;

	public enum Soort {
		/** Spawn wave {@link Gebeurtenis#wave()} in elke arena die nog niet klaar is. */
		START_WAVE,
		/** De wave telt als klaar zonder dat alles dood is: haal de overgebleven mobs weg. */
		WAVE_GEFORCEERD,
		/** Deze arena is klaar met de beurt (al haar spelers af): haal haar mobs weg. */
		ARENA_KLAAR,
		/** Beide arena's klaar: de beurt is voorbij. */
		BEURT_KLAAR
	}

	/** @param arena 1 of 2, of 0 als het om beide gaat */
	public record Gebeurtenis(Soort soort, int arena, int wave) {
	}

	private final int aantalWaves;
	private final boolean[] klaar = new boolean[3];
	private int wave;
	private int secondenInWave;
	private int pauze = -1;
	private boolean geforceerd;
	private boolean beurtKlaar;

	public MobVerloop(int aantalWaves) {
		if (aantalWaves < 1) {
			throw new IllegalArgumentException("minstens één wave");
		}
		this.aantalWaves = aantalWaves;
	}

	/** {@code /mobarena wave volgende}: de huidige wave telt in beide arena's als klaar. */
	public void forceer() {
		if (wave > 0 && !beurtKlaar) {
			geforceerd = true;
		}
	}

	/**
	 * @param mobs    levende mobs per arena, index 1 en 2 (index 0 wordt niet gebruikt)
	 * @param spelers spelers die nog in de arena staan, index 1 en 2
	 */
	public List<Gebeurtenis> seconde(int[] mobs, int[] spelers) {
		List<Gebeurtenis> uit = new ArrayList<>();
		if (beurtKlaar) {
			return uit;
		}
		for (int a = 1; a <= 2; a++) {
			if (!klaar[a] && spelers[a] <= 0) {
				klaar[a] = true;
				uit.add(new Gebeurtenis(Soort.ARENA_KLAAR, a, wave));
			}
		}
		if (klaar[1] && klaar[2]) {
			return beurtKlaar(uit);
		}
		if (wave == 0) {
			return startWave(uit);
		}
		if (pauze >= 0) {
			if (--pauze < 0) {
				return startWave(uit);
			}
			return uit;
		}
		secondenInWave++;
		boolean waveKlaar = true;
		for (int a = 1; a <= 2; a++) {
			if (!klaar[a] && mobs[a] > 0) {
				waveKlaar = false;
			}
		}
		if (!waveKlaar && (geforceerd || secondenInWave >= MAX_WAVE_SECONDEN)) {
			uit.add(new Gebeurtenis(Soort.WAVE_GEFORCEERD, 0, wave));
			waveKlaar = true;
		}
		geforceerd = false;
		if (waveKlaar) {
			if (wave >= aantalWaves) {
				return beurtKlaar(uit);
			}
			pauze = PAUZE - 1;
			if (pauze < 0) {
				return startWave(uit);
			}
		}
		return uit;
	}

	private List<Gebeurtenis> startWave(List<Gebeurtenis> uit) {
		wave++;
		secondenInWave = 0;
		pauze = -1;
		geforceerd = false;
		uit.add(new Gebeurtenis(Soort.START_WAVE, 0, wave));
		return uit;
	}

	private List<Gebeurtenis> beurtKlaar(List<Gebeurtenis> uit) {
		beurtKlaar = true;
		klaar[1] = true;
		klaar[2] = true;
		uit.add(new Gebeurtenis(Soort.BEURT_KLAAR, 0, wave));
		return uit;
	}

	/** De lopende wave, vanaf 1; 0 als er nog geen gestart is. */
	public int wave() {
		return wave;
	}

	public int aantalWaves() {
		return aantalWaves;
	}

	public boolean arenaKlaar(int arena) {
		return klaar[arena];
	}

	public boolean beurtKlaar() {
		return beurtKlaar;
	}

	/** Loopt de pauze van 5 seconden tussen twee waves? */
	public boolean inPauze() {
		return pauze >= 0;
	}
}
