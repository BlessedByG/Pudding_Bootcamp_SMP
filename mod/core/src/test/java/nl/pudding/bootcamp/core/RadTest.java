package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadTest {
	private static int draaiUit(Rad rad) {
		int ticks = 0;
		while (!rad.geland()) {
			rad.tick();
			if (++ticks > 100_000) {
				throw new AssertionError("het rad stopt niet");
			}
		}
		return ticks;
	}

	@Test
	void landtAltijdOpHetDoel() {
		for (int doel = 0; doel < 20; doel++) {
			for (int start = 0; start < 20; start++) {
				for (int rondes = 2; rondes <= 3; rondes++) {
					Rad rad = new Rad(20, doel, start, rondes);
					draaiUit(rad);
					assertEquals(doel, rad.pos(), "doel " + doel + " start " + start);
					assertEquals(0, rad.rest());
				}
			}
		}
	}

	@Test
	void tweeOfDrieRondes() {
		Random random = new Random(42);
		for (int i = 0; i < 200; i++) {
			Rad rad = Rad.willekeurig(20, 13, random);
			assertTrue(rad.rest() >= 40 && rad.rest() < 80, "rest " + rad.rest());
			draaiUit(rad);
			assertEquals(13, rad.pos());
		}
	}

	@Test
	void startOpHetDoelLooptTochRond() {
		Rad rad = new Rad(20, 5, 5, 2);
		assertEquals(40, rad.rest());
	}

	@Test
	void ritmeLooptOp() {
		assertEquals(Rad.SNELSTE_STAP, Rad.wachttijd(60));
		assertEquals(Rad.SNELSTE_STAP, Rad.wachttijd(Rad.VERTRAAG_VANAF));
		assertEquals(Rad.TRAAGSTE_STAP, Rad.wachttijd(1));
		for (int rest = 60; rest > 1; rest--) {
			assertTrue(Rad.wachttijd(rest - 1) >= Rad.wachttijd(rest), "sneller bij rest " + rest);
		}
	}

	@Test
	void elkeStapSchuiftEenPilaarOp() {
		Rad rad = new Rad(20, 3, 18, 2);
		int stappen = 0;
		int verwacht = 18;
		while (!rad.geland()) {
			Rad.Gebeurtenis g = rad.tick();
			if (g != Rad.Gebeurtenis.NIKS) {
				assertEquals(verwacht, rad.vorigePos());
				verwacht = (verwacht + 1) % 20;
				assertEquals(verwacht, rad.pos());
				stappen++;
			}
		}
		assertEquals(45, stappen);
		assertEquals(Rad.Gebeurtenis.NIKS, rad.tick());
	}

	@Test
	void duurtOngeveerEenKwartMinuut() {
		int ticks = draaiUit(new Rad(20, 0, 0, 3));
		assertTrue(ticks > 10 * 20 && ticks < 30 * 20, "ticks " + ticks);
	}

	@Test
	void ongeldigeInvoer() {
		assertThrows(IllegalArgumentException.class, () -> new Rad(20, 20, 0, 2));
		assertThrows(IllegalArgumentException.class, () -> new Rad(20, 0, -1, 2));
	}
}
