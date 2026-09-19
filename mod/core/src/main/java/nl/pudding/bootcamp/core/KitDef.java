package nl.pudding.bootcamp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Een kit uit {@code config/bootcamp/kits/<naam>.json}, als tekst: per slot een item in de syntax
 * van {@code /give}, met optioneel een aantal erachter. Het echte parsen van het item doet de
 * mod met de vanilla item-parser; hier zit de vorm en de validatie.
 */
public record KitDef(String bestand, boolean clear, List<Plek> plekken) {
	public static final int HOTBAR_SLOTS = 9;
	public static final int INVENTORY_SLOTS = 27;
	public static final int MAX_AANTAL = 99;

	public enum Doel {
		HEAD, CHEST, LEGS, FEET, OFFHAND, HOTBAR, INVENTORY
	}

	/** Een item met aantal, zoals het in het bestand staat. */
	public record ItemRegel(String spec, int aantal) {
	}

	/**
	 * Eén gevuld slot.
	 *
	 * @param naam  hoe het slot in een foutmelding heet, bijvoorbeeld {@code hotbar[2]}
	 * @param index plek binnen de hotbar of de inventory; 0 voor armor en offhand
	 */
	public record Plek(String naam, Doel doel, int index, ItemRegel item) {
	}

	/** Een fout in een kitbestand: één regel met bestandsnaam en slot. */
	public static final class KitFout extends IllegalArgumentException {
		public KitFout(String bestand, String slot, String melding) {
			super(bestand + (slot == null ? "" : ", " + slot) + ": " + melding);
		}
	}

	private static final Pattern MET_AANTAL = Pattern.compile("^(.*\\S)\\s+(\\d+)$");

	/** Splitst {@code "minecraft:arrow 16"} in het item en het aantal (standaard 1). */
	public static ItemRegel leesItem(String bestand, String slot, String tekst) {
		String t = tekst == null ? "" : tekst.strip();
		if (t.isEmpty()) {
			throw new KitFout(bestand, slot, "leeg item");
		}
		int aantal = 1;
		Matcher m = MET_AANTAL.matcher(t);
		if (m.matches()) {
			t = m.group(1);
			try {
				aantal = Integer.parseInt(m.group(2));
			} catch (NumberFormatException e) {
				throw new KitFout(bestand, slot, "aantal is geen getal: " + m.group(2));
			}
		}
		if (aantal < 1 || aantal > MAX_AANTAL) {
			throw new KitFout(bestand, slot, "aantal moet 1 t/m " + MAX_AANTAL + " zijn, niet " + aantal);
		}
		if (t.chars().anyMatch(Character::isWhitespace) && !t.contains("[")) {
			throw new KitFout(bestand, slot, "onverwachte spatie in '" + t + "'");
		}
		return new ItemRegel(t, aantal);
	}

	public static KitDef uitJson(String bestand, String json) {
		JsonObject root;
		try {
			JsonElement el = JsonParser.parseString(json);
			if (!el.isJsonObject()) {
				throw new KitFout(bestand, null, "het bestand moet een object zijn");
			}
			root = el.getAsJsonObject();
		} catch (JsonParseException e) {
			throw new KitFout(bestand, null, "geen geldige JSON (" + e.getMessage() + ")");
		}

		boolean clear = true;
		if (root.has("clear")) {
			JsonElement c = root.get("clear");
			if (!c.isJsonPrimitive() || !c.getAsJsonPrimitive().isBoolean()) {
				throw new KitFout(bestand, "clear", "moet true of false zijn");
			}
			clear = c.getAsBoolean();
		}

		List<Plek> plekken = new ArrayList<>();
		if (root.has("armor")) {
			if (!root.get("armor").isJsonObject()) {
				throw new KitFout(bestand, "armor", "moet een object zijn met head, chest, legs, feet");
			}
			JsonObject armor = root.getAsJsonObject("armor");
			for (String sleutel : armor.keySet()) {
				Doel doel = switch (sleutel) {
					case "head" -> Doel.HEAD;
					case "chest" -> Doel.CHEST;
					case "legs" -> Doel.LEGS;
					case "feet" -> Doel.FEET;
					default -> throw new KitFout(bestand, "armor." + sleutel, "onbekend slot; kies head, chest, legs of feet");
				};
				voegToe(plekken, bestand, "armor." + sleutel, doel, 0, armor.get(sleutel));
			}
		}
		if (root.has("offhand")) {
			voegToe(plekken, bestand, "offhand", Doel.OFFHAND, 0, root.get("offhand"));
		}
		leesRij(plekken, bestand, root, "hotbar", Doel.HOTBAR, HOTBAR_SLOTS);
		leesRij(plekken, bestand, root, "inventory", Doel.INVENTORY, INVENTORY_SLOTS);

		for (String sleutel : root.keySet()) {
			if (!List.of("clear", "armor", "offhand", "hotbar", "inventory").contains(sleutel)) {
				throw new KitFout(bestand, sleutel, "onbekend veld");
			}
		}
		return new KitDef(bestand, clear, List.copyOf(plekken));
	}

	private static void leesRij(List<Plek> plekken, String bestand, JsonObject root, String veld, Doel doel, int max) {
		if (!root.has(veld)) {
			return;
		}
		if (!root.get(veld).isJsonArray()) {
			throw new KitFout(bestand, veld, "moet een lijst zijn");
		}
		JsonArray rij = root.getAsJsonArray(veld);
		if (rij.size() > max) {
			throw new KitFout(bestand, veld, "te veel items: " + rij.size() + ", er passen er " + max);
		}
		for (int i = 0; i < rij.size(); i++) {
			voegToe(plekken, bestand, veld + "[" + i + "]", doel, i, rij.get(i));
		}
	}

	/** {@code null} of een lege tekst laat het slot leeg. */
	private static void voegToe(List<Plek> plekken, String bestand, String slot, Doel doel, int index, JsonElement el) {
		if (el == null || el.isJsonNull()) {
			return;
		}
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw new KitFout(bestand, slot, "moet tekst zijn, zoals \"minecraft:arrow 16\"");
		}
		String tekst = el.getAsString();
		if (tekst.isBlank()) {
			return;
		}
		plekken.add(new Plek(slot, doel, index, leesItem(bestand, slot, tekst)));
	}

	public boolean heeftHelm() {
		return plekken.stream().anyMatch(p -> p.doel() == Doel.HEAD);
	}
}
