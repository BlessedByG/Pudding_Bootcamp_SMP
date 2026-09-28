package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WavesDefTest {
	private static final String TWEE_WAVES = """
			{"waves": [
			  {"naam": "Zombies", "mobs": [{"type": "minecraft:zombie", "aantal": 4}]},
			  {"naam": "Boss", "mobs": [
			    {"type": "minecraft:ravager", "aantal": 1},
			    {"type": "minecraft:vindicator", "aantal": 3, "gear": {"mainhand": "minecraft:iron_axe"}}
			  ]}
			]}
			""";

	@Test
	void leestWaves() {
		WavesDef def = WavesDef.uitJson(TWEE_WAVES);
		assertEquals(2, def.waves().size());
		assertEquals("minecraft:iron_axe", def.waves().get(1).mobs().get(1).gear().get("mainhand"));
	}

	@Test
	void totaalPerArenaSchaaltNiet() {
		WavesDef def = WavesDef.uitJson(TWEE_WAVES);
		assertEquals(4, def.waves().get(0).totaal());
		assertEquals(4, def.waves().get(1).totaal());
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
