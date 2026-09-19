package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** De kleine onderdelen bij elkaar; de namen van de geneste klassen staan in REGELS.md. */
class KleineTests {
	@Nested
	class DoodtekstenTest {
		@Test
		void kiestUitDeLijst() {
			Random random = new Random(3);
			Set<String> gezien = new HashSet<>();
			for (int i = 0; i < 100; i++) {
				gezien.add(Doodteksten.kies(List.of("a", "b"), random));
			}
			assertEquals(Set.of("a", "b"), gezien);
		}

		@Test
		void legeLijstValtTerugOpStandaard() {
			Random random = new Random(3);
			assertTrue(Doodteksten.STANDAARD.contains(Doodteksten.kies(List.of(), random)));
			assertTrue(Doodteksten.STANDAARD.contains(Doodteksten.kies(null, random)));
			assertEquals(3, Doodteksten.STANDAARD.size());
		}
	}

	@Nested
	class ResetRegisterTest {
		@Test
		void eenFalendeStapHoudtDeRestNietTegen() {
			ResetRegister<List<String>> register = new ResetRegister<>();
			register.registreer("een", log -> log.add("een"));
			register.registreer("kapot", log -> {
				throw new IllegalStateException("boem");
			});
			register.registreer("drie", log -> log.add("drie"));

			List<String> log = new ArrayList<>();
			List<ResetRegister.Fout> fouten = register.draai(log);

			assertEquals(List.of("een", "drie"), log);
			assertEquals(1, fouten.size());
			assertEquals("kapot", fouten.get(0).stap());
		}

		@Test
		void zelfdeNaamVervangt() {
			ResetRegister<List<String>> register = new ResetRegister<>();
			register.registreer("a", log -> log.add("oud"));
			register.registreer("b", log -> log.add("b"));
			register.registreer("a", log -> log.add("nieuw"));
			List<String> log = new ArrayList<>();
			register.draai(log);
			assertEquals(List.of("nieuw", "b"), log);
			assertEquals(List.of("a", "b"), register.namen());
		}
	}

	@Nested
	class CountdownTest {
		@Test
		void meldtElkeSecondeEnEenKeerNul() {
			Countdown c = new Countdown(3);
			List<Integer> gemeld = new ArrayList<>();
			for (int i = 0; i < 200; i++) {
				int s = c.tick();
				if (s != Countdown.NIKS) {
					gemeld.add(s);
				}
			}
			assertEquals(List.of(2, 1, 0), gemeld);
			assertTrue(c.klaar());
		}

		@Test
		void nulIsMeteenKlaar() {
			Countdown c = new Countdown(0);
			assertTrue(c.klaar());
			assertEquals(Countdown.NIKS, c.tick());
		}

		@Test
		void secondenOverRondtNaarBoven() {
			Countdown c = new Countdown(10);
			assertEquals(10, c.secondenOver());
			c.tick();
			assertEquals(10, c.secondenOver());
			for (int i = 0; i < 19; i++) {
				c.tick();
			}
			assertEquals(9, c.secondenOver());
		}
	}

	@Nested
	class RegeerperiodesTest {
		@Test
		void elkeWisselIsEenNieuwePeriode() {
			Regeerperiodes r = new Regeerperiodes();
			assertNull(r.langste());
			r.seconde();
			r.nieuweKoning("Clown");
			for (int i = 0; i < 252; i++) {
				r.seconde();
			}
			r.nieuweKoning("X");
			for (int i = 0; i < 38; i++) {
				r.seconde();
			}
			r.nieuweKoning("Clown");
			r.seconde();

			assertEquals(List.of("Clown 4:12", "X 0:38", "Clown 0:01"),
					r.periodes().stream().map(Regeerperiodes.Periode::tekst).toList());
			assertEquals("Clown 4:12", r.langste().tekst());
			r.wis();
			assertTrue(r.periodes().isEmpty());
		}
	}

	@Nested
	class FinaleStandTest {
		@Test
		void tweeGewonnenIsKlaar() {
			UUID a = UUID.randomUUID();
			UUID b = UUID.randomUUID();
			FinaleStand s = new FinaleStand(a, b);
			assertEquals(1, s.potje());
			s.potjeVerlorenDoor(a);
			assertNull(s.winnaar());
			s.potjeVerlorenDoor(b);
			assertNull(s.winnaar());
			assertEquals(3, s.potje());
			s.potjeVerlorenDoor(a);
			assertEquals(b, s.winnaar());
			assertEquals(1, s.winstEen());
			assertEquals(2, s.winstTwee());
		}

		@Test
		void naDeWinstTeltNiksMeer() {
			UUID a = UUID.randomUUID();
			UUID b = UUID.randomUUID();
			FinaleStand s = new FinaleStand(a, b);
			s.potjeVerlorenDoor(b);
			s.potjeVerlorenDoor(b);
			s.potjeVerlorenDoor(a);
			s.potjeVerlorenDoor(a);
			assertEquals(a, s.winnaar());
			assertEquals(0, s.winstTwee());
		}

		@Test
		void tegenstanderEnBuitenstaander() {
			UUID a = UUID.randomUUID();
			UUID b = UUID.randomUUID();
			FinaleStand s = new FinaleStand(a, b);
			assertEquals(b, s.tegenstander(a));
			assertEquals(a, s.tegenstander(b));
			assertFalse(s.isFinalist(UUID.randomUUID()));
			s.potjeVerlorenDoor(UUID.randomUUID());
			assertEquals(1, s.potje());
		}
	}

	@Nested
	class BossbarTekstTest {
		@Test
		void formatenUitDeDocs() {
			assertEquals("Pudding Bootcamp", BossbarTekst.BASISKAMP);
			assertEquals("Doolhof · 09:41", BossbarTekst.doolhof(581));
			assertEquals("Wave 3 · 12 mobs", BossbarTekst.horde(3, 12));
			assertEquals("Het Ei · 07:12", BossbarTekst.ei(432));
			assertEquals("Koning: Clown · 12:34", BossbarTekst.king("Clown", 754));
			assertEquals("Finale over 01:59", BossbarTekst.rust(119));
			assertEquals("FFA · 7 over · 04:59", BossbarTekst.ffa(7, 299));
			assertEquals("Finale · 1 - 0", BossbarTekst.finale(1, 0));
		}

		@Test
		void tijdWordtNooitNegatief() {
			assertEquals("00:00", Tijd.mmss(-5));
			assertEquals("15:00", Tijd.mmss(900));
			assertEquals("0:38", Tijd.mss(38));
		}
	}
}
