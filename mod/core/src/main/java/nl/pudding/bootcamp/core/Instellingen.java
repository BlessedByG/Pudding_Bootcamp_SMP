package nl.pudding.bootcamp.core;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * De instellingen per ronde uit {@code bootcamp.json}: timers, de momenten in het doolhof, de
 * aantallen van het Ei, de punten van de mob arena en de teksten. Ze overleven een herstart en
 * {@code /bc reset}. Tijden in hele minuten, gerekend vanaf de start van de ronde.
 *
 * <p>Elke {@code check}-methode geeft {@code null} als de waarde mag, en anders de ene regel
 * waarmee het commando weigert.
 */
public final class Instellingen {
	public static final int DOOLHOF_TIMER = 15;
	public static final int DOOLHOF_POORT = 4;
	public static final int DOOLHOF_HINT = 10;
	/** Zoveel husks en silverfish komen er uit een valkist in het doolhof: willekeurig, min t/m max. */
	public static final int VAL_MOBS_MIN = 3;
	public static final int VAL_MOBS_MAX = 10;
	public static final int MAX_VAL_MOBS = 20;
	public static final int EI_TIMER = 15;
	public static final int MIN_TIMER = 5;
	public static final int MAX_TIMER = 60;
	public static final int MAX_TEKST = 60;
	public static final int MAX_MOB_PUNTEN = 100;
	public static final String AFTEKST = "Af · je speelt geen beurt meer";
	/** Tot hoeveel blokken boven de selectie een veld van de mob arena voor kijkers en wachtenden telt. */
	public static final int VELD_HOOGTE = 3;
	public static final int MAX_VELD_HOOGTE = 10;
	/** De laagste en hoogste valgrens van het Ei: de bouwhoogte van de overworld. */
	public static final int MIN_Y = -64;
	public static final int MAX_Y = 319;
	public static final String WACHTTEKST = "Wacht op het startsein";
	/** Punten voor een mobtype dat niet in de tabel staat. */
	public static final int MOB_PUNTEN_ANDER = 1;

	public static final Map<String, Integer> MOB_PUNTEN = standaardMobPunten();

	private static Map<String, Integer> standaardMobPunten() {
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put("zombie", 1);
		m.put("skeleton", 2);
		m.put("spider", 2);
		m.put("cave_spider", 2);
		m.put("creeper", 3);
		m.put("witch", 4);
		m.put("vindicator", 5);
		m.put("evoker", 8);
		m.put("ravager", 10);
		// Alleen uit het Warden-ei van de winnaar van het Ei.
		m.put("warden", 50);
		return java.util.Collections.unmodifiableMap(m);
	}

	private int doolhofTimer = DOOLHOF_TIMER;
	private int doolhofPoort = DOOLHOF_POORT;
	private int doolhofHint = DOOLHOF_HINT;
	private String hinttekst;
	private boolean poortMelding = true;
	private int valMobsMin = VAL_MOBS_MIN;
	private int valMobsMax = VAL_MOBS_MAX;
	private final Map<String, Integer> schrikFotos = new TreeMap<>();
	private String doolhofWachttekst = WACHTTEKST;
	private int eiTimer = EI_TIMER;
	private final EnumMap<EiBlok, Integer> eiBlokken = new EnumMap<>(EiBlok.class);
	private Integer eiValgrens;
	private final Map<String, Integer> mobPunten = new LinkedHashMap<>(MOB_PUNTEN);
	private String aftekst = AFTEKST;
	private int veldHoogte = VELD_HOOGTE;
	private final EnumMap<WardenWaarde, Integer> warden = new EnumMap<>(WardenWaarde.class);
	private String clownWachttekst = WACHTTEKST;
	private String ffaWachttekst = WACHTTEKST;
	private String finaleWachttekst = WACHTTEKST;

	public Instellingen() {
		for (EiBlok b : EiBlok.values()) {
			eiBlokken.put(b, b.standaard());
		}
		for (WardenWaarde w : WardenWaarde.values()) {
			warden.put(w, w.standaard());
		}
	}

	// Doolhof

	public int doolhofTimer() {
		return doolhofTimer;
	}

