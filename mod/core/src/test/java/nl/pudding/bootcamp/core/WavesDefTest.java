package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavesDefTest {
	private static final String TWEE_WAVES = """
			{"waves": [
			  {"naam": "Zombies", "mobs": [{"type": "minecraft:zombie", "aantal": 20}]},
			  {"naam": "Boss", "schaal": false, "mobs": [
			    {"type": "minecraft:ravager", "aantal": 2},
			    {"type": "minecraft:vindicator", "aantal": 10, "gear": {"mainhand": "minecraft:iron_axe"}}
			  ]}
			]}
			""";

	@Test
	void leestWaves() {
		WavesDef def = WavesDef.uitJson(TWEE_WAVES);
		assertEquals(2, def.waves().size());
		assertTrue(def.waves().get(0).schaal());
		assertFalse(def.waves().get(1).schaal());
		assertEquals("minecraft:iron_axe", def.waves().get(1).mobs().get(1).gear().get("mainhand"));
	}

	@Test
	void totaalSchaalt() {
		WavesDef def = WavesDef.uitJson(TWEE_WAVES);
		assertEquals(20, def.waves().get(0).totaal(20));
		assertEquals(5, def.waves().get(0).totaal(5));
		assertEquals(12, def.waves().get(1).totaal(5));
	}

	@Test
	void foutenZijnLeesbaar() {
		IllegalArgumentException geenWaves = assertThrows(IllegalArgumentException.class, () -> WavesDef.uitJson("{\"waves\": []}"));
		assertTrue(geenWaves.getMessage().startsWith("waves.json:"));

		IllegalArgumentException id = assertThrows(IllegalArgumentException.class,
				() -> WavesDef.uitJson("{\"waves\": [{\"mobs\": [{\"type\": \"Zombie\"}]}]}"));
		assertTrue(id.getMessage().contains("wave 1, mob 1"), id.getMessage());

		assertThrows(IllegalArgumentException.class,
				() -> WavesDef.uitJson("{\"waves\": [{\"mobs\": [{\"type\": \"minecraft:zombie\", \"gear\": {\"hoed\": \"x\"}}]}]}"));
		assertThrows(IllegalArgumentException.class, () -> WavesDef.uitJson("{kapot"));
		assertThrows(IllegalArgumentException.class, () -> WavesDef.uitJson("{}"));
	}
}
