package nl.pudding.bootcamp.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * De inhoud van {@code <wereld>/bootcamp.json}: regio's, punten, doodteksten, de grapjes van de
 * nep-uitgangen, de teamkeuzes, de uitverkorene, de presentator, de uitslag voor de finale en de
 * instellingen per ronde.
 * Geen coördinaten in code.
 *
 * <p>Spelers staan hier op naam, zodat de staff alles kan klaarzetten voordat iemand online is.
 * Namen worden in kleine letters bewaard en vergeleken.
 */
public final class BootcampConfig {
	public static final List<String> GRAPJES = List.of(
			"BOEM. Verkeerde deur.",
			"Haha, nep!",
			"Dit is niet de uitgang, sukkel");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Map<String, Regio> regios = new TreeMap<>();
	private final Map<String, Punt> punten = new TreeMap<>();
	private final List<String> doodteksten = new ArrayList<>(Doodteksten.STANDAARD);
	private final List<String> grapjes = new ArrayList<>(GRAPJES);
	private final Map<String, Kleur> teams = new TreeMap<>();
	private String uitverkoren;
	private String presentator;
	private String winnaarKing;
	private String winnaarFfa;
	private String tweedeFfa;
	private Instellingen instellingen = new Instellingen();

	public Map<String, Regio> regios() {
		return regios;
	}

	public Map<String, Punt> punten() {
		return punten;
	}

	public List<String> doodteksten() {
		return doodteksten;
	}

	/** De grapjes die je in beeld krijgt bij een nep-uitgang. */
	public List<String> grapjes() {
		return grapjes;
	}

	public Instellingen instellingen() {
		return instellingen;
	}

	// Teams

	/** Spelernaam (kleine letters) naar teamkleur. */
	public Map<String, Kleur> teams() {
		return teams;
	}

	public Kleur teamVan(String naam) {
		return naam == null ? null : teams.get(sleutel(naam));
	}

	/** {@code null} haalt de keuze weg. */
	public void zetTeam(String naam, Kleur kleur) {
		if (kleur == null) {
			teams.remove(sleutel(naam));
		} else {
			teams.put(sleutel(naam), kleur);
		}
	}

	public int aantalInTeam(Kleur kleur) {
		return (int) teams.values().stream().filter(k -> k == kleur).count();
	}

	// Rollen op naam

	/** Naam van de uitverkorene in kleine letters, of {@code null}. */
	public String uitverkoren() {
		return uitverkoren;
	}

	public void zetUitverkoren(String naam) {
		this.uitverkoren = naam == null ? null : sleutel(naam);
	}

	public boolean isUitverkoren(String naam) {
		return uitverkoren != null && naam != null && uitverkoren.equals(sleutel(naam));
	}

	/** Naam van de presentator van de quiz in kleine letters, of {@code null}. */
	public String presentator() {
		return presentator;
	}

	public void zetPresentator(String naam) {
		this.presentator = naam == null ? null : sleutel(naam);
	}

	public boolean isPresentator(String naam) {
		return presentator != null && naam != null && presentator.equals(sleutel(naam));
	}

	// Uitslag voor de finale

	/** Winnaar van King of the Hill in kleine letters, of {@code null}. */
	public String winnaarKing() {
		return winnaarKing;
	}

	public void zetWinnaarKing(String naam) {
		winnaarKing = naam == null ? null : sleutel(naam);
	}

	/** Winnaar van de FFA in kleine letters, of {@code null}. */
	public String winnaarFfa() {
		return winnaarFfa;
	}

	/** Wie in de FFA als laatste afviel, in kleine letters, of {@code null}. */
	public String tweedeFfa() {
		return tweedeFfa;
	}

	public void zetUitslagFfa(String winnaar, String tweede) {
		winnaarFfa = winnaar == null ? null : sleutel(winnaar);
		tweedeFfa = tweede == null ? null : sleutel(tweede);
	}

	/** {@code /bc reset}: een nieuwe avond, nog geen winnaars. */
	public void wisUitslag() {
		winnaarKing = null;
		winnaarFfa = null;
		tweedeFfa = null;
	}

	public static String sleutel(String naam) {
		return naam.toLowerCase(Locale.ROOT);
	}

	// JSON

