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
	void nummersEnCommandos() {
		assertEquals(List.of(Ronde.BASISKAMP, Ronde.DOOLHOF, Ronde.EI, Ronde.MOBARENA, Ronde.QUIZ, Ronde.CLOWN, Ronde.FFA),
				List.of(Ronde.values()));
		for (Ronde r : Ronde.values()) {
			assertEquals(r.ordinal(), r.nummer());
		}
		assertEquals("doolhof", Ronde.DOOLHOF.commando());
		assertEquals("ei", Ronde.EI.commando());
		assertEquals("mobarena", Ronde.MOBARENA.commando());
		assertEquals("quiz", Ronde.QUIZ.commando());
		assertEquals("clown", Ronde.CLOWN.commando());
		assertEquals("ffa", Ronde.FFA.commando());
		assertNull(Ronde.BASISKAMP.commando());
	}

	@Test
	void alleenEiIsSurvival() {
		for (Ronde r : Ronde.values()) {
			assertEquals(r == Ronde.EI, r.survival(), r.name());
		}
	}

	@Test
	void arenaEnTeams() {
		assertTrue(Ronde.CLOWN.inArena());
		assertTrue(Ronde.FFA.inArena());
		assertFalse(Ronde.MOBARENA.inArena());
		for (Ronde r : Ronde.values()) {
			assertEquals(r.nummer() >= 1 && r.nummer() <= 4, r.metTeams(), r.name());
		}
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
		assertEquals(List.of("doolhof", "doolhof_uit", "poort_doolhof", "doolhof_start", "v2"),
				Ronde.DOOLHOF.ontbreekt(Set.of(), Set.of()));
		assertEquals(List.of("troon"), Ronde.CLOWN.ontbreekt(Set.of("vloer"), Set.of("jager_1", "tribune_1")));
		List<String> mob = Ronde.MOBARENA.ontbreekt(Set.of("mobarena", "veld_1", "veld_2"), Set.of());
		assertTrue(mob.contains("start_2_geel"));
		assertTrue(mob.contains("kooi_1"));
	}

	@Test
	void compleetIsLeeg() {
		for (Ronde r : Ronde.values()) {
			assertTrue(r.ontbreekt(new HashSet<>(r.vereisteRegios()), new HashSet<>(r.vereistePunten())).isEmpty());
		}
	}

	@Test
	void reeksTotHetEersteGat() {
		Set<String> punten = Set.of("jager_1", "jager_2", "jager_3", "jager_5");
		assertEquals(List.of("jager_1", "jager_2", "jager_3"), Ronde.reeks("jager_", punten));
		assertEquals(4, Ronde.volgendVrij("jager_", punten));
		assertEquals(1, Ronde.volgendVrij("tribune_", punten));
		assertTrue(Ronde.reeks("tribune_", punten).isEmpty());
	}
}
