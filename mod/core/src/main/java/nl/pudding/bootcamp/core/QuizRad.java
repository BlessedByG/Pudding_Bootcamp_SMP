package nl.pudding.bootcamp.core;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Het ronde quiz-rad uit het resource pack: 16 vakken in de teamkleuren, elk team 4 keer, buren
 * (ook rondom) nooit gelijk. Het pack heeft 64 plaatjes; plaatje s is het rad s × 5,625° met de
 * klok mee gedraaid. Op plaatje 0 staat vak 0 onder het pijltje, en elk vierde plaatje heeft een
 * vak precies onder het pijltje. Een vakgrens gaat onder het pijltje door op de standen 2, 6, 10...
 *
 * <p>BouwPack tekent de plaatjes met dezelfde {@link #VAKKEN} en dezelfde draairichting.
 */
public final class QuizRad {
	public static final int STANDEN = 64;
	public static final int PER_VAK = 4;
	/**
	 * De eerste glyph van font {@code bootcamp:rad}. Elke stand is 2 x 2 tegels (zie {@link FontTegels}):
	 * stand s heeft de codepoints U+E100 + 4s t/m U+E100 + 4s + 3.
	 */
	public static final int EERSTE_GLYPH = 0xE100;
	public static final int TEGELS_PER_STAND = 4;

	public static final List<Kleur> VAKKEN = List.of(
			Kleur.ROOD, Kleur.BLAUW, Kleur.GROEN, Kleur.GEEL,
			Kleur.BLAUW, Kleur.ROOD, Kleur.GEEL, Kleur.GROEN,
			Kleur.ROOD, Kleur.GROEN, Kleur.BLAUW, Kleur.GEEL,
			Kleur.GROEN, Kleur.GEEL, Kleur.ROOD, Kleur.BLAUW);

	private QuizRad() {
	}

	/** Welk vak staat er onder het pijltje bij deze stand (afgerond naar het dichtstbijzijnde)? */
	public static int vakBijStand(int stand) {
		int s = Math.floorMod(stand, STANDEN);
		int vakstappen = (s + PER_VAK / 2) / PER_VAK;
		// Met de klok mee draaien: de vakken komen in dalende volgorde langs het pijltje.
		return Math.floorMod(-vakstappen, VAKKEN.size());
	}

	/** De stand waarbij dit vak precies onder het pijltje staat. */
	public static int standVanVak(int vak) {
		return Math.floorMod(-vak, VAKKEN.size()) * PER_VAK;
	}

	public static Kleur kleurBijStand(int stand) {
		return VAKKEN.get(vakBijStand(stand));
	}

	/** Gaat er bij deze stand een vakgrens onder het pijltje door? Dan klinkt de tik. */
	public static boolean isVakgrens(int stand) {
		return Math.floorMod(stand, PER_VAK) == PER_VAK / 2;
	}

	/** De tekst die deze stand in font {@code bootcamp:rad} tekent: 2 x 2 tegels. */
	public static String glyph(int stand) {
		return FontTegels.tekst(EERSTE_GLYPH + Math.floorMod(stand, STANDEN) * TEGELS_PER_STAND, 2, 2);
	}

	/** Een draai: een willekeurig vak (elk team 25%), landend op het midden van dat vak. */
	public static Rad draai(RandomGenerator random) {
		int vak = random.nextInt(VAKKEN.size());
		return Rad.willekeurig(STANDEN, standVanVak(vak), random, Rad.QUIZ);
	}
}