	public String naarJson() {
		JsonObject root = new JsonObject();

		JsonObject r = new JsonObject();
		regios.forEach((naam, regio) -> r.add(naam, regioNaarJson(regio)));
		root.add("regios", r);

		JsonObject p = new JsonObject();
		punten.forEach((naam, punt) -> {
			JsonObject o = new JsonObject();
			o.addProperty("x", punt.x());
			o.addProperty("y", punt.y());
			o.addProperty("z", punt.z());
			if (punt.blok()) {
				o.addProperty("blok", true);
			} else {
				o.addProperty("yaw", punt.yaw());
				o.addProperty("pitch", punt.pitch());
			}
			p.add(naam, o);
		});
		root.add("punten", p);

		JsonArray d = new JsonArray();
		doodteksten.forEach(d::add);
		root.add("doodteksten", d);

		JsonArray g = new JsonArray();
		grapjes.forEach(g::add);
		root.add("grapjes", g);

		JsonObject t = new JsonObject();
		teams.forEach((naam, kleur) -> t.addProperty(naam, kleur.id()));
		root.add("teams", t);

		if (uitverkoren != null) {
			root.addProperty("uitverkoren", uitverkoren);
		}
		if (presentator != null) {
			root.addProperty("presentator", presentator);
		}
		if (winnaarKing != null || winnaarFfa != null || tweedeFfa != null) {
			JsonObject u = new JsonObject();
			if (winnaarKing != null) {
				u.addProperty("kingofthehill", winnaarKing);
			}
			if (winnaarFfa != null) {
				u.addProperty("ffa", winnaarFfa);
			}
			if (tweedeFfa != null) {
				u.addProperty("ffa_tweede", tweedeFfa);
			}
			root.add("uitslag", u);
		}
		root.add("instellingen", instellingenNaarJson(instellingen));
		return GSON.toJson(root);
	}

