package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** De rondes van de avond, met wat elke ronde aan regio's en punten nodig heeft. */
public enum Ronde {
	BASISKAMP(0, "Basiskamp", 0, false, List.of(), List.of()),
	DOOLHOF(1, "Doolhof", 10 * 60, false,
			List.of("doolhof", "doolhof_uit"),
			List.of("doolhof_start", "v2")),
	HORDE(2, "De Horde", 10 * 60, false,
			List.of("arena"),
			List.of("arena_spawn", "mob_1", "mob_2", "mob_3", "mob_4",
					"tribune_horde_1", "tribune_horde_2", "v3")),
	EI(3, "Het Ei", 10 * 60, true,
			List.of("eibos", "eiplaat"),
			List.of("ei_start", "ei_beacon", "kring")),
	KING(4, "King of the SMP", 15 * 60, false,
			List.of("vloer"),
			List.of("troon", "hunter_1", "hunter_2", "hunter_3", "hunter_4",
					"tribune_1", "tribune_2", "tribune_3", "tribune_4")),
	FFA(5, "Arena FFA", 10 * 60, false,
			List.of("vloer"),
			List.of("hunter_1", "hunter_2", "hunter_3", "hunter_4",
					"tribune_1", "tribune_2", "tribune_3", "tribune_4")),
	FINALE(6, "De Finale", 0, false,
			List.of("finale"),
			List.of("finale_1", "finale_2", "kroning",
					"tribune_1", "tribune_2", "tribune_3", "tribune_4"));

	public static final int AANTAL_LAMPEN = 20;

	private final int nummer;
	private final String naam;
	private final int duurSeconden;
	private final boolean survival;
	private final List<String> vereisteRegios;
	private final List<String> vereistePunten;

	Ronde(int nummer, String naam, int duurSeconden, boolean survival,
			List<String> vereisteRegios, List<String> vereistePunten) {
		this.nummer = nummer;
		this.naam = naam;
		this.duurSeconden = duurSeconden;
		this.survival = survival;
		this.vereisteRegios = vereisteRegios;
		this.vereistePunten = vereistePunten;
	}

	public int nummer() {
		return nummer;
	}

	public String naam() {
		return naam;
	}

	/** Duur van de timer in seconden; 0 als de ronde geen timer heeft. */
	public int duurSeconden() {
		return duurSeconden;
	}

	/** Alleen in het Ei-bos moet je minen; overal elders is het adventure. */
	public boolean survival() {
		return survival;
	}

	public List<String> vereisteRegios() {
		return vereisteRegios;
	}

	public List<String> vereistePunten() {
		return vereistePunten;
	}

	/** Speelt deze ronde in de Arena (tribune = {@code tribune_n}, terugkomers worden kijker)? */
	public boolean inArena() {
		return this == KING || this == FFA || this == FINALE;
	}

	public static Ronde vanNummer(int nummer) {
		for (Ronde r : values()) {
			if (r.nummer == nummer) {
				return r;
			}
		}
		return null;
	}

	/** Wat Het Rad nodig heeft: de twintig lampen van De Kring. */
	public static List<String> vereistePuntenRad() {
		List<String> uit = new ArrayList<>();
		for (int i = 0; i < AANTAL_LAMPEN; i++) {
			uit.add("lamp_" + i);
		}
		return uit;
	}

	/** Alles wat ontbreekt voor deze ronde, regio's eerst. Leeg betekent: starten mag. */
	public List<String> ontbreekt(Set<String> regios, Set<String> punten) {
		return ontbreekt(vereisteRegios, vereistePunten, regios, punten);
	}

	public static List<String> ontbreekt(List<String> nodigRegios, List<String> nodigPunten,
			Set<String> regios, Set<String> punten) {
		List<String> uit = new ArrayList<>();
		for (String r : nodigRegios) {
			if (!regios.contains(r)) {
				uit.add(r);
			}
		}
		for (String p : nodigPunten) {
			if (!punten.contains(p)) {
				uit.add(p);
			}
		}
		return uit;
	}
}
