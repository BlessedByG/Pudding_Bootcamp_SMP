package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamOverzichtTest {
	private static Map<Kleur, List<String>> leden(int rood, int blauw, int groen, int geel) {
		Map<Kleur, List<String>> m = new EnumMap<>(Kleur.class);
		int[] aantal = {rood, blauw, groen, geel};
		for (Kleur k : Kleur.values()) {
			List<String> namen = new ArrayList<>();
			for (int i = 0; i < aantal[k.ordinal()]; i++) {
				namen.add(k.id() + i);
			}
			m.put(k, namen);
		}
		return m;
	}

	@Test
	void weinigNamenElkOpEenEigenRegel() {
		List<TeamOverzicht.Regel> r = TeamOverzicht.regels(leden(3, 1, 0, 2));
		// Vier koppen en zes namen.
		assertEquals(10, r.size());
		assertEquals(new TeamOverzicht.Regel(Kleur.ROOD, true, List.of()), r.get(0));
		assertEquals(List.of("rood0"), r.get(1).namen());
		assertEquals(new TeamOverzicht.Regel(Kleur.GROEN, true, List.of()), r.get(6));
		assertEquals(new TeamOverzicht.Regel(Kleur.GEEL, true, List.of()), r.get(7));
	}

	@Test
	void volleTeamsDrieNaastElkaar() {
		// 4 x (1 + 5) = 24 en 4 x (1 + 3) = 16 passen niet; 4 x (1 + 2) = 12 wel.
		List<TeamOverzicht.Regel> r = TeamOverzicht.regels(leden(5, 5, 5, 5));
		assertEquals(12, r.size());
		assertEquals(List.of("rood0", "rood1", "rood2"), r.get(1).namen());
		assertEquals(List.of("rood3", "rood4"), r.get(2).namen());
	}

	@Test
	void pastAltijdInVijftienRegels() {
		for (int n = 0; n <= 12; n++) {
			for (int m = 0; m <= 12; m++) {
				List<TeamOverzicht.Regel> r = TeamOverzicht.regels(leden(n, m, n, m));
				assertTrue(r.size() <= TeamOverzicht.MAX_REGELS, n + "/" + m + ": " + r.size() + " regels");
				int namen = r.stream().mapToInt(x -> x.namen().size()).sum();
				assertEquals(2 * n + 2 * m, namen, "iedereen staat erin");
			}
		}
	}

	@Test
	void legeTeamsAlleenDeKop() {
		List<TeamOverzicht.Regel> r = TeamOverzicht.regels(leden(0, 0, 0, 0));
		assertEquals(4, r.size());
		assertTrue(r.stream().allMatch(TeamOverzicht.Regel::kop));
	}
}