	/**
	 * Leest een config. Ontbrekende onderdelen zijn leeg of standaard. Een regio in de oude vorm
	 * met één doos ({@code min} en {@code max}) wordt nog gelezen.
	 *
	 * @throws IllegalArgumentException met een leesbare melding als de JSON niet klopt
	 */
	public static BootcampConfig uitJson(String json) {
		BootcampConfig c = new BootcampConfig();
		JsonObject root;
		try {
			JsonElement el = JsonParser.parseString(json);
			if (!el.isJsonObject()) {
				throw new IllegalArgumentException("bootcamp.json: het bestand moet een object zijn");
			}
			root = el.getAsJsonObject();
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("bootcamp.json: geen geldige JSON (" + e.getMessage() + ")");
		}

		try {
			if (root.has("regios")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("regios").entrySet()) {
					c.regios.put(e.getKey(), regioUitJson(e.getKey(), e.getValue().getAsJsonObject()));
				}
			}
			if (root.has("punten")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("punten").entrySet()) {
					JsonObject o = e.getValue().getAsJsonObject();
					boolean blok = o.has("blok") && o.get("blok").getAsBoolean();
					c.punten.put(e.getKey(), new Punt(
							o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(),
							o.has("yaw") ? o.get("yaw").getAsFloat() : 0f,
							o.has("pitch") ? o.get("pitch").getAsFloat() : 0f,
							blok));
				}
			}
			leesTeksten(root, "doodteksten", c.doodteksten);
			if (c.doodteksten.equals(Doodteksten.OUDE_STANDAARD)) {
				// Nooit aangepast: dan krijgt ook een oude wereld de nieuwe teksten.
				c.doodteksten.clear();
				c.doodteksten.addAll(Doodteksten.STANDAARD);
			}
			leesTeksten(root, "grapjes", c.grapjes);
			if (root.has("teams")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("teams").entrySet()) {
					Kleur k = Kleur.vanId(e.getValue().getAsString());
					if (k == null) {
						throw new IllegalArgumentException("teams." + e.getKey() + ": onbekende kleur '" + e.getValue().getAsString() + "'");
					}
					c.zetTeam(e.getKey(), k);
				}
			}
			if (root.has("uitverkoren") && !root.get("uitverkoren").isJsonNull()) {
				c.zetUitverkoren(root.get("uitverkoren").getAsString());
			}
			if (root.has("presentator") && !root.get("presentator").isJsonNull()) {
				c.zetPresentator(root.get("presentator").getAsString());
			}
			if (root.has("uitslag")) {
				JsonObject u = root.getAsJsonObject("uitslag");
				if (u.has("kingofthehill")) {
					c.zetWinnaarKing(u.get("kingofthehill").getAsString());
				}
				c.zetUitslagFfa(u.has("ffa") ? u.get("ffa").getAsString() : null,
						u.has("ffa_tweede") ? u.get("ffa_tweede").getAsString() : null);
			}
			if (root.has("instellingen")) {
				c.instellingen = instellingenUitJson(root.getAsJsonObject("instellingen"));
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage().startsWith("bootcamp.json") ? e.getMessage() : "bootcamp.json: " + e.getMessage());
		} catch (RuntimeException e) {
			// ClassCast, IllegalState, NullPointer uit Gson: een veld heeft de verkeerde vorm.
			throw new IllegalArgumentException("bootcamp.json: een veld heeft de verkeerde vorm (" + e + ")");
		}
		return c;
	}

	private static void leesTeksten(JsonObject root, String veld, List<String> doel) {
		if (!root.has(veld)) {
			return;
		}
		List<String> teksten = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray(veld)) {
			String t = e.getAsString().strip();
			if (!t.isEmpty()) {
				teksten.add(t);
			}
		}
		if (!teksten.isEmpty()) {
			doel.clear();
			doel.addAll(teksten);
		}
	}

	private static JsonObject regioNaarJson(Regio regio) {
		JsonObject o = new JsonObject();
		if (regio.isCilinder()) {
			Regio.Cilinder c = regio.cilinder();
			JsonObject ci = new JsonObject();
			ci.addProperty("x", c.x());
			ci.addProperty("z", c.z());
			ci.addProperty("y", c.y());
			ci.addProperty("diameter", c.diameter());
			ci.addProperty("hoogte", c.hoogte());
			o.add("cilinder", ci);
		} else if (regio.delen().size() == 1) {
			o.add("min", pos(regio.delen().get(0).min()));
			o.add("max", pos(regio.delen().get(0).max()));
		} else {
			JsonArray delen = new JsonArray();
			for (Regio.Doos d : regio.delen()) {
				JsonObject deel = new JsonObject();
				deel.add("min", pos(d.min()));
				deel.add("max", pos(d.max()));
				delen.add(deel);
			}
			o.add("delen", delen);
		}
		return o;
	}

	private static Regio regioUitJson(String naam, JsonObject o) {
		if (o.has("cilinder")) {
			JsonObject c = o.getAsJsonObject("cilinder");
			return Regio.cilinder(c.get("x").getAsDouble(), c.get("z").getAsDouble(), c.get("y").getAsInt(),
					c.get("diameter").getAsInt(), c.has("hoogte") ? c.get("hoogte").getAsInt() : 5);
		}
		if (o.has("delen")) {
			List<Regio.Doos> delen = new ArrayList<>();
			for (JsonElement e : o.getAsJsonArray("delen")) {
				JsonObject d = e.getAsJsonObject();
				delen.add(Regio.Doos.van(pos(d.getAsJsonArray("min")), pos(d.getAsJsonArray("max"))));
			}
			if (delen.isEmpty()) {
				throw new IllegalArgumentException("regio " + naam + " heeft geen delen");
			}
			return Regio.uitDelen(delen);
		}
		return Regio.van(pos(o.getAsJsonArray("min")), pos(o.getAsJsonArray("max")));
	}

	private static JsonObject instellingenNaarJson(Instellingen i) {
		JsonObject o = new JsonObject();
		JsonObject doolhof = new JsonObject();
		doolhof.addProperty("timer", i.doolhofTimer());
		doolhof.addProperty("poort", i.doolhofPoort());
		doolhof.addProperty("hint", i.doolhofHint());
		doolhof.addProperty("poortmelding", i.poortMelding());
		JsonArray valmobs = new JsonArray();
		valmobs.add(i.valMobsMin());
		valmobs.add(i.valMobsMax());
		doolhof.add("valmobs", valmobs);
		doolhof.addProperty("wachttekst", i.doolhofWachttekst());
		if (i.hinttekst() != null) {
			doolhof.addProperty("hinttekst", i.hinttekst());
		}
		o.add("doolhof", doolhof);

		JsonObject ei = new JsonObject();
		ei.addProperty("timer", i.eiTimer());
		JsonObject blokken = new JsonObject();
		i.eiBlokken().forEach((b, n) -> blokken.addProperty(b.id(), n));
		ei.add("blokken", blokken);
		o.add("ei", ei);

		JsonObject mob = new JsonObject();
		JsonObject punten = new JsonObject();
		i.mobPunten().forEach(punten::addProperty);
		mob.add("punten", punten);
		mob.addProperty("aftekst", i.aftekst());
		mob.addProperty("veldhoogte", i.veldHoogte());
		JsonObject warden = new JsonObject();
		for (Instellingen.WardenWaarde w : Instellingen.WardenWaarde.values()) {
			warden.addProperty(w.id(), i.warden(w));
		}
		mob.add("warden", warden);
		o.add("mobarena", mob);

		JsonObject clown = new JsonObject();
		clown.addProperty("wachttekst", i.clownWachttekst());
		o.add("clown", clown);
		JsonObject ffa = new JsonObject();
		ffa.addProperty("wachttekst", i.ffaWachttekst());
		o.add("ffa", ffa);
		JsonObject finale = new JsonObject();
		finale.addProperty("wachttekst", i.finaleWachttekst());
		o.add("finale", finale);
		return o;
	}

	private static Instellingen instellingenUitJson(JsonObject o) {
		Instellingen i = new Instellingen();
		if (o.has("doolhof")) {
			JsonObject d = o.getAsJsonObject("doolhof");
			int timer = d.has("timer") ? d.get("timer").getAsInt() : i.doolhofTimer();
			int poort = d.has("poort") ? d.get("poort").getAsInt() : i.doolhofPoort();
			int hint = d.has("hint") ? d.get("hint").getAsInt() : i.doolhofHint();
			wrap("instellingen.doolhof", () -> i.zetDoolhof(timer, poort, hint));
			if (d.has("hinttekst") && !d.get("hinttekst").isJsonNull()) {
				wrap("instellingen.doolhof.hinttekst", () -> i.zetHinttekst(d.get("hinttekst").getAsString()));
			}
			if (d.has("poortmelding")) {
				wrap("instellingen.doolhof.poortmelding", () -> i.zetPoortMelding(d.get("poortmelding").getAsBoolean()));
			}
			if (d.has("valmobs")) {
				JsonArray v = d.getAsJsonArray("valmobs");
				wrap("instellingen.doolhof.valmobs", () -> i.zetValMobs(v.get(0).getAsInt(), v.get(1).getAsInt()));
			}
			if (d.has("wachttekst")) {
				wrap("instellingen.doolhof.wachttekst", () -> i.zetDoolhofWachttekst(d.get("wachttekst").getAsString()));
			}
		}
		if (o.has("ei")) {
			JsonObject e = o.getAsJsonObject("ei");
			if (e.has("timer")) {
				wrap("instellingen.ei.timer", () -> i.zetEiTimer(e.get("timer").getAsInt()));
			}
			if (e.has("blokken")) {
				Map<EiBlok, Integer> nieuw = new EnumMap<>(EiBlok.class);
				for (Map.Entry<String, JsonElement> b : e.getAsJsonObject("blokken").entrySet()) {
					EiBlok blok = EiBlok.vanId(b.getKey());
					if (blok == null) {
						throw new IllegalArgumentException("instellingen.ei.blokken: onbekende soort '" + b.getKey() + "'");
					}
					nieuw.put(blok, b.getValue().getAsInt());
				}
				nieuw.forEach((blok, n) -> wrap("instellingen.ei.blokken." + blok.id(), () -> i.zetEiBlokken(blok, n)));
			}
		}
		if (o.has("mobarena")) {
			JsonObject m = o.getAsJsonObject("mobarena");
			if (m.has("punten")) {
				for (Map.Entry<String, JsonElement> p : m.getAsJsonObject("punten").entrySet()) {
					wrap("instellingen.mobarena.punten." + p.getKey(), () -> i.zetMobPunten(p.getKey(), p.getValue().getAsInt()));
				}
			}
			if (m.has("aftekst")) {
				wrap("instellingen.mobarena.aftekst", () -> i.zetAftekst(m.get("aftekst").getAsString()));
			}
			if (m.has("veldhoogte")) {
				wrap("instellingen.mobarena.veldhoogte", () -> i.zetVeldHoogte(m.get("veldhoogte").getAsInt()));
			}
			if (m.has("warden")) {
				JsonObject w = m.getAsJsonObject("warden");
				for (Instellingen.WardenWaarde waarde : Instellingen.WardenWaarde.values()) {
					if (w.has(waarde.id())) {
						wrap("instellingen.mobarena.warden." + waarde.id(), () -> i.zetWarden(waarde, w.get(waarde.id()).getAsInt()));
					}
				}
			}
		}
		if (o.has("clown") && o.getAsJsonObject("clown").has("wachttekst")) {
			wrap("instellingen.clown.wachttekst", () -> i.zetClownWachttekst(o.getAsJsonObject("clown").get("wachttekst").getAsString()));
		}
		if (o.has("ffa") && o.getAsJsonObject("ffa").has("wachttekst")) {
			wrap("instellingen.ffa.wachttekst", () -> i.zetFfaWachttekst(o.getAsJsonObject("ffa").get("wachttekst").getAsString()));
		}
		if (o.has("finale") && o.getAsJsonObject("finale").has("wachttekst")) {
			wrap("instellingen.finale.wachttekst", () -> i.zetFinaleWachttekst(o.getAsJsonObject("finale").get("wachttekst").getAsString()));
		}
		return i;
	}

	private static void wrap(String waar, Runnable r) {
		try {
			r.run();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(waar + ": " + e.getMessage());
		}
	}

	private static JsonArray pos(BlokPos p) {
		JsonArray a = new JsonArray();
		a.add(p.x());
		a.add(p.y());
		a.add(p.z());
		return a;
	}

	private static BlokPos pos(JsonArray a) {
		if (a == null || a.size() != 3) {
			throw new IllegalArgumentException("een hoek moet [x, y, z] zijn");
		}
		return new BlokPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}
}
