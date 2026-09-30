package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Het verloop van één beurt in de mob arena: één veld, de waves na elkaar. Een wave is klaar als
 * alle mobs dood zijn; dan na 5 seconden de volgende. Na 120 seconden telt een wave altijd als
 * klaar, behalve een wave zonder limiet (de warden). De beurt is klaar na de laatste wave, of
 * zodra alle spelers in het veld af zijn.
 *
 * <p>Roep elke seconde {@link #seconde(int, int)} aan, na de countdown.
 */
public final class MobVerloop {
	public static final int PAUZE = 5;
	public static final int MAX_WAVE_SECONDEN = 120;

	public enum Soort {
		/** Spawn wave {@link Gebeurtenis#wave()}. */
		START_WAVE,
		/** De wave telt als klaar zonder dat alles dood is: haal de overgebleven mobs weg. */
		WAVE_GEFORCEERD,
		/** De beurt is voorbij: de laatste wave is klaar, of niemand staat nog in het veld. */
		BEURT_KLAAR
	}

	public record Gebeurtenis(Soort soort, int wave) {
	}

	private final int aantalWaves;
	private int wave;
	private int secondenInWave;
	private int pauze = -1;
	private boolean geforceerd;
	private boolean zonderLimiet;
	private boolean beurtKlaar;

	public MobVerloop(int aantalWaves) {
		if (aantalWaves < 1) {
			throw new IllegalArgumentException("minstens één wave");
		}
		this.aantalWaves = aantalWaves;
	}

	/** {@code /mobarena wave volgende}: de huidige wave telt als klaar, ook een wave zonder limiet. */
	public void forceer() {
		if (wave > 0 && !beurtKlaar) {
			geforceerd = true;
		}
	}

	/** De lopende wave is de warden: geen 120 seconden, hij loopt tot alles dood is (of {@link #forceer()}). */
	public void zonderLimiet() {
		zonderLimiet = true;
	}

	/**
	 * @param mobs    levende mobs in het veld
	 * @param spelers spelers die nog in het veld staan
	 */
	public List<Gebeurtenis> seconde(int mobs, int spelers) {
		List<Gebeurtenis> uit = new ArrayList<>();
		if (beurtKlaar) {
			return uit;
		}
		if (spelers <= 0) {
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
		boolean waveKlaar = mobs <= 0;
		if (!waveKlaar && (geforceerd || (!zonderLimiet && secondenInWave >= MAX_WAVE_SECONDEN))) {
			uit.add(new Gebeurtenis(Soort.WAVE_GEFORCEERD, wave));
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
		zonderLimiet = false;
		uit.add(new Gebeurtenis(Soort.START_WAVE, wave));
		return uit;
	}

	private List<Gebeurtenis> beurtKlaar(List<Gebeurtenis> uit) {
		beurtKlaar = true;
		uit.add(new Gebeurtenis(Soort.BEURT_KLAAR, wave));
		return uit;
	}

	/** De lopende wave, vanaf 1; 0 als er nog geen gestart is. */
	public int wave() {
		return wave;
	}

	public int aantalWaves() {
		return aantalWaves;
	}

	public boolean beurtKlaar() {
		return beurtKlaar;
	}

	/** Loopt de pauze van 5 seconden tussen twee waves? */
	public boolean inPauze() {
		return pauze >= 0;
	}

	/** Is de lopende wave de laatste? Dan komt er in deze beurt geen volgende meer. */
	public boolean laatsteWave() {
		return wave >= aantalWaves;
	}
}
