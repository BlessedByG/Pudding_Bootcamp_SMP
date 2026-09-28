package nl.pudding.bootcamp.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Strooien in het Ei: uit de deepslate-plekken van de vastlegging trekt de mod willekeurig,
 * zonder dubbele, zoveel plekken als {@code /ei blokken} zegt.
 */
public final class EiVerdeling {
	private EiVerdeling() {
	}

	/**
	 * @param plekken  hoeveel deepslate-plekken er zijn (index 0 t/m plekken - 1)
	 * @param aantallen hoeveel van elke soort
	 * @return per soort de gekozen indexen; samen nooit een index dubbel
	 * @throws IllegalArgumentException als er samen meer gevraagd wordt dan er plekken zijn
	 */
	public static Map<EiBlok, int[]> trek(int plekken, Map<EiBlok, Integer> aantallen, RandomGenerator random) {
		int totaal = 0;
		for (EiBlok b : EiBlok.values()) {
			int n = aantallen.getOrDefault(b, 0);
			if (n < 0) {
				throw new IllegalArgumentException(b.id() + ": een aantal kan niet negatief zijn");
			}
			totaal += n;
		}
		if (totaal > plekken) {
			throw new IllegalArgumentException("samen " + totaal + " puntenblokken, maar het Ei heeft maar "
					+ plekken + " deepslate-plekken");
		}
		// Gedeeltelijke Fisher-Yates: de eerste 'totaal' plekken van een geschudde rij.
		int[] rij = new int[plekken];
		for (int i = 0; i < plekken; i++) {
			rij[i] = i;
		}
		for (int i = 0; i < totaal; i++) {
			int j = i + random.nextInt(plekken - i);
			int t = rij[i];
			rij[i] = rij[j];
			rij[j] = t;
		}
		Map<EiBlok, int[]> uit = new EnumMap<>(EiBlok.class);
		int van = 0;
		for (EiBlok b : EiBlok.values()) {
			int n = aantallen.getOrDefault(b, 0);
			int[] deel = new int[n];
			System.arraycopy(rij, van, deel, 0, n);
			uit.put(b, deel);
			van += n;
		}
		return uit;
	}
}
