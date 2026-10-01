package nl.pudding.bootcamp.core;

/** De teksten van de bossbar: kort, altijd hetzelfde formaat (tabel in docs/04). */
public final class BossbarTekst {
	public static final String BASISKAMP = "Pudding Bootcamp";
	public static final String QUIZ_DRAAI = "Quiz · draai het rad";
	public static final String DOOLHOF_WACHT = "Doolhof · wacht op de start";
	public static final String CLOWN_WACHT = "King of the Hill · wacht op de start";
	public static final String FFA_WACHT = "FFA · wacht op de start";
	public static final String FINALE_WACHT = "De Finale · wacht op de start";

	private BossbarTekst() {
	}

	public static String doolhof(int seconden) {
		return "Doolhof · " + Tijd.mmss(seconden);
	}

	/** Na de timer, zolang niet iedereen een team heeft. */
	public static String doolhofGif() {
		return "Doolhof · de tijd is om · gif";
	}

	/** Met hoe sterk het gif nu is: {@code gif x4} na twee minuten. */
	public static String doolhofGif(int keer) {
		return keer <= 1 ? doolhofGif() : doolhofGif() + " x" + keer;
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
		return "King of the Hill · Kroon: " + kroon + " · " + over + " over";
	}

	/** De kroonhouder is uitgelogd: de mod telt af tot de kroon doorgaat. */
	public static String kroonWeg(String kroon, int wachtSeconden) {
		return "King of the Hill · " + kroon + " is weg · kroon door over " + Tijd.mmss(wachtSeconden);
	}

	public static String ffa(int over) {
		return "FFA · " + over + " over";
	}

	public static String finale(String een, String twee) {
		return "De Finale · " + een + " tegen " + twee;
	}

	/** Een finalist is uitgelogd: de finale staat stil tot de commander kiest. */
	public static String finalePauze(String weg) {
		return "De Finale · " + weg + " is weg · Pudding beslist";
	}

	public static String king(String naam) {
		return "Pudding Bootcamp · King: " + naam;
	}
}