	public int doolhofPoort() {
		return doolhofPoort;
	}

	public int doolhofHint() {
		return doolhofHint;
	}

	/** De eigen hinttekst, of {@code null}: dan rekent de mod een windrichting uit. */
	public String hinttekst() {
		return hinttekst;
	}

	public String checkDoolhofTimer(int minuten) {
		String grens = checkTimer(minuten);
		if (grens != null) {
			return grens;
		}
		if (doolhofPoort >= minuten) {
			return "de poort (" + doolhofPoort + " min) gaat dan pas open na het einde (" + minuten + " min)";
		}
		if (doolhofHint >= minuten) {
			return "de hint (" + doolhofHint + " min) valt dan na het einde (" + minuten + " min)";
		}
		return null;
	}

	public String checkDoolhofPoort(int minuten) {
		if (minuten < 0) {
			return "de poort kan niet voor de start open (" + minuten + " min)";
		}
		if (minuten >= doolhofTimer) {
			return "de poort (" + minuten + " min) gaat pas open na het einde (" + doolhofTimer + " min)";
		}
		return null;
	}

	public String checkDoolhofHint(int minuten) {
		if (minuten < 1) {
			return "de hint komt op zijn vroegst na 1 minuut";
		}
		if (minuten >= doolhofTimer) {
			return "de hint (" + minuten + " min) valt na het einde (" + doolhofTimer + " min)";
		}
		return null;
	}

	/**
	 * Zet timer, poort en hint in één keer, zoals ze uit {@code bootcamp.json} komen: los van de
	 * volgorde. Klopt het geheel niet, dan weigert hij en blijft alles zoals het was.
	 */
	public void zetDoolhof(int timer, int poort, int hint) {
		vereis(checkTimer(timer));
		if (poort < 0 || poort >= timer) {
			throw new IllegalArgumentException("de poort (" + poort + " min) moet voor het einde (" + timer + " min) vallen");
		}
		if (hint < 1 || hint >= timer) {
			throw new IllegalArgumentException("de hint (" + hint + " min) moet voor het einde (" + timer + " min) vallen");
		}
		doolhofTimer = timer;
		doolhofPoort = poort;
		doolhofHint = hint;
	}

	public void zetDoolhofTimer(int minuten) {
		vereis(checkDoolhofTimer(minuten));
		doolhofTimer = minuten;
	}

	public void zetDoolhofPoort(int minuten) {
		vereis(checkDoolhofPoort(minuten));
		doolhofPoort = minuten;
	}

	public void zetDoolhofHint(int minuten) {
		vereis(checkDoolhofHint(minuten));
		doolhofHint = minuten;
	}

	/** Hoorn en title als de uitgang van het doolhof opengaat. */
	public boolean poortMelding() {
		return poortMelding;
	}

	public void zetPoortMelding(boolean aan) {
		poortMelding = aan;
	}

	public int valMobsMin() {
		return valMobsMin;
	}

	public int valMobsMax() {
		return valMobsMax;
	}

	public static String checkValMobs(int min, int max) {
		if (min < 0 || max > MAX_VAL_MOBS) {
			return "een valkist heeft 0 t/m " + MAX_VAL_MOBS + " mobs, niet " + min + " t/m " + max;
		}
		if (min > max) {
			return "het minimum (" + min + ") is groter dan het maximum (" + max + ")";
		}
		return null;
	}

	public void zetValMobs(int min, int max) {
		vereis(checkValMobs(min, max));
		valMobsMin = min;
		valMobsMax = max;
	}

	/** R1.6: de foto van een schrikplek ({@code schrik_3}), of 0 voor willekeurig. */
	public int schrikFoto(String plek) {
		return schrikFotos.getOrDefault(plek, 0);
	}

	/** De schrikplekken met een vaste foto; wat er niet in staat, is willekeurig. */
	public Map<String, Integer> schrikFotos() {
		return java.util.Collections.unmodifiableMap(schrikFotos);
	}

	public static String checkSchrikFoto(int foto) {
		if (foto < 0 || foto > Regels.SCHRIK_FOTOS) {
			return "kies foto 1 t/m " + Regels.SCHRIK_FOTOS + " of willekeurig, niet " + foto;
		}
		return null;
	}

