package nl.pudding.bootcamp.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * De inhoud van {@code <wereld>/bootcamp.json}: regio's, punten, doodteksten, de pilaar van elke
 * kop in De Kring en de uitverkorene. Geen coördinaten in code.
 *
 * <p>Spelers staan hier op naam, zodat de staff alles kan klaarzetten voordat iemand online is.
 * Namen worden in kleine letters bewaard en vergeleken.
 */
public final class BootcampConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Map<String, Regio> regios = new TreeMap<>();
	private final Map<String, Punt> punten = new TreeMap<>();
	private final List<String> doodteksten = new ArrayList<>(Doodteksten.STANDAARD);
	private final Map<String, Integer> slots = new TreeMap<>();
	private String uitverkoren;

	public Map<String, Regio> regios() {
		return regios;
	}

	public Map<String, Punt> punten() {
		return punten;
	}

	public List<String> doodteksten() {
		return doodteksten;
	}

	/** Spelernaam (kleine letters) naar pilaar 0 t/m 19. */
	public Map<String, Integer> slots() {
		return slots;
	}

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

	/**
	 * Zet de pilaar van een speler. Een pilaar heeft maar één kop: wie er al stond raakt zijn slot
	 * kwijt.
	 */
	public void zetSlot(String naam, int slot) {
		if (slot < 0 || slot >= Ronde.AANTAL_LAMPEN) {
			throw new IllegalArgumentException("slot moet 0 t/m " + (Ronde.AANTAL_LAMPEN - 1) + " zijn");
		}
		slots.values().removeIf(s -> s == slot);
		slots.put(sleutel(naam), slot);
	}

	/** De pilaar van deze speler, of -1 als hij er geen heeft. */
	public int slotVan(String naam) {
		Integer s = naam == null ? null : slots.get(sleutel(naam));
		return s == null ? -1 : s;
	}

	public static String sleutel(String naam) {
		return naam.toLowerCase(Locale.ROOT);
	}

	// JSON

	public String naarJson() {
		JsonObject root = new JsonObject();

		JsonObject r = new JsonObject();
		regios.forEach((naam, regio) -> {
			JsonObject o = new JsonObject();
			o.add("min", pos(regio.min()));
			o.add("max", pos(regio.max()));
			r.add(naam, o);
		});
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

		JsonObject s = new JsonObject();
		slots.forEach(s::addProperty);
		root.add("slots", s);

		if (uitverkoren != null) {
			root.addProperty("uitverkoren", uitverkoren);
		}
		return GSON.toJson(root);
	}

	/**
	 * Leest een config. Ontbrekende onderdelen zijn leeg (doodteksten: de standaardlijst).
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
					JsonObject o = e.getValue().getAsJsonObject();
					c.regios.put(e.getKey(), Regio.van(pos(o.getAsJsonArray("min")), pos(o.getAsJsonArray("max"))));
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
			if (root.has("doodteksten")) {
				List<String> teksten = new ArrayList<>();
				for (JsonElement e : root.getAsJsonArray("doodteksten")) {
					String t = e.getAsString().strip();
					if (!t.isEmpty()) {
						teksten.add(t);
					}
				}
				if (!teksten.isEmpty()) {
					c.doodteksten.clear();
					c.doodteksten.addAll(teksten);
				}
			}
			if (root.has("slots")) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("slots").entrySet()) {
					c.zetSlot(e.getKey(), e.getValue().getAsInt());
				}
			}
			if (root.has("uitverkoren") && !root.get("uitverkoren").isJsonNull()) {
				c.zetUitverkoren(root.get("uitverkoren").getAsString());
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("bootcamp.json: " + e.getMessage());
		} catch (RuntimeException e) {
			// ClassCast, IllegalState, NullPointer uit Gson: een veld heeft de verkeerde vorm.
			throw new IllegalArgumentException("bootcamp.json: een veld heeft de verkeerde vorm (" + e + ")");
		}
		return c;
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
