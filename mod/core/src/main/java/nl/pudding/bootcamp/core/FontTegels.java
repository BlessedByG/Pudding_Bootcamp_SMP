package nl.pudding.bootcamp.core;

/**
 * Een groot plaatje in een font, als tegels. Minecraft zet font-glyphs op vellen van 256 x 256
 * pixels: een glyph die groter is, wordt stilletjes een leeg vierkantje. Daarom knipt BouwPack de
 * jumpscare-foto en het quiz-rad in tegels van hooguit 242 pixels, en zet de mod ze met deze tekst
 * weer aan elkaar.
 *
 * <p>Twee rijen, want een glyph mag niet hoger boven de basislijn staan dan hij zelf hoog is
 * ({@code ascent <= height}): de bovenste rij hangt boven de basislijn, de onderste eronder. Na elke
 * tegel een spatie van -1 (een bitmap-glyph schuift één eenheid meer op dan hij breed is), na de
 * bovenste rij een spatie terug over de hele breedte. Beide spaties staan in hetzelfde font met de
 * breedte die bij dat plaatje hoort. Tegels die een smallere foto niet nodig heeft, zijn in het
 * font spaties van +1, dus samen met hun -1 niks.
 *
 * <p>BouwPack ({@code pack/BouwPack.java}) volgt dezelfde nummering.
 */
public final class FontTegels {
	public static final int RIJEN = 2;
	/** Spatie van -1 na elke tegel. */
	public static final char TERUG_EEN = '';
	/** Spatie terug over de breedte van het plaatje, na de bovenste rij. */
	public static final char TERUG_RIJ = '';

	private FontTegels() {
	}

	/**
	 * De tekst voor een plaatje van twee rijen: tegel (rij r, kolom c) heeft codepoint
	 * {@code eerste + r * rijStap + c}.
	 */
	public static String tekst(int eerste, int rijStap, int kolommen) {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < RIJEN; r++) {
			for (int c = 0; c < kolommen; c++) {
				sb.appendCodePoint(eerste + r * rijStap + c);
				sb.append(TERUG_EEN);
			}
			if (r < RIJEN - 1) {
				sb.append(TERUG_RIJ);
			}
		}
		return sb.toString();
	}
}
