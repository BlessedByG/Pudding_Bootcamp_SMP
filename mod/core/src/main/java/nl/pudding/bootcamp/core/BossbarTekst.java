package nl.pudding.bootcamp.core;

/** De teksten van de bossbar: kort, altijd hetzelfde formaat (tabel in docs/04). */
public final class BossbarTekst {
	public static final String BASISKAMP = "Pudding Bootcamp";
	public static final String QUIZ_DRAAI = "Quiz · draai het rad";
	public static final String CLOWN_WACHT = "Clown vs All · wacht op de start";
	public static final String FFA_WACHT = "FFA · wacht op de start";

	private BossbarTekst() {
	}

	public static String doolhofPoortDicht(int secondenTotOpen) {
		return "Doolhof · uitgang open over " + Tijd.mmss(secondenTotOpen);
	}

	public static String doolhof(int seconden) {
		return "Doolhof · " + Tijd.mmss(seconden);
	}

	public static String ei(int seconden) {
		return "Het Ei · " + Tijd.mmss(seconden);
	}

	/** Wave 0: de beurt is er nog niet of net klaar. */
	public static String mobarena(int beurt, int beurten, int wave) {
		String t = "Mob Arena · beurt " + beurt + "/" + beurten;
		return wave > 0 ? t + " · wave " + wave : t;
	}

	public static String quiz(String team) {
		return team == null ? QUIZ_DRAAI : "Quiz · aan de beurt: " + team;
	}

	public static String clown(String kroon, int over) {
		return "Clown vs All · Kroon: " + kroon + " · " + over + " over";
	}

	/** De kroonhouder is uitgelogd: de mod telt af tot de kroon doorgaat. */
	public static String kroonWeg(String kroon, int wachtSeconden) {
		return "Clown vs All · " + kroon + " is weg · kroon door over " + Tijd.mmss(wachtSeconden);
	}

	public static String ffa(int over) {
		return "FFA · " + over + " over";
	}

	public static String king(String naam) {
		return "Pudding Bootcamp · King: " + naam;
	}
}
