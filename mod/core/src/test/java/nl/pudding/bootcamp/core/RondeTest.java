package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RondeTest {
	@Test
	void duur() {
		assertEquals(600, Ronde.DOOLHOF.duurSeconden());
		assertEquals(600, Ronde.HORDE.duurSeconden());
		assertEquals(600, Ronde.EI.duurSeconden());
		assertEquals(900, Ronde.KING.duurSeconden());
		assertEquals(600, Ronde.FFA.duurSeconden());
	}

	@Test
	void alleenEiIsSurvival() {
		for (Ronde r : Ronde.values()) {
			assertEquals(r == Ronde.EI, r.survival(), r.name());
		}
	}

	@Test
	void arenaRondes() {
		assertTrue(Ronde.KING.inArena());
		assertTrue(Ronde.FFA.inArena());
		assertTrue(Ronde.FINALE.inArena());
		assertFalse(Ronde.HORDE.inArena());
	}

	@Test
	void vanNummer() {
		for (Ronde r : Ronde.values()) {
			assertEquals(r, Ronde.vanNummer(r.nummer()));
		}
		assertNull(Ronde.vanNummer(7));
	}

	@Test
	void ontbreektNoemtAlles() {
		Set<String> regios = Set.of("vloer");
		Set<String> punten = Set.of("troon", "hunter_1", "hunter_2", "hunter_4", "tribune_1", "tribune_2", "tribune_3");
		assertEquals(List.of("hunter_3", "tribune_4"), Ronde.KING.ontbreekt(regios, punten));
		assertEquals(List.of("doolhof", "doolhof_uit", "doolhof_start", "v2"), Ronde.DOOLHOF.ontbreekt(Set.of(), Set.of()));
	}

	@Test
	void compleetIsLeeg() {
		for (Ronde r : Ronde.values()) {
			assertTrue(r.ontbreekt(new HashSet<>(r.vereisteRegios()), new HashSet<>(r.vereistePunten())).isEmpty());
		}
	}

	@Test
	void radWilTwintigLampen() {
		List<String> lampen = Ronde.vereistePuntenRad();
		assertEquals(20, lampen.size());
		assertEquals("lamp_0", lampen.get(0));
		assertEquals("lamp_19", lampen.get(19));
	}
}