	/** @param foto 1 t/m {@link Regels#SCHRIK_FOTOS}, of 0 voor willekeurig */
	public void zetSchrikFoto(String plek, int foto) {
		vereis(checkSchrikFoto(foto));
		if (foto == 0) {
			schrikFotos.remove(plek);
		} else {
			schrikFotos.put(plek, foto);
		}
	}

	/** In de actionbar zolang het doolhof op {@code /doolhof go} wacht. */
	public String doolhofWachttekst() {
		return doolhofWachttekst;
	}

	public void zetDoolhofWachttekst(String tekst) {
		doolhofWachttekst = tekstOfStandaard(tekst, WACHTTEKST);
	}

	/** {@code null}, leeg of {@code -} wist de tekst. */
	public void zetHinttekst(String tekst) {
		String t = schoon(tekst);
		if (t != null && t.equals("-")) {
			t = null;
		}
		vereis(checkTekst(t));
		hinttekst = t;
	}

	// Het Ei

	public int eiTimer() {
		return eiTimer;
	}

	public void zetEiTimer(int minuten) {
		vereis(checkTimer(minuten));
		eiTimer = minuten;
	}

	public int eiBlokken(EiBlok blok) {
		return eiBlokken.get(blok);
	}

	public Map<EiBlok, Integer> eiBlokken() {
		return java.util.Collections.unmodifiableMap(eiBlokken);
	}

	/** Samen hoeveel puntenblokken er gestrooid worden. */
	public int eiTotaal() {
		return eiBlokken.values().stream().mapToInt(Integer::intValue).sum();
	}

	public static String checkEiAantal(int aantal) {
		return aantal < 0 ? "een aantal kan niet negatief zijn" : null;
	}

	/**
	 * Past het totaal met dit nieuwe aantal voor deze soort nog op de deepslate? {@code deepslate}
	 * negatief betekent: het Ei is nog niet vastgelegd, dan kan de mod het niet controleren.
	 */
	public String checkEiBlokken(EiBlok blok, int aantal, int deepslate) {
		String a = checkEiAantal(aantal);
		if (a != null) {
			return a;
		}
		int totaal = eiTotaal() - eiBlokken.get(blok) + aantal;
		if (deepslate >= 0 && totaal > deepslate) {
			return "samen " + totaal + " puntenblokken, maar het Ei heeft maar " + deepslate + " deepslate-plekken";
		}
		return null;
	}

	public void zetEiBlokken(EiBlok blok, int aantal) {
		vereis(checkEiAantal(aantal));
		eiBlokken.put(blok, aantal);
	}

	/** R2.14: op of onder deze hoogte gaat wie van het Ei valt terug naar zijn startplek; {@code null}: uit. */
	public Integer eiValgrens() {
		return eiValgrens;
	}

	public static String checkValgrens(int y) {
		return y < MIN_Y || y > MAX_Y ? "de valgrens moet y " + MIN_Y + " t/m " + MAX_Y + " zijn, niet " + y : null;
	}

	/** {@code null} zet de valgrens uit. */
	public void zetEiValgrens(Integer y) {
		if (y != null) {
			vereis(checkValgrens(y));
		}
		eiValgrens = y;
	}

	// Mob arena

	/** Punten voor een mobtype ({@code zombie} of {@code minecraft:zombie}). */
	public int mobPunten(String type) {
		return mobPunten.getOrDefault(mobSleutel(type), MOB_PUNTEN_ANDER);
	}

	public Map<String, Integer> mobPunten() {
		return java.util.Collections.unmodifiableMap(mobPunten);
	}

	public static String checkMobPunten(int punten) {
		if (punten < 0 || punten > MAX_MOB_PUNTEN) {
			return "punten moeten 0 t/m " + MAX_MOB_PUNTEN + " zijn, niet " + punten;
		}
		return null;
	}

	public void zetMobPunten(String type, int punten) {
		vereis(checkMobPunten(punten));
		mobPunten.put(mobSleutel(type), punten);
	}

	/** {@code minecraft:cave_spider} en {@code Cave_Spider} worden allebei {@code cave_spider}. */
	public static String mobSleutel(String type) {
		String s = type.toLowerCase(Locale.ROOT).strip();
		return s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s;
	}

