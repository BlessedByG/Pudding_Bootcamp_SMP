package nl.pudding.bootcamp.kits;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.KitDef;
import nl.pudding.bootcamp.core.KitDef.KitFout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Kits uit {@code config/bootcamp/kits/<naam>.json}. Het bestand wordt bij elk gebruik opnieuw
 * gelezen, dus je kunt een kit aanpassen zonder te herstarten. Een fout komt als één regel in de
 * console, met bestandsnaam en slot, niet als een crash.
 */
public final class Kits {
	private static final Pattern NAAM = Pattern.compile("[a-z0-9_]{1,32}");
	private static final int HOTBAR_START = 0;
	private static final int INVENTORY_START = 9;

	/** Een geparste kit: klaar om op spelers te zetten. */
	public record Kit(KitDef def, List<Gevuld> items) {
	}

	public record Gevuld(KitDef.Plek plek, ItemStack stack) {
	}

	private Kits() {
	}

	/**
	 * @throws KitFout met één leesbare regel: bestand ontbreekt, JSON kapot, of een item dat de
	 *                 vanilla parser niet snapt
	 */
	public static Kit laad(MinecraftServer server, String naam) {
		String bestand = naam + ".json";
		if (!NAAM.matcher(naam).matches()) {
			throw new KitFout(bestand, null, "een kitnaam is kleine letters, cijfers en _");
		}
		Path pad = Standaardbestanden.kitsMap().resolve(bestand);
		if (!Files.exists(pad)) {
			String extra = naam.equals("basis") ? " De basiskit komt van Pudding; zie basis.README.txt in die map." : "";
			throw new KitFout(bestand, null, "het bestand ontbreekt in " + Standaardbestanden.kitsMap() + "." + extra);
		}
		String json;
		try {
			json = Files.readString(pad, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new KitFout(bestand, null, "niet te lezen (" + e.getMessage() + ")");
		}
		return parse(server.registryAccess(), KitDef.uitJson(bestand, json));
	}

	/** Zet de tekst van elk slot om in een echt item. */
	public static Kit parse(HolderLookup.Provider registries, KitDef def) {
		List<Gevuld> items = new ArrayList<>();
		for (KitDef.Plek plek : def.plekken()) {
			try {
				items.add(new Gevuld(plek, Items26.parse(registries, plek.item().spec(), plek.item().aantal())));
			} catch (CommandSyntaxException e) {
				throw new KitFout(def.bestand(), plek.naam(), "'" + plek.item().spec() + "' is geen geldig item (" + e.getMessage() + ")");
			}
		}
		return new Kit(def, List.copyOf(items));
	}

	/**
	 * Zet een kit op een speler.
	 *
	 * <p>De kroon blijft altijd op: een kit schrijft nooit over de head-slot van wie de kroon
	 * draagt, en {@code clear} haalt hem niet weg. Bij {@code clear: false} komt een item op zijn
	 * slot als dat leeg is en anders ergens in de inventory; armor dat er al zat gaat naar de
	 * inventory.
	 */
	public static void geef(ServerPlayer speler, Kit kit) {
		Inventory inv = speler.getInventory();
		ItemStack hoofd = speler.getItemBySlot(EquipmentSlot.HEAD);
		boolean kroonOp = Items26.isKroon(hoofd);

		if (kit.def().clear()) {
			ItemStack kroon = kroonOp ? hoofd.copy() : ItemStack.EMPTY;
			inv.clearContent();
			if (kroonOp) {
				speler.setItemSlot(EquipmentSlot.HEAD, kroon);
			}
		}

		for (Gevuld g : kit.items()) {
			ItemStack stack = g.stack().copy();
			switch (g.plek().doel()) {
				case HEAD -> {
					if (!kroonOp) {
						zetUitrusting(speler, EquipmentSlot.HEAD, stack);
					}
				}
				case CHEST -> zetUitrusting(speler, EquipmentSlot.CHEST, stack);
				case LEGS -> zetUitrusting(speler, EquipmentSlot.LEGS, stack);
				case FEET -> zetUitrusting(speler, EquipmentSlot.FEET, stack);
				case OFFHAND -> zetUitrusting(speler, EquipmentSlot.OFFHAND, stack);
				case HOTBAR -> zetSlot(speler, HOTBAR_START + g.plek().index(), stack);
				case INVENTORY -> zetSlot(speler, INVENTORY_START + g.plek().index(), stack);
			}
		}
		speler.inventoryMenu.broadcastChanges();
	}

	private static void zetUitrusting(ServerPlayer speler, EquipmentSlot slot, ItemStack stack) {
		ItemStack oud = speler.getItemBySlot(slot);
		speler.setItemSlot(slot, stack);
		if (!oud.isEmpty()) {
			geefOfDrop(speler, oud);
		}
	}

	private static void zetSlot(ServerPlayer speler, int slot, ItemStack stack) {
		Inventory inv = speler.getInventory();
		if (inv.getItem(slot).isEmpty()) {
			inv.setItem(slot, stack);
		} else {
			geefOfDrop(speler, stack);
		}
	}

	public static void geefOfDrop(ServerPlayer speler, ItemStack stack) {
		if (!speler.getInventory().add(stack) && !stack.isEmpty()) {
			speler.drop(stack, false);
		}
	}

	/**
	 * Laadt de kit en zet hem op alle spelers. Gaat het laden mis, dan gebeurt er niks en komt de
	 * fout in de console.
	 *
	 * @return {@code null} als het gelukt is, anders de foutregel
	 */
	public static String geefAan(MinecraftServer server, String naam, Collection<ServerPlayer> spelers) {
		Kit kit;
		try {
			kit = laad(server, naam);
		} catch (KitFout e) {
			Bootcamp.LOG.error("Kit: {}", e.getMessage());
			return e.getMessage();
		}
		for (ServerPlayer s : spelers) {
			geef(s, kit);
		}
		return null;
	}
}
