package nl.pudding.bootcamp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * De waves van de mob arena uit {@code config/bootcamp/waves.json}. Het aantal geldt per arena
 * (voor 4 spelers) en schaalt niet mee: een lege startplek maakt de wave niet kleiner, zodat beide
 * arena's altijd precies dezelfde mobs krijgen.
 */
public record WavesDef(List<Wave> waves) {
	public static final String BESTAND = "waves.json";
	public static final Set<String> GEAR_SLOTS = Set.of("head", "chest", "legs", "feet", "mainhand", "offhand");
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");

	public record Wave(String naam, List<Mob> mobs) {
		/** Hoeveel mobs deze wave per arena heeft. */
		public int totaal() {
			return mobs.stream().mapToInt(Mob::aantal).sum();
		}
	}

	/**
	 * @param type entity-id, bijvoorbeeld {@code minecraft:zombie}
	 * @param gear slot ({@link #GEAR_SLOTS}) naar item in {@code /give}-syntax
	 */
	public record Mob(String type, int aantal, Map<String, String> gear) {
	}

	public static WavesDef uitJson(String json) {
		try {
			JsonElement el = JsonParser.parseString(json);
			JsonArray lijst = el.getAsJsonObject().getAsJsonArray("waves");
			if (lijst == null || lijst.isEmpty()) {
				throw new IllegalArgumentException("er staat geen enkele wave in");
			}
			List<Wave> waves = new ArrayList<>();
			for (int w = 0; w < lijst.size(); w++) {
				String waar = "wave " + (w + 1);
				JsonObject o = lijst.get(w).getAsJsonObject();
				String naam = o.has("naam") ? o.get("naam").getAsString() : "Wave " + (w + 1);
				JsonArray mobLijst = o.getAsJsonArray("mobs");
				if (mobLijst == null || mobLijst.isEmpty()) {
					throw new IllegalArgumentException(waar + ": geen mobs");
				}
				List<Mob> mobs = new ArrayList<>();
				for (int m = 0; m < mobLijst.size(); m++) {
					JsonObject mo = mobLijst.get(m).getAsJsonObject();
					String mobWaar = waar + ", mob " + (m + 1);
					if (!mo.has("type")) {
						throw new IllegalArgumentException(mobWaar + ": type ontbreekt");
					}
					String type = mo.get("type").getAsString();
					if (!ID.matcher(type).matches()) {
						throw new IllegalArgumentException(mobWaar + ": '" + type + "' is geen geldig id (verwacht minecraft:zombie)");
					}
					int aantal = mo.has("aantal") ? mo.get("aantal").getAsInt() : 1;
					if (aantal < 1 || aantal > 100) {
						throw new IllegalArgumentException(mobWaar + ": aantal moet 1 t/m 100 zijn, niet " + aantal);
					}
					Map<String, String> gear = new LinkedHashMap<>();
					if (mo.has("gear")) {
						for (Map.Entry<String, JsonElement> g : mo.getAsJsonObject("gear").entrySet()) {
							if (!GEAR_SLOTS.contains(g.getKey())) {
								throw new IllegalArgumentException(mobWaar + ": onbekend gear-slot '" + g.getKey() + "'");
							}
							gear.put(g.getKey(), g.getValue().getAsString());
						}
					}
					mobs.add(new Mob(type, aantal, Map.copyOf(gear)));
				}
				waves.add(new Wave(naam, List.copyOf(mobs)));
			}
			return new WavesDef(List.copyOf(waves));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(BESTAND + ": " + e.getMessage());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException(BESTAND + ": geen geldige JSON of een veld heeft de verkeerde vorm (" + e + ")");
		}
	}
}
