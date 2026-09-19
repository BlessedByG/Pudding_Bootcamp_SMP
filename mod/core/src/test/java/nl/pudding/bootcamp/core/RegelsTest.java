package nl.pudding.bootcamp.core;

import nl.pudding.bootcamp.core.Regels.FfaStand;
import nl.pudding.bootcamp.core.Regels.JoinActie;
import nl.pudding.bootcamp.core.Regels.QuitActie;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegelsTest {
	private static final UUID CLOWN = UUID.randomUUID();
	private static final UUID A = UUID.randomUUID();
	private static final UUID B = UUID.randomUUID();
	private static final UUID C = UUID.randomUUID();
	private final Random random = new Random(1);

	@Test
	void momenten() {
		assertEquals(180, Regels.DOOLHOF_HINT_BIJ);
		assertEquals(300, Regels.EI_BEACON_BIJ);
		assertEquals(180, Regels.EI_VUURPIJL_BIJ);
		assertEquals(30, Regels.OPSTELLING_START);
		assertEquals(10, Regels.OPSTELLING_WISSEL);
		assertEquals(30, Regels.KONING_UITLOG_WACHT);
		assertEquals(15, Regels.KONING_RESISTANCE);
		assertEquals(300, Regels.FFA_KRIMP_NA);
		assertEquals(120, Regels.FFA_KRIMP_DUUR);
		assertEquals(10, Regels.FFA_KRIMP_NAAR);
		assertEquals(120, Regels.RUST);
		assertEquals(20, Regels.FINALE_BORDER);
		assertEquals(180, Regels.FINALE_KRIMP_NA);
		assertEquals(30, Regels.FINALE_KRIMP_DUUR);
		assertEquals(6, Regels.FINALE_KRIMP_NAAR);
	}

	@Test
	void eersteVijfKrijgenVoorsprong() {
		for (int i = 1; i <= 5; i++) {
			assertTrue(Regels.krijgtVoorsprong(i));
		}
		assertFalse(Regels.krijgtVoorsprong(6));
		assertFalse(Regels.krijgtVoorsprong(0));
	}

	@Test
	void mobsSchalenMee() {
		assertEquals(20, Regels.schaalMobs(20, 20, true));
		assertEquals(5, Regels.schaalMobs(20, 5, true));
		assertEquals(4, Regels.schaalMobs(15, 5, true));
		assertEquals(30, Regels.schaalMobs(20, 30, true));
		assertEquals(1, Regels.schaalMobs(2, 1, true));
		assertEquals(10, Regels.schaalMobs(10, 4, false));
	}

	@Test
	void startpuntenOmEnOm() {
		int[] teller = new int[5];
		for (int i = 0; i < 19; i++) {
			teller[Regels.startpunt(i, 4)]++;
		}
		assertEquals(0, teller[0]);
		assertEquals(5, teller[1]);
		assertEquals(5, teller[2]);
		assertEquals(5, teller[3]);
		assertEquals(4, teller[4]);
	}

	@Test
	void kroonNaarDeKiller() {
		assertEquals(Optional.of(A), Regels.kroonOpvolger(A, B, List.of(A, B, C), random));
	}

	@Test
	void kroonNaarLaatsteHit() {
		assertEquals(Optional.of(B), Regels.kroonOpvolger(null, B, List.of(A, B, C), random));
	}

	@Test
	void kroonNaarWillekeurigeHunter() {
		Set<UUID> gezien = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			gezien.add(Regels.kroonOpvolger(null, null, List.of(A, B, C), random).orElseThrow());
		}
		assertEquals(Set.of(A, B, C), gezien);
	}

	@Test
	void dodeKillerTeltNiet() {
		// A schoot een pijl en ging dood voordat die de koning raakte.
		assertEquals(Optional.of(B), Regels.kroonOpvolger(A, B, List.of(B, C), random));
		assertEquals(Optional.of(C), Regels.kroonOpvolger(A, A, List.of(C), random));
	}

	@Test
	void geenOpvolgerZonderHunters() {
		assertEquals(Optional.empty(), Regels.kroonOpvolger(A, B, List.of(), random));
	}

	@Test
	void ronde4Einde() {
		assertFalse(Regels.ronde4Voorbij(1, 1));
		assertTrue(Regels.ronde4Voorbij(0, 5));
		assertTrue(Regels.ronde4Voorbij(600, 0));
	}

	@Test
	void ffaZonderClownEnFinalist() {
		// B is finalist 1; A ging dood in ronde 4 maar doet gewoon weer mee.
		assertEquals(List.of(A, C), Regels.ffaDeelnemers(List.of(CLOWN, A, B, C), CLOWN, B));
	}

	@Test
	void ffaAlsClownFinalistIs() {
		assertEquals(List.of(A, B, C), Regels.ffaDeelnemers(List.of(CLOWN, A, B, C), CLOWN, CLOWN));
	}

	@Test
	void tiebreakOpKills() {
		List<FfaStand> levend = List.of(new FfaStand(A, 1, 20f), new FfaStand(B, 3, 2f), new FfaStand(C, 2, 20f));
		assertEquals(Optional.of(B), Regels.ffaTiebreak(levend, random));
		assertEquals(Optional.empty(), Regels.ffaTiebreak(List.of(), random));
	}

	@Test
	void tiebreakGelijkOpHp() {
		List<FfaStand> levend = List.of(new FfaStand(A, 2, 8f), new FfaStand(B, 2, 14f));
		assertEquals(Optional.of(B), Regels.ffaTiebreak(levend, random));

		Set<UUID> gezien = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			gezien.add(Regels.ffaTiebreak(List.of(new FfaStand(A, 2, 10f), new FfaStand(B, 2, 10f)), random).orElseThrow());
		}
		assertEquals(Set.of(A, B), gezien);
	}

	@Test
	void quitHunterIsDood() {
		assertEquals(QuitActie.DOOD, Regels.bijQuit(Ronde.KING, Rol.HUNTER));
		assertEquals(QuitActie.DOOD, Regels.bijQuit(Ronde.FFA, Rol.FFA));
		assertEquals(QuitActie.DOOD, Regels.bijQuit(Ronde.HORDE, Rol.SPELER));
		assertEquals(QuitActie.NIKS, Regels.bijQuit(Ronde.KING, Rol.KIJKER));
		assertEquals(QuitActie.NIKS, Regels.bijQuit(Ronde.DOOLHOF, Rol.SPELER));
		assertEquals(QuitActie.NIKS, Regels.bijQuit(Ronde.FFA, Rol.FINALIST));
	}

	@Test
	void quitKoningWacht() {
		assertEquals(QuitActie.KONING_WACHT, Regels.bijQuit(Ronde.KING, Rol.KING));
	}

	@Test
	void quitFinalist() {
		assertEquals(QuitActie.POTJE_VERLOREN, Regels.bijQuit(Ronde.FINALE, Rol.KING));
		assertEquals(QuitActie.NIKS, Regels.bijQuit(Ronde.FINALE, Rol.KIJKER));
	}

	@Test
	void joinInArena() {
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.KING, Rol.HUNTER, false, false));
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.KING, null, false, false));
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.KING, Rol.KING, false, false));
		assertEquals(JoinActie.KONING_TERUG, Regels.bijJoin(Ronde.KING, Rol.KING, false, true));
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.FFA, Rol.FFA, false, false));
		assertEquals(JoinActie.FINALIST_TERUG, Regels.bijJoin(Ronde.FFA, Rol.FINALIST, false, false));
		assertEquals(JoinActie.FINALIST_TERUG, Regels.bijJoin(Ronde.FINALE, Rol.KING, false, false));
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.FINALE, Rol.KIJKER, false, false));
		assertEquals(JoinActie.NIKS, Regels.bijJoin(Ronde.KING, Rol.STAFF, false, false));
	}

	@Test
	void joinInRondeEenTotDrie() {
		assertEquals(JoinActie.NIKS, Regels.bijJoin(Ronde.BASISKAMP, null, false, false));
		assertEquals(JoinActie.STARTPUNT, Regels.bijJoin(Ronde.DOOLHOF, Rol.SPELER, false, false));
		assertEquals(JoinActie.VOLGEND_VERZAMELPUNT, Regels.bijJoin(Ronde.DOOLHOF, Rol.KIJKER, true, false));
		assertEquals(JoinActie.KIJKER_TRIBUNE, Regels.bijJoin(Ronde.HORDE, Rol.SPELER, false, false));
		assertEquals(JoinActie.STARTPUNT, Regels.bijJoin(Ronde.EI, null, false, false));
		assertEquals(JoinActie.VOLGEND_VERZAMELPUNT, Regels.bijJoin(Ronde.EI, Rol.KIJKER, true, false));
	}
}
