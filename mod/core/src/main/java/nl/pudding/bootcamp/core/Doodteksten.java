package nl.pudding.bootcamp.core;

import java.util.List;
import java.util.random.RandomGenerator;

/** De tekst die een dode groot in beeld krijgt. Alleen voor de dode zelf. */
public final class Doodteksten {
	public static final List<String> STANDAARD = List.of(
			"Grote L gepakt!",
			"Kleine L gepakt",
			"Had je nou maar beter je best gedaan",
			"Gelukkig is dit de CSMP niet..",
			"Dag 1...",
			"Op de lijst..");

	/** De standaardlijst tot 30 september 2026. Staat die nog zo in bootcamp.json, dan wordt het de nieuwe. */
	static final List<String> OUDE_STANDAARD = List.of(
			"Grote L gepakt!",
			"Had je nou maar beter je best gedaan",
			"Gelukkig is dit niet de CSMP");

	/** Zoveel tekens past er zeker in een grote title, ook met een grotere GUI-schaal. */
	public static final int MAX_TITEL = 22;

	private Doodteksten() {
	}

	/**
	 * R0.14: een doodtekst als title en subtitle. Tot {@link #MAX_TITEL} tekens alles groot; langer
	 * wordt gesplitst op de spatie die het dichtst bij het midden ligt, met het begin groot (hooguit
	 * {@link #MAX_TITEL} tekens) en de rest kleiner eronder. Zonder zo'n spatie alles klein.
	 *
	 * @return title en subtitle; de subtitle is {@code null} als alles groot past
	 */
	public static String[] verdeel(String tekst) {
		if (tekst.length() <= MAX_TITEL) {
			return new String[] {tekst, null};
		}
		int midden = tekst.length() / 2;
		int beste = -1;
		for (int i = 1; i <= MAX_TITEL && i < tekst.length() - 1; i++) {
			if (tekst.charAt(i) == ' ' && (beste < 0 || Math.abs(i - midden) < Math.abs(beste - midden))) {
				beste = i;
			}
		}
		if (beste < 0) {
			return new String[] {"", tekst};
		}
		return new String[] {tekst.substring(0, beste).strip(), tekst.substring(beste + 1).strip()};
	}

	/** Kiest willekeurig uit de lijst; is die leeg, dan uit de standaardlijst. */
	public static String kies(List<String> teksten, RandomGenerator random) {
		List<String> bron = teksten == null || teksten.isEmpty() ? STANDAARD : teksten;
		return bron.get(random.nextInt(bron.size()));
	}
}
