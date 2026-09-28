package nl.pudding.bootcamp.kits;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.KitDef;
import nl.pudding.bootcamp.core.KitDef.KitFout;
import nl.pudding.bootcamp.core.LootTabel;
import nl.pudding.bootcamp.core.WavesDef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Haalt elk item uit de standaardbestanden door de echte item-parser van 26.2. */
class KitsParseTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void start() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		registries = VanillaRegistries.createLookup();
		// In 26.2 krijgen items hun components pas als de registries geladen zijn; de server doet
		// dit bij het laden van de datapacks.
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(DataComponentInitializers.PendingComponents::apply);
	}

	private static String lees(String bestand) throws IOException {
		try (InputStream in = KitsParseTest.class.getResourceAsStream("/standaard/" + bestand)) {
			assertNotNull(in, bestand + " zit niet in de resources");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void alleStandaardkitsParsen() throws IOException {
		int kits = 0;
		for (String bestand : Standaardbestanden.BESTANDEN) {
			if (!bestand.startsWith("kits/") || !bestand.endsWith(".json")) {
				continue;
			}
			String naam = bestand.substring("kits/".length());
			Kits.Kit kit = Kits.parse(registries, KitDef.uitJson(naam, lees(bestand)));
			assertFalse(kit.items().isEmpty(), naam);
			kits++;
		}
		assertEquals(6, kits);
	}

	@Test
	void enchantmentsKomenErop() throws IOException {
		Kits.Kit boss = Kits.parse(registries, KitDef.uitJson("boss.json", lees("kits/boss.json")));
		ItemStack zwaard = boss.items().stream().map(Kits.Gevuld::stack).filter(s -> s.is(Items.DIAMOND_SWORD)).findFirst().orElseThrow();
		assertTrue(zwaard.isEnchanted(), "het zwaard van de bosskit hoort Sharpness V te hebben");
		Kits.Kit arena = Kits.parse(registries, KitDef.uitJson("arena.json", lees("kits/arena.json")));
		ItemStack gapples = arena.items().stream().map(Kits.Gevuld::stack).filter(s -> s.is(Items.GOLDEN_APPLE)).findFirst().orElseThrow();
		assertEquals(32, gapples.getCount());
		Kits.Kit jager = Kits.parse(registries, KitDef.uitJson("jager.json", lees("kits/jager.json")));
		assertEquals(16, jager.items().stream().map(Kits.Gevuld::stack).filter(s -> s.is(Items.GOLDEN_APPLE)).findFirst().orElseThrow().getCount());
		assertTrue(jager.items().stream().map(Kits.Gevuld::stack).anyMatch(s -> s.is(Items.DIAMOND_AXE) && s.isEnchanted()));
	}

	@Test
	void deKroon() {
		ItemStack kroon = Items26.kroon(registries);
		assertTrue(kroon.is(Items.DIAMOND_HELMET));
		assertTrue(Items26.isKroon(kroon));
		assertTrue(kroon.isEnchanted());
		assertTrue(kroon.has(DataComponents.ITEM_NAME));
		assertFalse(Items26.isKroon(new ItemStack(Items.DIAMOND_HELMET)));
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		Items26.markeer(pickaxe, Items26.EI_TAG, null);
		assertTrue(Items26.heeftTag(pickaxe, Items26.EI_TAG));
		Items26.markeer(pickaxe, Items26.QUIZ_TAG, "goed");
		assertEquals("goed", Items26.tagWaarde(pickaxe, Items26.QUIZ_TAG));
		assertTrue(Items26.heeftTag(pickaxe, Items26.EI_TAG), "een tweede vlag laat de eerste staan");
		assertFalse(Items26.isKroon(ItemStack.EMPTY));
	}

	@Test
	void eenOnbekendItemNoemtBestandEnSlot() {
		KitDef def = KitDef.uitJson("test.json", "{\"hotbar\": [\"minecraft:iron_sword\", \"minecraft:bestaat_niet\"]}");
		KitFout fout = assertThrows(KitFout.class, () -> Kits.parse(registries, def));
		assertTrue(fout.getMessage().startsWith("test.json, hotbar[1]:"), fout.getMessage());
	}

	@Test
	void lootVanHetDoolhofParst() throws Exception {
		LootTabel loot = LootTabel.uitJson(lees(LootTabel.BESTAND));
		for (LootTabel.Item i : loot.items()) {
			assertFalse(Items26.parse(registries, i.item().spec(), i.item().aantal()).isEmpty(), i.item().spec());
		}
	}

	@Test
	void gearVanDeWavesParst() throws Exception {
		WavesDef waves = WavesDef.uitJson(lees(WavesDef.BESTAND));
		for (WavesDef.Wave wave : waves.waves()) {
			for (WavesDef.Mob mob : wave.mobs()) {
				for (String spec : mob.gear().values()) {
					assertFalse(Items26.parse(registries, spec, 1).isEmpty(), spec);
				}
			}
		}
	}
}
