package nl.pudding.bootcamp.core;

import java.util.List;
import java.util.random.RandomGenerator;

/** De tekst die een dode groot in beeld krijgt. Alleen voor de dode zelf. */
public final class Doodteksten {
	public static final List<String> STANDAARD = List.of(
			"Grote L gepakt!",
			"Had je nou maar beter je best gedaan",
			"Gelukkig is dit niet de CSMP");

	private Doodteksten() {
	}

	/** Kiest willekeurig uit de lijst; is die leeg, dan uit de standaardlijst. */
	public static String kies(List<String> teksten, RandomGenerator random) {
		List<String> bron = teksten == null || teksten.isEmpty() ? STANDAARD : teksten;
		return bron.get(random.nextInt(bron.size()));
	}
}
