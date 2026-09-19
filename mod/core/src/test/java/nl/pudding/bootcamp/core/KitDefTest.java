package nl.pudding.bootcamp.core;

import nl.pudding.bootcamp.core.KitDef.Doel;
import nl.pudding.bootcamp.core.KitDef.ItemRegel;
import nl.pudding.bootcamp.core.KitDef.KitFout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitDefTest {
	private static final String VOORBEELD = """
			{
			  "clear": true,
			  "armor": {
			    "head":  "minecraft:iron_helmet",
			    "chest": "minecraft:iron_chestplate",
			    "legs":  "minecraft:iron_leggings",
			    "feet":  "minecraft:iron_boots"
			  },
			  "offhand": "minecraft:shield",
			  "hotbar": [
			    "minecraft:iron_sword",
			    "minecraft:bow[minecraft:enchantments={\\"minecraft:power\\":1}]",
			    "minecraft:arrow 16",
			    "minecraft:cooked_beef 8"
			  ],
			  "inventory": []
			}
			""";

	@Test
	void leestHetVoorbeeldUitDeDocs() {
		KitDef kit = KitDef.uitJson("test.json", VOORBEELD);
		assertTrue(kit.clear());
		assertTrue(kit.heeftHelm());
		assertEquals(9, kit.plekken().size());

		KitDef.Plek pijlen = kit.plekken().stream().filter(p -> p.naam().equals("hotbar[2]")).findFirst().orElseThrow();
		assertEquals(Doel.HOTBAR, pijlen.doel());
		assertEquals(2, pijlen.index());
		assertEquals(new ItemRegel("minecraft:arrow", 16), pijlen.item());

		KitDef.Plek boog = kit.plekken().stream().filter(p -> p.naam().equals("hotbar[1]")).findFirst().orElseThrow();
		assertEquals("minecraft:bow[minecraft:enchantments={\"minecraft:power\":1}]", boog.item().spec());
		assertEquals(1, boog.item().aantal());
	}

	@Test
	void aantalAchterComponents() {
		ItemRegel r = KitDef.leesItem("k.json", "hotbar[0]", "minecraft:potion[potion_contents={potion:\"minecraft:swiftness\"}] 2");
		assertEquals(2, r.aantal());
		assertTrue(r.spec().endsWith("}]"));
	}

	@Test
	void toevoegKitZonderClear() {
		KitDef kit = KitDef.uitJson("kroonpakket.json",
				"{\"clear\": false, \"inventory\": [\"minecraft:golden_apple 2\", \"minecraft:ender_pearl 2\"]}");
		assertFalse(kit.clear());
		assertFalse(kit.heeftHelm());
		assertEquals(2, kit.plekken().size());
	}

	@Test
	void legeSlotsWordenOvergeslagen() {
		KitDef kit = KitDef.uitJson("k.json", "{\"hotbar\": [\"minecraft:stick\", null, \"\", \"minecraft:apple\"]}");
		assertEquals(2, kit.plekken().size());
		assertEquals(3, kit.plekken().get(1).index());
	}

	@Test
	void foutenNoemenBestandEnSlot() {
		KitFout aantal = assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{\"hotbar\": [\"minecraft:arrow 0\"]}"));
		assertTrue(aantal.getMessage().startsWith("arena.json, hotbar[0]:"), aantal.getMessage());

		KitFout slot = assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{\"armor\": {\"hoofd\": \"minecraft:iron_helmet\"}}"));
		assertTrue(slot.getMessage().startsWith("arena.json, armor.hoofd:"), slot.getMessage());

		KitFout vol = assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json",
				"{\"hotbar\": [\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\",\"a:b\"]}"));
		assertTrue(vol.getMessage().contains("hotbar"), vol.getMessage());

		KitFout json = assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{kapot"));
		assertTrue(json.getMessage().startsWith("arena.json:"), json.getMessage());

		assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{\"hotbar\": [5]}"));
		assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{\"hotbaar\": []}"));
		assertThrows(KitFout.class, () -> KitDef.uitJson("arena.json", "{\"hotbar\": [\"minecraft:iron sword\"]}"));
	}
}
