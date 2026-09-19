package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
	@Test
	void heenEnTerug() {
		BootcampConfig c = new BootcampConfig();
		c.regios().put("doolhof", Regio.van(new BlokPos(0, 60, 0), new BlokPos(63, 70, 63)));
		c.punten().put("troon", Punt.positie(10.5, 80.0, -3.5, 90f, -10f));
		c.punten().put("lamp_0", Punt.blok(new BlokPos(1, 2, 3)));
		c.doodteksten().add("Nog eentje");
		c.zetSlot("ClownPierce", 7);
		c.zetUitverkoren("ClownPierce");

		BootcampConfig t = BootcampConfig.uitJson(c.naarJson());

		assertEquals(c.regios(), t.regios());
		assertEquals(c.punten(), t.punten());
		assertTrue(t.punten().get("lamp_0").blok());
		assertEquals(new BlokPos(1, 2, 3), t.punten().get("lamp_0").blokPos());
		assertEquals(c.doodteksten(), t.doodteksten());
		assertEquals(7, t.slotVan("clownpierce"));
		assertTrue(t.isUitverkoren("CLOWNPIERCE"));
	}

	@Test
	void leegBestandIsEenLegeConfigMetStandaardDoodteksten() {
		BootcampConfig c = BootcampConfig.uitJson("{}");
		assertTrue(c.regios().isEmpty());
		assertTrue(c.punten().isEmpty());
		assertEquals(Doodteksten.STANDAARD, c.doodteksten());
		assertNull(c.uitverkoren());
		assertFalse(c.isUitverkoren("iemand"));
	}

	@Test
	void kapotteJsonGeeftLeesbareFout() {
		IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> BootcampConfig.uitJson("{niet af"));
		assertTrue(e1.getMessage().startsWith("bootcamp.json:"));
		IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
				() -> BootcampConfig.uitJson("{\"regios\":{\"x\":{\"min\":[1,2],\"max\":[1,2,3]}}}"));
		assertTrue(e2.getMessage().startsWith("bootcamp.json:"));
		assertThrows(IllegalArgumentException.class, () -> BootcampConfig.uitJson("[]"));
	}

	@Test
	void slotIsUniek() {
		BootcampConfig c = new BootcampConfig();
		c.zetSlot("A", 3);
		c.zetSlot("B", 3);
		assertEquals(-1, c.slotVan("A"));
		assertEquals(3, c.slotVan("B"));
		c.zetSlot("B", 4);
		assertEquals(4, c.slotVan("B"));
		assertEquals(1, c.slots().size());
	}

	@Test
	void slotBuitenBereik() {
		BootcampConfig c = new BootcampConfig();
		assertThrows(IllegalArgumentException.class, () -> c.zetSlot("A", 20));
		assertThrows(IllegalArgumentException.class, () -> c.zetSlot("A", -1));
		c.zetSlot("A", 0);
		c.zetSlot("B", 19);
	}
}
