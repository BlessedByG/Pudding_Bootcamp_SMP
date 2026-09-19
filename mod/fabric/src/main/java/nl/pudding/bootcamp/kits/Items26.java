package nl.pudding.bootcamp.kits;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Items uit tekst, met de vanilla item-parser: dezelfde syntax als {@code /give}. */
public final class Items26 {
	/** De kroon: gouden helm met Curse of Binding, onbreekbaar, herkenbaar aan zijn custom data. */
	public static final String KROON = "minecraft:golden_helmet[minecraft:enchantments={\"minecraft:binding_curse\":1},"
			+ "minecraft:unbreakable={},minecraft:custom_data={bootcamp_kroon:1b},"
			+ "minecraft:item_name=\"De Kroon\",minecraft:rarity=\"epic\"]";
	private static final String KROON_TAG = "bootcamp_kroon";

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
		if (stack.isEmpty()) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(KROON_TAG, false);
	}
}
