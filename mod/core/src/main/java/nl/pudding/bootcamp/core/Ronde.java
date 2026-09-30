package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * De rondes van de avond, met hun commando en wat elke ronde aan regio's en punten nodig heeft.
 * Van een genummerde reeks ({@code ei_spawn_1} t/m {@code ei_spawn_n}) moet er minstens één zijn:
 * daarom staat alleen {@code _1} in de lijst. Hoe lang een ronde duurt staat in de
 * {@link Instellingen}, niet hier.
 */
public enum Ronde {
	BASISKAMP(0, "Basiskamp", null, false, List.of(), List.of()),
	DOOLHOF(1, "De Doolhof", "doolhof", false,
			List.of("doolhof", "doolhof_uit", "poort_doolhof", "poort_start", "doolhof_gif"),
			List.of("doolhof_start", "doolhof_finish", "v2")),
	EI(2, "Het Ei", "ei", true,
			List.of("ei", "eigebied"),
			List.of("ei_spawn_1", "v3")),
	MOBARENA(3, "De Mob Arena", "mobarena", false,
			List.of("mobarena", "veld_1", "veld_2", "tribune_mob"),
			List.of("kooi_1", "kooi_2", "tribune_mob_1", "mob_1_1", "mob_2_1",
					"start_1_rood", "start_1_blauw", "start_1_groen", "start_1_geel",
					"start_2_rood", "start_2_blauw", "start_2_groen", "start_2_geel")),
	QUIZ(4, "De Quiz", "quiz", false,
			List.of("quiz"),
			List.of("quiz_rood", "quiz_blauw", "quiz_groen", "quiz_geel", "quiz_podium",
					"quizlamp_rood", "quizlamp_blauw", "quizlamp_groen", "quizlamp_geel", "tribune_1")),
	CLOWN(5, "Clown vs All", "clown", false,
			List.of("vloer"),
			List.of("troon", "jager_1", "tribune_1")),
	FFA(6, "De FFA", "ffa", false,
			List.of("vloer"),
			List.of("troon", "jager_1", "tribune_1"));

	private final int nummer;
	private final String naam;
	private final String commando;
	private final boolean survival;
	private final List<String> vereisteRegios;
	private final List<String> vereistePunten;

	Ronde(int nummer, String naam, String commando, boolean survival,
			List<String> vereisteRegios, List<String> vereistePunten) {
		this.nummer = nummer;
		this.naam = naam;
		this.commando = commando;
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

	/** Het commando van deze ronde zonder slash ({@code mobarena}), {@code null} voor het basiskamp. */
	public String commando() {
		return commando;
	}

	/** Alleen in het Ei moet je minen; overal elders is het adventure. */
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
		return this == CLOWN || this == FFA;
	}

	/** Spelen de vier teamkleuren mee (ronde 1 t/m 4)? */
	public boolean metTeams() {
		return nummer >= 1 && nummer <= 4;
	}

	public static Ronde vanNummer(int nummer) {
		for (Ronde r : values()) {
			if (r.nummer == nummer) {
				return r;
			}
		}
		return null;
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

	/**
	 * De punten van een genummerde reeks die er zijn, op volgorde: {@code prefix + 1},
	 * {@code prefix + 2}, ... tot het eerste gat.
	 */
	public static List<String> reeks(String prefix, Set<String> punten) {
		List<String> uit = new ArrayList<>();
		for (int i = 1; punten.contains(prefix + i); i++) {
			uit.add(prefix + i);
		}
		return uit;
	}

	/** Het eerste nummer in een reeks dat nog vrij is ({@code /clown jagerplek} zonder nummer). */
	public static int volgendVrij(String prefix, Set<String> punten) {
		int i = 1;
		while (punten.contains(prefix + i)) {
			i++;
		}
		return i;
	}
}
