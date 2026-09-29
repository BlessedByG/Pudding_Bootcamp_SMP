package nl.pudding.bootcamp.core;

import java.util.random.RandomGenerator;

/** R2.13: een target in het Ei zet iedereen op de plek van een ander. */
public final class Hussel {
	private Hussel() {
	}

	/**
	 * Een willekeurige verdeling waarin niemand zijn eigen plek houdt: speler {@code i} gaat naar de
	 * plek van speler {@code uit[i]}. Met één speler blijft die staan.
	 */
	public static int[] verdeel(int aantal, RandomGenerator random) {
		int[] uit = new int[aantal];
		if (aantal < 2) {
			return uit;
		}
		// Schudden tot niemand op zijn eigen plek valt: gemiddeld zo'n drie keer.
		do {
			for (int i = 0; i < aantal; i++) {
				uit[i] = i;
			}
			for (int i = aantal - 1; i > 0; i--) {
				int j = random.nextInt(i + 1);
				int t = uit[i];
				uit[i] = uit[j];
				uit[j] = t;
			}
		} while (heeftVastePlek(uit));
		return uit;
	}

	private static boolean heeftVastePlek(int[] verdeling) {
		for (int i = 0; i < verdeling.length; i++) {
			if (verdeling[i] == i) {
				return true;
			}
		}
		return false;
	}
}
