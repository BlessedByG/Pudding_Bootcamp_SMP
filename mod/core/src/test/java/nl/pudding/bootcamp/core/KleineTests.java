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
	class BossbarTekstTest {
		@Test
		void formatenUitDeDocs() {
			assertEquals("Pudding Bootcamp", BossbarTekst.BASISKAMP);
			assertEquals("Doolhof · 04:41", BossbarTekst.doolhof(281));
			assertEquals("Doolhof · de tijd is om · gif", BossbarTekst.doolhofGif());
			assertEquals("Doolhof · wacht op de start", BossbarTekst.DOOLHOF_WACHT);
			assertEquals("Het Ei · 07:12", BossbarTekst.ei(432));
			assertEquals("Mob Arena · beurt 3/5 · wave 2", BossbarTekst.mobarena(3, 5, 2));
			assertEquals("Mob Arena · beurt 3/5", BossbarTekst.mobarena(3, 5, 0));
			assertEquals("Quiz · aan de beurt: Groen", BossbarTekst.quiz("Groen"));
			assertEquals("Quiz · draai het rad", BossbarTekst.quiz(null));
			assertEquals("King of the Hill · Kroon: Clown · 12 over", BossbarTekst.clown("Clown", 12));
			assertEquals("FFA · 7 over", BossbarTekst.ffa(7));
			assertEquals("Pudding Bootcamp · King: Speler7", BossbarTekst.king("Speler7"));
		}

		@Test
		void tijdWordtNooitNegatief() {
			assertEquals("00:00", Tijd.mmss(-5));
			assertEquals("15:00", Tijd.mmss(900));
			assertEquals("0:38", Tijd.mss(38));
		}
	}


	@Nested
	class KompasTest {
		@Test
		void achtStreken() {
			assertEquals("noord", Kompas.richting(0, -10));
			assertEquals("zuid", Kompas.richting(0, 10));
			assertEquals("oost", Kompas.richting(10, 0));
			assertEquals("west", Kompas.richting(-10, 0));
			assertEquals("noordoost", Kompas.richting(7, -7));
			assertEquals("zuidwest", Kompas.richting(-7, 7));
			assertEquals("noord", Kompas.richting(1, -10));
		}
	}
}