	public int veldHoogte() {
		return veldHoogte;
	}

	public static String checkVeldHoogte(int blokken) {
		if (blokken < 0 || blokken > MAX_VELD_HOOGTE) {
			return "de veldhoogte moet 0 t/m " + MAX_VELD_HOOGTE + " blokken zijn, niet " + blokken;
		}
		return null;
	}

	public void zetVeldHoogte(int blokken) {
		vereis(checkVeldHoogte(blokken));
		veldHoogte = blokken;
	}

	/** De warden uit het Warden-ei: zijn levens en zijn schade, in HP (2 HP is één hartje). */
	public enum WardenWaarde {
		LEVEN("leven", 200, 20, 1000),
		KLAP("klap", 16, 0, 60),
		BOOM("boom", 10, 0, 40);

		private final String id;
		private final int standaard;
		private final int min;
		private final int max;

		WardenWaarde(String id, int standaard, int min, int max) {
			this.id = id;
			this.standaard = standaard;
			this.min = min;
			this.max = max;
		}

		/** Zoals in {@code /mobarena warden leven}. */
		public String id() {
			return id;
		}

		public int standaard() {
			return standaard;
		}

		public int min() {
			return min;
		}

		public int max() {
			return max;
		}
	}

	public int warden(WardenWaarde w) {
		return warden.get(w);
	}

	public static String checkWarden(WardenWaarde w, int hp) {
		if (hp < w.min() || hp > w.max()) {
			return "de warden-" + w.id() + " moet " + w.min() + " t/m " + w.max() + " HP zijn, niet " + hp;
		}
		return null;
	}

	public void zetWarden(WardenWaarde w, int hp) {
		vereis(checkWarden(w, hp));
		warden.put(w, hp);
	}

	public String aftekst() {
		return aftekst;
	}

	public void zetAftekst(String tekst) {
		aftekst = tekstOfStandaard(tekst, AFTEKST);
	}

	// Clown vs All en FFA

	public String clownWachttekst() {
		return clownWachttekst;
	}

	public void zetClownWachttekst(String tekst) {
		clownWachttekst = tekstOfStandaard(tekst, WACHTTEKST);
	}

	public String ffaWachttekst() {
		return ffaWachttekst;
	}

	public void zetFfaWachttekst(String tekst) {
		ffaWachttekst = tekstOfStandaard(tekst, WACHTTEKST);
	}

	public String finaleWachttekst() {
		return finaleWachttekst;
	}

	public void zetFinaleWachttekst(String tekst) {
		finaleWachttekst = tekstOfStandaard(tekst, WACHTTEKST);
	}

	// Gedeeld

	public static String checkTimer(int minuten) {
		if (minuten < MIN_TIMER || minuten > MAX_TIMER) {
			return "de timer moet " + MIN_TIMER + " t/m " + MAX_TIMER + " minuten zijn, niet " + minuten;
		}
		return null;
	}

	/** Een lopende ronde: een timer korter dan wat er al gespeeld is, mag niet. */
	public static String checkNietVoorbij(int minuten, int gespeeldSeconden) {
		if (minuten * 60 <= gespeeldSeconden) {
			return "er is al " + Tijd.mss(gespeeldSeconden) + " gespeeld; kies meer dan " + (gespeeldSeconden / 60) + " minuten";
		}
		return null;
	}

	public static String checkTekst(String tekst) {
		if (tekst != null && tekst.length() > MAX_TEKST) {
			return "te lang: " + tekst.length() + " tekens, er passen er " + MAX_TEKST;
		}
		return null;
	}

	private String tekstOfStandaard(String tekst, String standaard) {
		String t = schoon(tekst);
		if (t == null || t.equals("-")) {
			return standaard;
		}
		vereis(checkTekst(t));
		return t;
	}

	private static String schoon(String tekst) {
		if (tekst == null) {
			return null;
		}
		String t = tekst.strip();
		return t.isEmpty() ? null : t;
	}

	private static void vereis(String fout) {
		if (fout != null) {
			throw new IllegalArgumentException(fout);
		}
	}
}
