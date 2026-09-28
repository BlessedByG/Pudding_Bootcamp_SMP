package nl.pudding.bootcamp.kits;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Items uit tekst, met de vanilla item-parser: dezelfde syntax als {@code /give}. */
public final class Items26 {
	/**
	 * De kroon: een diamond helm met dezelfde enchants als de kit plus Curse of Binding, de naam
	 * {@code Kroon} in goud, herkenbaar aan zijn custom data. De kroonhouder is dus niet zwakker dan
	 * de jagers; de zwevende gouden kroon en Glowing laten zien wie hem heeft.
	 */
	public static final String KROON = "minecraft:diamond_helmet[minecraft:enchantments={\"minecraft:protection\":4,"
			+ "\"minecraft:unbreaking\":3,\"minecraft:binding_curse\":1},minecraft:custom_data={bootcamp_kroon:1b},"
			+ "minecraft:item_name={\"text\":\"Kroon\",\"color\":\"gold\"},minecraft:rarity=\"epic\"]";
	public static final String KROON_TAG = "bootcamp_kroon";
	/** Alles uit {@code ei.json}: alleen voor het Ei, daarna weer weg. */
	public static final String EI_TAG = "bootcamp_ei";
	/** De drie items van de presentator in de quiz. */
	public static final String QUIZ_TAG = "bootcamp_quiz";

	private Items26() {
	}

	/**
	 * @throws CommandSyntaxException met de melding van de vanilla parser als de tekst niet klopt
	 */
	public static ItemStack parse(HolderLookup.Provider registries, String spec, int aantal) throws CommandSyntaxException {
		StringReader lezer = new StringReader(spec);
		ItemStack stack = new ItemParser(registries).parse(lezer).createItemStack(aantal);
		if (lezer.canRead()) {
			throw new CommandSyntaxException(null, () -> "onverwachte tekst na het item: '" + lezer.getRemaining() + "'");
		}
		return stack;
	}

	public static ItemStack kroon(HolderLookup.Provider registries) {
		try {
			return parse(registries, KROON, 1);
		} catch (CommandSyntaxException e) {
			throw new IllegalStateException("De kroon is niet te maken: " + e.getMessage(), e);
		}
	}

	public static boolean isKroon(ItemStack stack) {
		return heeftTag(stack, KROON_TAG);
	}

	/** Zet een vlag in de custom data van dit item (de rest blijft staan). */
	public static void markeer(ItemStack stack, String tag, String waarde) {
		CustomData oud = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag t = oud == null ? new CompoundTag() : oud.copyTag();
		if (waarde == null) {
			t.putBoolean(tag, true);
		} else {
			t.putString(tag, waarde);
		}
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
	}

	public static boolean heeftTag(ItemStack stack, String tag) {
		if (stack.isEmpty()) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().contains(tag);
	}

	/** De tekstwaarde van een vlag, of {@code null}. */
	public static String tagWaarde(ItemStack stack, String tag) {
		if (stack.isEmpty()) {
			return null;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? null : data.copyTag().getStringOr(tag, null);
	}

	/**
	 * Haalt elk item met deze vlag weg: uit inventory, armor, offhand en wat de speler met de muis
	 * vasthoudt.
	 *
	 * @return hoeveel stacks er weg zijn
	 */
	public static int haalWeg(ServerPlayer speler, String tag) {
		int weg = 0;
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (heeftTag(inv.getItem(i), tag)) {
				inv.setItem(i, ItemStack.EMPTY);
				weg++;
			}
		}
		if (heeftTag(speler.containerMenu.getCarried(), tag)) {
			speler.containerMenu.setCarried(ItemStack.EMPTY);
			weg++;
		}
		if (weg > 0) {
			speler.inventoryMenu.broadcastChanges();
			speler.containerMenu.broadcastChanges();
		}
		return weg;
	}
}
