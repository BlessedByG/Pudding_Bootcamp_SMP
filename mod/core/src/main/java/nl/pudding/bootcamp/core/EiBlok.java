package nl.pudding.bootcamp.core;

import java.util.Locale;

/** De puntenblokken van het Ei: wat ze waard zijn en hoeveel de mod er standaard strooit. */
public enum EiBlok {
	NETHERITE("netherite", "minecraft:netherite_block", 50, 6),
	DIAMOND("diamond", "minecraft:diamond_block", 10, 90),
	GOLD("gold", "minecraft:gold_block", 5, 120),
	/** Geen punten: 50/50 Haste voor de hakker of een bevriezing voor de rest. */
	REDSTONE("redstone", "minecraft:redstone_block", 0, 10),
	/** Geen punten: een jumpscare bij een ander. */
	EMERALD("emerald", "minecraft:emerald_block", 0, 10);

	/** Het blok waar de puntenblokken op komen: gewone deepslate, geen varianten. */
	public static final String DEEPSLATE = "minecraft:deepslate";

	private final String id;
	private final String blok;
	private final int punten;
	private final int standaard;

	EiBlok(String id, String blok, int punten, int standaard) {
		this.id = id;
		this.blok = blok;
		this.punten = punten;
		this.standaard = standaard;
	}

	/** Zoals in {@code /ei blokken}: {@code netherite}. */
	public String id() {
		return id;
	}

	/** Het Minecraft-blok. */
	public String blok() {
		return blok;
	}

	public int punten() {
		return punten;
	}

	public int standaard() {
		return standaard;
	}

	public static EiBlok vanId(String id) {
		if (id == null) {
			return null;
		}
		String s = id.toLowerCase(Locale.ROOT).strip();
		for (EiBlok b : values()) {
			if (b.id.equals(s)) {
				return b;
			}
		}
		return null;
	}

	/** Het puntenblok bij een Minecraft-blok-id, of {@code null}. */
	public static EiBlok vanBlok(String blokId) {
		for (EiBlok b : values()) {
			if (b.blok.equals(blokId)) {
				return b;
			}
		}
		return null;
	}
}
