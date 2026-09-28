package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** De teamkeuze bij de uitgang van het doolhof: het maximum per team en wie waar heen gaat. */
public final class TeamKeuze {
	public static final int MINIMUM_MAX = 5;

	private TeamKeuze() {
	}

	/**
	 * Het maximum per team: 5, of meer als er meer dan 20 spelers zijn. De mod rekent met het
	 * aantal deelnemers op het moment dat de eerste kiest.
	 */
	public static int maximum(int spelers) {
		return Math.max(MINIMUM_MAX, (Math.max(0, spelers) + 3) / 4);
	}

	public static boolean vol(int aantal, int maximum) {
		return aantal >= maximum;
	}

	/**
	 * Het team met de minste spelers, voor wie bij het einde van het doolhof nog geen team heeft.
	 * Bij gelijk aantal het lot. Het maximum telt hier niet: de timer lost het op.
	 */
	public static Kleur kleinste(Map<Kleur, Integer> aantallen, RandomGenerator random) {
		int minst = Integer.MAX_VALUE;
		List<Kleur> gelijk = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			int n = aantallen.getOrDefault(k, 0);
			if (n < minst) {
				minst = n;
				gelijk.clear();
				gelijk.add(k);
			} else if (n == minst) {
				gelijk.add(k);
			}
		}
		return gelijk.get(random.nextInt(gelijk.size()));
	}
}
