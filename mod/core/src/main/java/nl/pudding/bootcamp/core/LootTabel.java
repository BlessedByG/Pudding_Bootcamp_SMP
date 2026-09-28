package nl.pudding.bootcamp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * De loot-tabel van het doolhof uit {@code config/bootcamp/doolhof_loot.json}: per kist een
 * willekeurig aantal items tussen {@code perKist[0]} en {@code perKist[1]}, gewogen getrokken.
 */
public record LootTabel(int min, int max, List<Item> items) {
	public static final String BESTAND = "doolhof_loot.json";
	/** Een kist heeft 27 plekken. */
	public static final int MAX_PER_KIST = 27;

	public record Item(KitDef.ItemRegel item, int gewicht) {
	}

	public static LootTabel uitJson(String json) {
		JsonObject root;
		try {
			JsonElement el = JsonParser.parseString(json);
			if (!el.isJsonObject()) {
				throw new IllegalArgumentException("het bestand moet een object zijn");
			}
			root = el.getAsJsonObject();
		} catch (JsonParseException e) {
			throw new IllegalArgumentException(BESTAND + ": geen geldige JSON (" + e.getMessage() + ")");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(BESTAND + ": " + e.getMessage());
		}
		try {
			int min = 2;
			int max = 4;
			if (root.has("perKist")) {
				JsonArray pk = root.getAsJsonArray("perKist");
				if (pk.size() != 2) {
					throw new IllegalArgumentException("perKist moet [min, max] zijn");
				}
				min = pk.get(0).getAsInt();
				max = pk.get(1).getAsInt();
			}
			if (min < 0 || max < min || max > MAX_PER_KIST) {
				throw new IllegalArgumentException("perKist moet 0 <= min <= max <= " + MAX_PER_KIST + " zijn, niet [" + min + ", " + max + "]");
			}
			JsonArray lijst = root.getAsJsonArray("items");
			if (lijst == null || lijst.isEmpty()) {
				throw new IllegalArgumentException("er staat geen enkel item in");
			}
			List<Item> items = new ArrayList<>();
			for (int i = 0; i < lijst.size(); i++) {
				String slot = "items[" + i + "]";
				JsonObject o = lijst.get(i).getAsJsonObject();
				if (!o.has("item")) {
					throw new IllegalArgumentException(slot + ": item ontbreekt");
				}
				KitDef.ItemRegel regel = KitDef.leesItem(BESTAND, slot, o.get("item").getAsString());
				int gewicht = o.has("gewicht") ? o.get("gewicht").getAsInt() : 1;
				if (gewicht < 1) {
					throw new IllegalArgumentException(slot + ": gewicht moet minstens 1 zijn");
				}
				items.add(new Item(regel, gewicht));
			}
			return new LootTabel(min, max, List.copyOf(items));
		} catch (KitDef.KitFout e) {
			throw e;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(BESTAND + ": " + e.getMessage());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException(BESTAND + ": een veld heeft de verkeerde vorm (" + e + ")");
		}
	}

	public int totaalGewicht() {
		return items.stream().mapToInt(Item::gewicht).sum();
	}

	/** Eén item, gewogen getrokken. */
	public KitDef.ItemRegel trekEen(RandomGenerator random) {
		int r = random.nextInt(totaalGewicht());
		for (Item i : items) {
			r -= i.gewicht();
			if (r < 0) {
				return i.item();
			}
		}
		return items.get(items.size() - 1).item();
	}

	/** De inhoud van één kist. */
	public List<KitDef.ItemRegel> trekKist(RandomGenerator random) {
		int n = min + random.nextInt(max - min + 1);
		List<KitDef.ItemRegel> uit = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			uit.add(trekEen(random));
		}
		return uit;
	}
}
