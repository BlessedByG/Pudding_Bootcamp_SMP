package nl.pudding.bootcamp.core;

import java.util.Locale;

/**
 * De vier teamkleuren van ronde 1 t/m 4, gekozen bij de uitgang van het doolhof. De rgb-waarde is
 * die van het quiz-rad in het resource pack.
 */
public enum Kleur {
	ROOD("rood", "Rood", 0xE24B4A),
	BLAUW("blauw", "Blauw", 0x378ADD),
	GROEN("groen", "Groen", 0x639922),
	GEEL("geel", "Geel", 0xEF9F27);

	private final String id;
	private final String naam;
	private final int rgb;

	Kleur(String id, String naam, int rgb) {
		this.id = id;
		this.naam = naam;
		this.rgb = rgb;
	}

	/** Zoals in commands, punten en {@code bootcamp.json}: {@code rood}. */
	public String id() {
		return id;
	}

	/** Zoals in beeld: {@code Rood}. */
	public String naam() {
		return naam;
	}

	public int rgb() {
		return rgb;
	}

	/** De kleur bij een id, hoofdletters maken niet uit; {@code null} als het geen teamkleur is. */
	public static Kleur vanId(String id) {
		if (id == null) {
			return null;
		}
		String s = id.toLowerCase(Locale.ROOT).strip();
		for (Kleur k : values()) {
			if (k.id.equals(s)) {
				return k;
			}
		}
		return null;
	}
}
