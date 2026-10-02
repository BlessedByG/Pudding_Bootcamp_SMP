package nl.pudding.bootcamp.core;

import java.util.Locale;

/**
 * De blokken die de mod in het Ei strooit: wat ze waard zijn en hoeveel er standaard komen. Alleen
 * netherite, diamond, gold en iron geven punten; de rest doet iets.
 */
public enum EiBlok {
	NETHERITE("netherite", "minecraft:netherite_block", 50, 10),
	DIAMOND("diamond", "minecraft:diamond_block", 10, 150),
	GOLD("gold", "minecraft:gold_block", 5, 200),
	/** Een punt, maar heel veel: zo vind je steeds iets. Niet in de chat, anders loopt die over. */
	IRON("iron", "minecraft:iron_block", 1, 2000),
	/** Geen punten: 50/50 Haste voor de hakker of een bevriezing voor de rest. */
	REDSTONE("redstone", "minecraft:redstone_block", 0, 20),
	/** Geen punten: kies uit een kistmenu wie een jumpscare krijgt. */
	EMERALD("emerald", "minecraft:emerald_block", 0, 30),
	/** Geen punten: kies uit een kistmenu wie terug moet naar zijn startplek. */
	LAPIS("lapis", "minecraft:lapis_block", 0, 30),
	/** Geen punten: een TNT in je inventory die meteen afgaat als je hem neerzet en deepslate wegblaast. */
	TNT("tnt", "minecraft:tnt", 0, 20),
	/** Geen punten: even Efficiency V op je pickaxe en Haste II. */
	GLOWSTONE("glowstone", "minecraft:glowstone", 0, 20),
	/** Geen punten: Nausea voor alle anderen. */
	SLIME("slime", "minecraft:slime_block", 0, 30),
	/** Geen punten: iedereen wisselt willekeurig van plek. */
	TARGET("target", "minecraft:target", 0, 30);

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
