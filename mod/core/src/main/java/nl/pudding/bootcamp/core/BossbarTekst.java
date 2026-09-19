package nl.pudding.bootcamp.core;

/** De teksten van de bossbar: kort, altijd hetzelfde formaat (tabel in docs/04). */
public final class BossbarTekst {
	public static final String BASISKAMP = "Pudding Bootcamp";

	private BossbarTekst() {
	}

	public static String doolhof(int seconden) {
		return "Doolhof · " + Tijd.mmss(seconden);
	}

	public static String horde(int wave, int mobs) {
		return "Wave " + wave + " · " + mobs + " mobs";
	}

	public static String ei(int seconden) {
		return "Het Ei · " + Tijd.mmss(seconden);
	}

	public static String king(String koning, int seconden) {
		return "Koning: " + koning + " · " + Tijd.mmss(seconden);
	}

	/** De koning is uitgelogd: de mod telt af tot de kroon doorgaat. */
	public static String koningWeg(String koning, int wachtSeconden) {
		return "Koning " + koning + " is weg · kroon door over " + Tijd.mmss(wachtSeconden);
	}

	public static String rust(int seconden) {
		return "Finale over " + Tijd.mmss(seconden);
	}

	public static String ffa(int over, int seconden) {
		return "FFA · " + over + " over · " + Tijd.mmss(seconden);
	}

	public static String finale(int winstEen, int winstTwee) {
		return "Finale · " + winstEen + " - " + winstTwee;
	}
}
