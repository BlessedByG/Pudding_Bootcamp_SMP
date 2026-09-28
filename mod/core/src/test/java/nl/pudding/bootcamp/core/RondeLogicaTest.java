package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** De regels van de zes rondes; de namen van de geneste klassen staan in REGELS.md. */
class RondeLogicaTest {
	private static List<UUID> spelers(int n) {
		List<UUID> uit = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			uit.add(UUID.randomUUID());
		}
		return uit;
	}

	@Nested
	class TeamKeuzeTest {
		@Test
		void maximumIsVijfOfMeer() {
			assertEquals(5, TeamKeuze.maximum(0));
			assertEquals(5, TeamKeuze.maximum(20));
			assertEquals(6, TeamKeuze.maximum(21));
			assertEquals(6, TeamKeuze.maximum(24));
			assertEquals(7, TeamKeuze.maximum(25));
		}

		@Test
		void volBijHetMaximum() {
			assertFalse(TeamKeuze.vol(4, 5));
			assertTrue(TeamKeuze.vol(5, 5));
		}

		@Test
		void kleinsteTeamBijGelijkHetLot() {
			Map<Kleur, Integer> n = new EnumMap<>(Kleur.class);
			n.put(Kleur.ROOD, 5);
			n.put(Kleur.BLAUW, 3);
			n.put(Kleur.GROEN, 4);
			n.put(Kleur.GEEL, 3);
			Set<Kleur> gezien = new HashSet<>();
			Random random = new Random(2);
			for (int i = 0; i < 100; i++) {
				gezien.add(TeamKeuze.kleinste(n, random));
			}
			assertEquals(Set.of(Kleur.BLAUW, Kleur.GEEL), gezien);
		}
	}

	@Nested
	class KlassementTest {
		@Test
		void gelijkeScoreWieHetEerstHad() {
			UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
			Klassement k = new Klassement();
			k.voegToe(a, 10);
			k.voegToe(b, 5);
			k.voegToe(b, 5);
			k.voegToe(c, 50);
			// a en b allebei 10; a had die score het eerst.
			assertEquals(List.of(c, a, b), k.volgorde().stream().map(Klassement.Regel::speler).toList());
			assertEquals(1, k.plek(c));
			assertEquals(3, k.plek(b));
			k.voegToe(a, 5);
			k.voegToe(b, 5);
			assertEquals(2, k.plek(a));
			assertEquals(c, k.winnaar().orElseThrow().speler());
		}

		@Test
		void nulStaatErNietIn() {
			UUID a = UUID.randomUUID();
			Klassement k = new Klassement();
			k.voegToe(a, 0);
			assertTrue(k.volgorde().isEmpty());
			assertEquals(0, k.plek(a));
			assertTrue(k.winnaar().isEmpty());
		}

		@Test
		void topTien() {
			Klassement k = new Klassement();
			for (int i = 1; i <= 15; i++) {
				k.voegToe(UUID.randomUUID(), i);
			}
			assertEquals(10, k.top(10).size());
			assertEquals(15, k.top(10).get(0).score());
		}
	}

	@Nested
	class EiVerdelingTest {
		@Test
		void geenDubbeleEnDeJuisteAantallen() {
			Map<EiBlok, Integer> n = new EnumMap<>(EiBlok.class);
			for (EiBlok b : EiBlok.values()) {
				n.put(b, b.standaard());
			}
			Map<EiBlok, int[]> uit = EiVerdeling.trek(28_000, n, new Random(4));
			Set<Integer> alle = new HashSet<>();
			for (EiBlok b : EiBlok.values()) {
				assertEquals(b.standaard(), uit.get(b).length);
				for (int i : uit.get(b)) {
					assertTrue(i >= 0 && i < 28_000);
					assertTrue(alle.add(i), "plek " + i + " dubbel");
				}
			}
			assertEquals(236, alle.size());
		}

		@Test
		void allesVolMagPrecies() {
			Map<EiBlok, Integer> n = new EnumMap<>(EiBlok.class);
			n.put(EiBlok.GOLD, 10);
			Map<EiBlok, int[]> uit = EiVerdeling.trek(10, n, new Random(1));
			assertEquals(10, uit.get(EiBlok.GOLD).length);
			assertEquals(0, uit.get(EiBlok.NETHERITE).length);
		}

		@Test
		void teVeelWeigert() {
			Map<EiBlok, Integer> n = new EnumMap<>(EiBlok.class);
			n.put(EiBlok.DIAMOND, 11);
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EiVerdeling.trek(10, n, new Random()));
			assertTrue(e.getMessage().contains("10 deepslate"), e.getMessage());
		}

		@Test
		void blokkenEnPunten() {
			assertEquals(50, EiBlok.NETHERITE.punten());
			assertEquals(10, EiBlok.DIAMOND.punten());
			assertEquals(5, EiBlok.GOLD.punten());
			assertEquals(EiBlok.EMERALD, EiBlok.vanBlok("minecraft:emerald_block"));
			assertNull(EiBlok.vanBlok(EiBlok.DEEPSLATE));
			assertEquals(EiBlok.REDSTONE, EiBlok.vanId("Redstone"));
		}
	}

	@Nested
	class MobSchemaTest {
		private Map<Kleur, List<UUID>> teams(int... groottes) {
			Map<Kleur, List<UUID>> t = new EnumMap<>(Kleur.class);
			for (int i = 0; i < groottes.length; i++) {
				t.put(Kleur.values()[i], spelers(groottes[i]));
			}
			return t;
		}

		@Test
		void vijfBeurtenIedereenEenKeerInElkeArena() {
			MobSchema s = MobSchema.loot(teams(5, 5, 5, 5), new Random(3));
			assertEquals(5, s.beurten());
			assertEquals(2, s.verschuiving());
			for (Kleur k : Kleur.values()) {
				Map<UUID, Integer> arena1 = new HashMap<>();
				Map<UUID, Integer> arena2 = new HashMap<>();
				for (int b = 0; b < 5; b++) {
					arena1.put(s.gepland(b, 1, k), b);
					arena2.put(s.gepland(b, 2, k), b);
				}
				assertEquals(5, arena1.size());
				assertEquals(arena1.keySet(), arena2.keySet());
				for (UUID speler : arena1.keySet()) {
					int een = arena1.get(speler);
					int twee = arena2.get(speler);
					assertTrue(Math.abs(een - twee) > 1, "twee beurten achter elkaar: " + een + " en " + twee);
				}
			}
		}

		@Test
		void hetVoorbeeldUitDeDocs() {
			List<UUID> team = spelers(5);
			MobSchema s = MobSchema.vast(Map.of(Kleur.ROOD, team));
			// Beurt 1: speler 1 in arena 1, speler 3 in arena 2; beurt 4: speler 4 en speler 1.
			assertEquals(team.get(0), s.gepland(0, 1, Kleur.ROOD));
			assertEquals(team.get(2), s.gepland(0, 2, Kleur.ROOD));
			assertEquals(team.get(3), s.gepland(3, 1, Kleur.ROOD));
			assertEquals(team.get(0), s.gepland(3, 2, Kleur.ROOD));
			assertEquals(team.get(1), s.gepland(4, 2, Kleur.ROOD));
		}

		@Test
		void kleinerTeamHeeftExtraBeurten() {
			MobSchema s = MobSchema.loot(teams(5, 5, 4, 4), new Random(9));
			assertEquals(5, s.beurten());
			int leeg = 0;
			for (int b = 0; b < 5; b++) {
				for (int arena = 1; arena <= 2; arena++) {
					if (s.gepland(b, arena, Kleur.GEEL) == null) {
						leeg++;
					}
				}
			}
			// Vier spelers, vijf beurten: in elke arena één lege plek.
			assertEquals(2, leeg);
		}

		@Test
		void extraBeurtPaktIemandDieNogMagEnNietAlSpeelt() {
			List<UUID> rood = spelers(5);
			List<UUID> geel = spelers(4);
			MobSchema s = MobSchema.vast(Map.of(Kleur.ROOD, rood, Kleur.GEEL, geel));
			// Beurt 5 (index 4): arena 1 speelt geel speler 5, die er niet is: extra beurt.
			assertNull(s.gepland(4, 1, Kleur.GEEL));
			UUID tweedeArena = s.gepland(4, 2, Kleur.GEEL);
			assertNotNull(tweedeArena);
			Set<UUID> af = Set.of(geel.get(0));
			List<UUID> aanwezig = new ArrayList<>(rood);
			aanwezig.addAll(geel);
			Random random = new Random(5);
			for (int i = 0; i < 50; i++) {
				MobSchema.Plek extra = s.opstelling(4, af, aanwezig, random).stream()
						.filter(p -> p.arena() == 1 && p.kleur() == Kleur.GEEL).findFirst().orElseThrow();
				assertTrue(extra.extra());
				assertNotNull(extra.speler());
				assertFalse(af.contains(extra.speler()));
				assertFalse(extra.speler().equals(tweedeArena));
			}
		}

		@Test
		void wieAfIsLaatZijnPlekLeeg() {
			List<UUID> rood = spelers(5);
			MobSchema s = MobSchema.vast(Map.of(Kleur.ROOD, rood));
			UUID gepland = s.gepland(2, 1, Kleur.ROOD);
			List<MobSchema.Plek> plekken = s.opstelling(2, Set.of(gepland), rood, new Random());
			MobSchema.Plek p = plekken.stream().filter(x -> x.arena() == 1).findFirst().orElseThrow();
			assertNull(p.speler());
			assertFalse(p.extra());
			// Een leeg team doet niet mee.
			assertTrue(plekken.stream().allMatch(x -> x.kleur() == Kleur.ROOD));
		}

		@Test
		void extraBeurtZonderKandidaatBlijftLeeg() {
			List<UUID> geel = spelers(1);
			MobSchema s = MobSchema.vast(Map.of(Kleur.ROOD, spelers(3), Kleur.GEEL, geel));
			// Geel heeft één speler; in beurt 1 speelt die in arena 1, arena 2 is extra maar er is niemand meer.
			List<MobSchema.Plek> plekken = s.opstelling(0, Set.of(), List.of(geel.get(0)), new Random());
			MobSchema.Plek arena2 = plekken.stream().filter(x -> x.arena() == 2 && x.kleur() == Kleur.GEEL).findFirst().orElseThrow();
			assertTrue(arena2.extra());
			assertNull(arena2.speler());
		}
	}

	@Nested
	class MobVerloopTest {
		private final int[] geen = {0, 0, 0};

		private static int[] n(int een, int twee) {
			return new int[] {0, een, twee};
		}

		private static boolean heeft(List<MobVerloop.Gebeurtenis> g, MobVerloop.Soort s) {
			return g.stream().anyMatch(x -> x.soort() == s);
		}

		@Test
		void eersteSecondeStartWaveEen() {
			MobVerloop v = new MobVerloop(5);
			List<MobVerloop.Gebeurtenis> g = v.seconde(geen, n(4, 4));
			assertTrue(heeft(g, MobVerloop.Soort.START_WAVE));
			assertEquals(1, v.wave());
		}

		@Test
		void volgendeWaveVijfSecondenNaBeideKlaar() {
			MobVerloop v = new MobVerloop(5);
			v.seconde(geen, n(4, 4));
			// Arena 1 is leeg, arena 2 nog niet: geen pauze.
			v.seconde(n(0, 3), n(4, 4));
			assertFalse(v.inPauze());
			v.seconde(n(0, 0), n(4, 4));
			assertTrue(v.inPauze());
			for (int i = 0; i < 4; i++) {
				assertFalse(heeft(v.seconde(geen, n(4, 4)), MobVerloop.Soort.START_WAVE));
			}
			assertTrue(heeft(v.seconde(geen, n(4, 4)), MobVerloop.Soort.START_WAVE));
			assertEquals(2, v.wave());
		}

		@Test
		void naTweeMinutenAltijdKlaar() {
			MobVerloop v = new MobVerloop(5);
			v.seconde(geen, n(4, 4));
			for (int i = 1; i < MobVerloop.MAX_WAVE_SECONDEN; i++) {
				assertFalse(heeft(v.seconde(n(1, 1), n(4, 4)), MobVerloop.Soort.WAVE_GEFORCEERD));
			}
			assertTrue(heeft(v.seconde(n(1, 1), n(4, 4)), MobVerloop.Soort.WAVE_GEFORCEERD));
			assertTrue(v.inPauze());
		}

		@Test
		void forcerenMetHetCommando() {
			MobVerloop v = new MobVerloop(5);
			v.seconde(geen, n(4, 4));
			v.forceer();
			assertTrue(heeft(v.seconde(n(3, 2), n(4, 4)), MobVerloop.Soort.WAVE_GEFORCEERD));
		}

		@Test
		void beurtKlaarNaDeLaatsteWave() {
			MobVerloop v = new MobVerloop(2);
			v.seconde(geen, n(4, 4));
			v.seconde(geen, n(4, 4));
			for (int i = 0; i < MobVerloop.PAUZE; i++) {
				v.seconde(geen, n(4, 4));
			}
			assertEquals(2, v.wave());
			List<MobVerloop.Gebeurtenis> g = v.seconde(geen, n(4, 4));
			assertTrue(heeft(g, MobVerloop.Soort.BEURT_KLAAR));
			assertTrue(v.beurtKlaar());
			assertTrue(v.seconde(geen, n(4, 4)).isEmpty());
		}

		@Test
		void arenaKlaarAlsIedereenAfIs() {
			MobVerloop v = new MobVerloop(5);
			v.seconde(geen, n(4, 4));
			List<MobVerloop.Gebeurtenis> g = v.seconde(n(3, 3), n(0, 2));
			assertTrue(g.stream().anyMatch(x -> x.soort() == MobVerloop.Soort.ARENA_KLAAR && x.arena() == 1));
			assertTrue(v.arenaKlaar(1));
			// De mobs van arena 1 tellen niet meer: arena 2 leeg is genoeg voor de pauze.
			v.seconde(n(3, 0), n(0, 2));
			assertTrue(v.inPauze());
			// Ook arena 2 af: de beurt is klaar.
			assertTrue(heeft(v.seconde(geen, n(0, 0)), MobVerloop.Soort.BEURT_KLAAR));
		}

		@Test
		void legeArenaIsMeteenKlaar() {
			MobVerloop v = new MobVerloop(5);
			List<MobVerloop.Gebeurtenis> g = v.seconde(geen, n(0, 3));
			assertTrue(v.arenaKlaar(1));
			assertTrue(heeft(g, MobVerloop.Soort.START_WAVE));
		}
	}

	@Nested
	class MobPuntenTest {
		@Test
		void puntenPerTypeEnTiebreakOpKills() {
			Instellingen i = new Instellingen();
			MobPunten p = new MobPunten();
			assertEquals(10, p.kill(Kleur.ROOD, "minecraft:ravager", i));
			assertEquals(2, p.kill(Kleur.BLAUW, "minecraft:cave_spider", i));
			assertEquals(1, p.kill(Kleur.BLAUW, "minecraft:vex", i));
			for (int k = 0; k < 7; k++) {
				p.kill(Kleur.BLAUW, "minecraft:zombie", i);
			}
			// Rood 10 uit 1 kill, blauw 10 uit 9 kills: blauw wint op kills.
			assertEquals(10, p.punten(Kleur.ROOD));
			assertEquals(10, p.punten(Kleur.BLAUW));
			assertEquals(List.of(Kleur.BLAUW), p.winnaars(List.of(Kleur.values())));
		}

		@Test
		void helemaalGelijkIsSamen() {
			Instellingen i = new Instellingen();
			MobPunten p = new MobPunten();
			p.kill(Kleur.ROOD, "zombie", i);
			p.kill(Kleur.GEEL, "zombie", i);
			assertEquals(List.of(Kleur.ROOD, Kleur.GEEL), p.winnaars(List.of(Kleur.values())));
		}

		@Test
		void standaardTabel() {
			Instellingen i = new Instellingen();
			assertEquals(1, i.mobPunten("zombie"));
			assertEquals(2, i.mobPunten("skeleton"));
			assertEquals(2, i.mobPunten("spider"));
			assertEquals(3, i.mobPunten("creeper"));
			assertEquals(4, i.mobPunten("witch"));
			assertEquals(5, i.mobPunten("vindicator"));
			assertEquals(8, i.mobPunten("evoker"));
			assertEquals(10, i.mobPunten("Minecraft:Ravager"));
			assertEquals(1, i.mobPunten("husk"));
		}
	}

	@Nested
	class QuizTest {
		@Test
		void goedFoutEnDeReeks() {
			QuizStand q = new QuizStand();
			assertFalse(q.goed());
			assertEquals("Draai het rad", q.presentatorTekst());
			q.geland(Kleur.ROOD);
			assertTrue(q.goed());
			assertEquals("+1 Rood", q.goedTekst());
			assertTrue(q.goed());
			assertTrue(q.goed());
			assertEquals("+1 Rood · 3 op rij", q.goedTekst());
			assertEquals("Aan de beurt: Rood · 3 op rij", q.presentatorTekst());
			assertEquals(3, q.punten(Kleur.ROOD));
			assertTrue(q.fout());
			assertNull(q.aanDeBeurt());
			assertEquals(0, q.reeks());
			assertFalse(q.fout());
			q.geland(Kleur.ROOD);
			assertEquals(0, q.reeks());
		}

		@Test
		void winnaarOfGelijkspel() {
			QuizStand q = new QuizStand();
			q.punt(Kleur.ROOD, 3);
			q.punt(Kleur.GEEL, 3);
			assertEquals(List.of(Kleur.ROOD, Kleur.GEEL), q.leiders());
			q.punt(Kleur.GEEL, -1);
			assertEquals(List.of(Kleur.ROOD), q.leiders());
		}

		@Test
		void hetRadHeeftElkTeamVierKeerZonderGelijkeBuren() {
			List<Kleur> v = QuizRad.VAKKEN;
			assertEquals(16, v.size());
			for (Kleur k : Kleur.values()) {
				assertEquals(4, v.stream().filter(x -> x == k).count(), k.name());
			}
			for (int i = 0; i < v.size(); i++) {
				assertFalse(v.get(i) == v.get((i + 1) % v.size()), "buren gelijk bij " + i);
			}
		}

		@Test
		void standenEnVakken() {
			assertEquals(0, QuizRad.vakBijStand(0));
			// Met de klok mee: na vier standen staat het laatste vak onder het pijltje.
			assertEquals(15, QuizRad.vakBijStand(4));
			for (int vak = 0; vak < 16; vak++) {
				int stand = QuizRad.standVanVak(vak);
				assertEquals(0, stand % 4);
				assertEquals(vak, QuizRad.vakBijStand(stand));
			}
			assertTrue(QuizRad.isVakgrens(2));
			assertTrue(QuizRad.isVakgrens(62));
			assertFalse(QuizRad.isVakgrens(4));
			assertEquals("", QuizRad.glyph(0));
			assertEquals("", QuizRad.glyph(63));
		}

		@Test
		void draaiLandtOpEenVakmiddenElkTeamEvenVaak() {
			Random random = new Random(11);
			Map<Kleur, Integer> n = new EnumMap<>(Kleur.class);
			for (int i = 0; i < 4000; i++) {
				Rad rad = QuizRad.draai(random);
				assertEquals(0, rad.doel() % 4);
				assertTrue(rad.rest() >= 128 && rad.rest() < 256, "rest " + rad.rest());
				n.merge(QuizRad.kleurBijStand(rad.doel()), 1, Integer::sum);
			}
			for (Kleur k : Kleur.values()) {
				assertTrue(n.get(k) > 850 && n.get(k) < 1150, k + " " + n.get(k));
			}
		}

		@Test
		void quizRadRemtAfVanEenNaarZes() {
			assertEquals(1, Rad.wachttijd(200, Rad.QUIZ));
			assertEquals(1, Rad.wachttijd(40, Rad.QUIZ));
			assertEquals(6, Rad.wachttijd(1, Rad.QUIZ));
			Rad rad = new Rad(64, 0, 0, 2, Rad.QUIZ);
			int ticks = 0;
			while (!rad.geland()) {
				rad.tick();
				ticks++;
			}
			assertEquals(0, rad.pos());
			assertTrue(ticks > 5 * 20 && ticks < 20 * 20, "ticks " + ticks);
		}
	}

	@Nested
	class PvpTest {
		@Test
		void deTabelUitDocs04() {
			for (Ronde r : List.of(Ronde.BASISKAMP, Ronde.DOOLHOF, Ronde.EI, Ronde.MOBARENA, Ronde.QUIZ)) {
				assertFalse(PvpRegel.mag(r, false, Rol.SPELER, Rol.SPELER), r.name());
			}
			assertTrue(PvpRegel.mag(Ronde.CLOWN, false, Rol.KROON, Rol.JAGER));
			assertTrue(PvpRegel.mag(Ronde.CLOWN, false, Rol.JAGER, Rol.KROON));
			assertFalse(PvpRegel.mag(Ronde.CLOWN, false, Rol.JAGER, Rol.JAGER));
			assertTrue(PvpRegel.mag(Ronde.FFA, false, Rol.FFA, Rol.FFA));
		}

		@Test
		void nooitTijdensEenOpstellingOfMetEenKijker() {
			assertFalse(PvpRegel.mag(Ronde.FFA, true, Rol.FFA, Rol.FFA));
			assertFalse(PvpRegel.mag(Ronde.CLOWN, true, Rol.KROON, Rol.JAGER));
			assertFalse(PvpRegel.mag(Ronde.FFA, false, Rol.KIJKER, Rol.FFA));
			assertFalse(PvpRegel.mag(Ronde.CLOWN, false, Rol.KROON, Rol.KIJKER));
			assertFalse(PvpRegel.mag(Ronde.FFA, false, Rol.STAFF, Rol.FFA));
		}
	}

	@Nested
	class LootTest {
		private static final String TABEL = """
				{"perKist": [2, 4], "items": [
				  {"item": "minecraft:diamond_chestplate", "gewicht": 1},
				  {"item": "minecraft:arrow 16", "gewicht": 9}
				]}
				""";

		@Test
		void aantalPerKistEnGewicht() {
			LootTabel t = LootTabel.uitJson(TABEL);
			Random random = new Random(8);
			int pijlen = 0;
			int totaal = 0;
			for (int i = 0; i < 1000; i++) {
				List<KitDef.ItemRegel> kist = t.trekKist(random);
				assertTrue(kist.size() >= 2 && kist.size() <= 4);
				for (KitDef.ItemRegel r : kist) {
					totaal++;
					if (r.spec().equals("minecraft:arrow")) {
						pijlen++;
						assertEquals(16, r.aantal());
					}
				}
			}
			double deel = (double) pijlen / totaal;
			assertTrue(deel > 0.85 && deel < 0.95, "pijlen " + deel);
		}

		@Test
		void foutenZijnLeesbaar() {
			assertThrows(IllegalArgumentException.class, () -> LootTabel.uitJson("{\"items\": []}"));
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
					() -> LootTabel.uitJson("{\"perKist\": [5, 2], \"items\": [{\"item\": \"minecraft:bow\"}]}"));
			assertTrue(e.getMessage().startsWith(LootTabel.BESTAND), e.getMessage());
			assertThrows(IllegalArgumentException.class,
					() -> LootTabel.uitJson("{\"items\": [{\"item\": \"minecraft:bow\", \"gewicht\": 0}]}"));
		}
	}

	@Nested
	class InstellingenTest {
		@Test
		void standaarden() {
			Instellingen i = new Instellingen();
			assertEquals(15, i.doolhofTimer());
			assertEquals(4, i.doolhofPoort());
			assertEquals(10, i.doolhofHint());
			assertEquals(15, i.eiTimer());
			assertEquals(6, i.eiBlokken(EiBlok.NETHERITE));
			assertEquals(236, i.eiTotaal());
			assertEquals("Af · je speelt geen beurt meer", i.aftekst());
			assertEquals("Wacht op het startsein", i.clownWachttekst());
			assertEquals("Wacht op het startsein", i.ffaWachttekst());
		}

		@Test
		void grenzenVanHetDoolhof() {
			Instellingen i = new Instellingen();
			assertEquals("de hint (16 min) valt na het einde (15 min)", i.checkDoolhofHint(16));
			assertNotNull(i.checkDoolhofHint(0));
			assertNull(i.checkDoolhofHint(14));
			assertNull(i.checkDoolhofPoort(0));
			assertNotNull(i.checkDoolhofPoort(15));
			assertNotNull(i.checkDoolhofTimer(4));
			assertNotNull(i.checkDoolhofTimer(61));
			// Korter dan de hint (10) mag niet.
			assertNotNull(i.checkDoolhofTimer(10));
			assertNull(i.checkDoolhofTimer(11));
			assertThrows(IllegalArgumentException.class, () -> i.zetDoolhofHint(20));
			assertEquals(10, i.doolhofHint());
		}

		@Test
		void eenLopendeTimerKanNietKorterDanWatErGespeeldIs() {
			assertNotNull(Instellingen.checkNietVoorbij(6, 6 * 60 + 12));
			assertNull(Instellingen.checkNietVoorbij(7, 6 * 60 + 12));
		}

		@Test
		void eiBlokkenPassenOpDeDeepslate() {
			Instellingen i = new Instellingen();
			assertNull(i.checkEiBlokken(EiBlok.DIAMOND, 100, 1000));
			// 236 - 90 + 900 = 1046 > 1000.
			assertNotNull(i.checkEiBlokken(EiBlok.DIAMOND, 900, 1000));
			// Niet vastgelegd: niet te controleren.
			assertNull(i.checkEiBlokken(EiBlok.DIAMOND, 900, -1));
			assertNotNull(i.checkEiBlokken(EiBlok.DIAMOND, -1, 1000));
		}

		@Test
		void teksten() {
			Instellingen i = new Instellingen();
			i.zetHinttekst("Bij de lantaarn");
			assertEquals("Bij de lantaarn", i.hinttekst());
			i.zetHinttekst("-");
			assertNull(i.hinttekst());
			assertThrows(IllegalArgumentException.class, () -> i.zetAftekst("x".repeat(61)));
			i.zetAftekst("Weg");
			i.zetAftekst("-");
			assertEquals(Instellingen.AFTEKST, i.aftekst());
			assertNotNull(Instellingen.checkMobPunten(101));
			assertNull(Instellingen.checkMobPunten(0));
		}
	}
}
