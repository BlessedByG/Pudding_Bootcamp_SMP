package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegelsTest {
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();

	@Test
	void momenten() {
		assertEquals(5, Regels.COUNTDOWN);
		assertEquals(10, Regels.OPSTELLING);
		assertEquals(30, Regels.KROON_UITLOG_WACHT);
		assertEquals(15, Regels.EI_HASTE);
		assertEquals(15, Regels.EI_BEVRIEZING);
		assertEquals(10, Regels.BEURT_VIEREN);
		assertEquals(20, Regels.KRONING_VUURWERK);
		assertEquals(60, Regels.KRIMP_SECONDEN);
	}

	@Test
	void startpuntenOmEnOm() {
		assertEquals(1, Regels.startpunt(0, 4));
		assertEquals(4, Regels.startpunt(3, 4));
		assertEquals(1, Regels.startpunt(4, 4));
		assertEquals(2, Regels.startpunt(5, 4));
	}

	@Test
	void jagersWillekeurigEenPerPlek() {
		Random random = new Random(7);
		// 19 jagers op 20 plekken: allemaal een eigen plek, één blijft leeg.
		int[] plek = Regels.verdeelWillekeurig(19, 20, random);
		Set<Integer> gebruikt = new HashSet<>();
		for (int p : plek) {
			assertTrue(p >= 1 && p <= 20);
			assertTrue(gebruikt.add(p), "plek " + p + " dubbel");
		}
		// 25 op 20: iedereen een plek, geen plek meer dan twee keer.
		int[] meer = Regels.verdeelWillekeurig(25, 20, random);
		int[] telling = new int[21];
		for (int p : meer) {
			telling[p]++;
		}
		assertTrue(Arrays.stream(telling).max().getAsInt() <= 2);
	}

	@Test
	void redstoneGokIsVijftigVijftig() {
		Random random = new Random(1);
		int haste = 0;
		for (int i = 0; i < 10_000; i++) {
			if (Regels.redstoneGok(random) == Regels.Gok.HASTE) {
				haste++;
			}
		}
		assertTrue(haste > 4_700 && haste < 5_300, "haste " + haste);
	}

	@Test
	void kroonNaarDeKiller() {
		assertEquals(Optional.of(a), Regels.kroonOpvolger(a, b, List.of(a, b, c), new Random()));
	}

	@Test
	void kroonNaarLaatsteHit() {
		assertEquals(Optional.of(b), Regels.kroonOpvolger(null, b, List.of(a, b, c), new Random()));
	}

	@Test
	void kroonNaarWillekeurigeJager() {
		Random random = new Random(1);
		for (int i = 0; i < 50; i++) {
			UUID gekozen = Regels.kroonOpvolger(null, null, List.of(a, b, c), random).orElseThrow();
			assertTrue(List.of(a, b, c).contains(gekozen));
		}
	}

	@Test
	void dodeKillerTeltNiet() {
		// De killer is zelf al af (bijvoorbeeld tegelijk gestorven): de laatste hit telt.
		assertEquals(Optional.of(b), Regels.kroonOpvolger(c, b, List.of(a, b), new Random()));
	}

	@Test
	void geenOpvolgerZonderJagers() {
		assertTrue(Regels.kroonOpvolger(a, b, List.of(), new Random()).isEmpty());
	}

	@Test
	void eindeBijEenOver() {
		assertFalse(Regels.laatsteOver(2));
		assertTrue(Regels.laatsteOver(1));
		assertTrue(Regels.laatsteOver(0));
	}

	@Test
	void ffaZonderDeUitverkorene() {
		assertEquals(List.of(a, c), Regels.ffaDeelnemers(List.of(a, b, c), b));
		assertEquals(List.of(a, b, c), Regels.ffaDeelnemers(List.of(a, b, c), UUID.randomUUID()));
	}

	@Test
	void laatsteDrieEnTwee() {
		assertEquals("LAATSTE DRIE", Regels.aftelTitle(3));
		assertEquals("LAATSTE TWEE", Regels.aftelTitle(2));
		assertNull(Regels.aftelTitle(4));
		assertNull(Regels.aftelTitle(1));
	}

	@Test
	void quitRegels() {
		assertEquals(Regels.QuitActie.DOOD, Regels.bijQuit(Ronde.CLOWN, Rol.JAGER, false));
		assertEquals(Regels.QuitActie.KROON_WACHT, Regels.bijQuit(Ronde.CLOWN, Rol.KROON, false));
		assertEquals(Regels.QuitActie.NIKS, Regels.bijQuit(Ronde.CLOWN, Rol.KIJKER, false));
		assertEquals(Regels.QuitActie.DOOD, Regels.bijQuit(Ronde.FFA, Rol.FFA, false));
		assertEquals(Regels.QuitActie.DOOD, Regels.bijQuit(Ronde.MOBARENA, Rol.SPELER, true));
		assertEquals(Regels.QuitActie.NIKS, Regels.bijQuit(Ronde.MOBARENA, Rol.SPELER, false));
		for (Ronde r : List.of(Ronde.BASISKAMP, Ronde.DOOLHOF, Ronde.EI, Ronde.QUIZ)) {
			assertEquals(Regels.QuitActie.NIKS, Regels.bijQuit(r, Rol.SPELER, false), r.name());
		}
	}

	@Test
	void joinRegels() {
		assertEquals(Regels.JoinActie.DOOLHOF_START, Regels.bijJoin(Ronde.DOOLHOF, Rol.SPELER, false, false));
		assertEquals(Regels.JoinActie.NAAR_V2, Regels.bijJoin(Ronde.DOOLHOF, Rol.SPELER, true, false));
		assertEquals(Regels.JoinActie.EIGEN_EI_SPAWN, Regels.bijJoin(Ronde.EI, Rol.SPELER, true, false));
		assertEquals(Regels.JoinActie.MOB_TRIBUNE, Regels.bijJoin(Ronde.MOBARENA, Rol.SPELER, true, false));
		assertEquals(Regels.JoinActie.QUIZ_BANK, Regels.bijJoin(Ronde.QUIZ, Rol.SPELER, true, false));
		assertEquals(Regels.JoinActie.KROON_TERUG, Regels.bijJoin(Ronde.CLOWN, Rol.KROON, false, true));
		assertEquals(Regels.JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.CLOWN, Rol.KROON, false, false));
		assertEquals(Regels.JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.CLOWN, Rol.JAGER, false, false));
		assertEquals(Regels.JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.FFA, null, false, false));
		assertEquals(Regels.JoinActie.NIKS, Regels.bijJoin(Ronde.FFA, Rol.STAFF, false, false));
		assertEquals(Regels.JoinActie.NIKS, Regels.bijJoin(Ronde.BASISKAMP, Rol.SPELER, false, false));
	}
}
