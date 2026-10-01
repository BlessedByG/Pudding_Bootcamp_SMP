package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.List;

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
		c.regios().put("veld_1", Regio.van(new BlokPos(0, 60, 0), new BlokPos(20, 60, 4))
				.metDeel(new BlokPos(8, 60, 5), new BlokPos(12, 60, 20)));
		c.regios().put("vloer", Regio.cilinder(10.5, -3.5, 64, 60, 5));
		c.punten().put("troon", Punt.positie(10.5, 80.0, -3.5, 90f, -10f));
		c.punten().put("quizlamp_rood_2", Punt.blok(new BlokPos(1, 2, 3)));
		c.doodteksten().add("Nog eentje");
		c.grapjes().add("Mis!");
		c.zetTeam("Speler7", Kleur.GROEN);
		c.zetUitverkoren("ClownPierce");
		c.zetPresentator("Pudding");
		c.zetWinnaarKing("ClownPierce");
		c.zetUitslagFfa("Speler7", "Speler2");
		c.instellingen().zetFinaleWachttekst("Zo gaat het beginnen");
		c.instellingen().zetDoolhofTimer(20);
		c.instellingen().zetDoolhofHint(12);
		c.instellingen().zetHinttekst("De echte gang begint bij de lantaarn");
		c.instellingen().zetEiBlokken(EiBlok.NETHERITE, 9);
		c.instellingen().zetMobPunten("minecraft:ravager", 12);
		c.instellingen().zetAftekst("Weg!");
		c.instellingen().zetVeldHoogte(1);
		c.instellingen().zetPoortMelding(false);
		c.instellingen().zetValMobs(2, 6);
		c.instellingen().zetSchrikFoto("schrik_2", 4);
		c.instellingen().zetSchrikFoto("schrik_3", 1);
		c.instellingen().zetSchrikFoto("schrik_3", 0);
		c.instellingen().zetDoolhofWachttekst("Nog even geduld");
		c.instellingen().zetWarden(Instellingen.WardenWaarde.LEVEN, 300);

		BootcampConfig t = BootcampConfig.uitJson(c.naarJson());

		assertEquals(c.regios(), t.regios());
		assertEquals(2, t.regios().get("veld_1").aantalDelen());
		assertTrue(t.regios().get("vloer").isCilinder());
		assertEquals(c.punten(), t.punten());
		assertTrue(t.punten().get("quizlamp_rood_2").blok());
		assertEquals(new BlokPos(1, 2, 3), t.punten().get("quizlamp_rood_2").blokPos());
		assertEquals(c.doodteksten(), t.doodteksten());
		assertEquals(c.grapjes(), t.grapjes());
		assertEquals(Kleur.GROEN, t.teamVan("SPELER7"));
		assertTrue(t.isUitverkoren("CLOWNPIERCE"));
		assertTrue(t.isPresentator("pudding"));
		assertEquals("clownpierce", t.winnaarKing());
		assertEquals("speler7", t.winnaarFfa());
		assertEquals("speler2", t.tweedeFfa());
		assertEquals("Zo gaat het beginnen", t.instellingen().finaleWachttekst());
		assertEquals(20, t.instellingen().doolhofTimer());
		assertEquals(12, t.instellingen().doolhofHint());
		assertEquals(4, t.instellingen().doolhofPoort());
		assertEquals("De echte gang begint bij de lantaarn", t.instellingen().hinttekst());
		assertEquals(9, t.instellingen().eiBlokken(EiBlok.NETHERITE));
		assertEquals(150, t.instellingen().eiBlokken(EiBlok.DIAMOND));
		assertEquals(12, t.instellingen().mobPunten("ravager"));
		assertEquals("Weg!", t.instellingen().aftekst());
		assertEquals(1, t.instellingen().veldHoogte());
		assertFalse(t.instellingen().poortMelding());
		assertEquals(2, t.instellingen().valMobsMin());
		assertEquals(6, t.instellingen().valMobsMax());
		assertEquals(4, t.instellingen().schrikFoto("schrik_2"));
		assertEquals(0, t.instellingen().schrikFoto("schrik_3"));
		assertEquals(1, t.instellingen().schrikFotos().size());
		assertEquals("Nog even geduld", t.instellingen().doolhofWachttekst());
		assertEquals(300, t.instellingen().warden(Instellingen.WardenWaarde.LEVEN));
		assertEquals(8, t.instellingen().warden(Instellingen.WardenWaarde.KLAP));
	}

	@Test
	void leegBestandIsEenLegeConfigMetStandaarden() {
		BootcampConfig c = BootcampConfig.uitJson("{}");
		assertTrue(c.regios().isEmpty());
		assertTrue(c.punten().isEmpty());
		assertTrue(c.teams().isEmpty());
		assertEquals(Doodteksten.STANDAARD, c.doodteksten());
		assertEquals(BootcampConfig.GRAPJES, c.grapjes());
		assertNull(c.uitverkoren());
		assertNull(c.presentator());
		assertNull(c.winnaarKing());
		assertNull(c.winnaarFfa());
		c.zetWinnaarKing("ClownPierce");
		c.zetUitslagFfa("Speler7", null);
		c.wisUitslag();
		assertNull(c.winnaarKing());
		assertNull(c.winnaarFfa());
		assertFalse(c.isUitverkoren("iemand"));
		assertEquals(15, c.instellingen().doolhofTimer());
		assertNull(c.instellingen().hinttekst());
	}

	@Test
	void oudeStandaardDoodtekstenWordenDeNieuwe() {
		BootcampConfig oud = BootcampConfig.uitJson("{\"doodteksten\":[\"Grote L gepakt!\",\"Had je nou maar beter je best gedaan\",\"Gelukkig is dit niet de CSMP\"]}");
		assertEquals(Doodteksten.STANDAARD, oud.doodteksten());
		// Zelf aangepast blijft aangepast, ook als de oude teksten erin staan.
		BootcampConfig eigen = BootcampConfig.uitJson("{\"doodteksten\":[\"Grote L gepakt!\",\"Weg ermee\"]}");
		assertEquals(List.of("Grote L gepakt!", "Weg ermee"), eigen.doodteksten());
	}

	@Test
	void oudeLampWordtLampEen() {
		BootcampConfig c = BootcampConfig.uitJson("{\"punten\":{\"quizlamp_rood\":{\"x\":1,\"y\":2,\"z\":3,\"blok\":true},"
				+ "\"quizlamp_geel\":{\"x\":4,\"y\":5,\"z\":6,\"blok\":true},\"quizlamp_geel_1\":{\"x\":7,\"y\":8,\"z\":9,\"blok\":true}}}");
		assertEquals(new BlokPos(1, 2, 3), c.punten().get("quizlamp_rood_1").blokPos());
		assertFalse(c.punten().containsKey("quizlamp_rood"));
		// Staat lamp 1 er al, dan blijft alles zoals het is.
		assertEquals(new BlokPos(7, 8, 9), c.punten().get("quizlamp_geel_1").blokPos());
		assertTrue(c.punten().containsKey("quizlamp_geel"));
	}

	@Test
	void oudeVormMetEenDoosEnSlotsWordtGelezen() {
		BootcampConfig c = BootcampConfig.uitJson("{\"regios\":{\"arena\":{\"min\":[0,1,2],\"max\":[3,4,5]}},\"slots\":{\"a\":3}}");
		assertEquals(Regio.van(new BlokPos(0, 1, 2), new BlokPos(3, 4, 5)), c.regios().get("arena"));
	}

	@Test
	void kapotteJsonGeeftLeesbareFout() {
		IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> BootcampConfig.uitJson("{niet af"));
		assertTrue(e1.getMessage().startsWith("bootcamp.json:"));
		IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
				() -> BootcampConfig.uitJson("{\"regios\":{\"x\":{\"min\":[1,2],\"max\":[1,2,3]}}}"));
		assertTrue(e2.getMessage().startsWith("bootcamp.json:"));
		IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
				() -> BootcampConfig.uitJson("{\"teams\":{\"a\":\"paars\"}}"));
		assertTrue(e3.getMessage().contains("paars"), e3.getMessage());
		IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class,
				() -> BootcampConfig.uitJson("{\"instellingen\":{\"doolhof\":{\"timer\":8}}}"));
		assertTrue(e4.getMessage().contains("instellingen.doolhof"), e4.getMessage());
		assertThrows(IllegalArgumentException.class, () -> BootcampConfig.uitJson("[]"));
	}

	@Test
	void teamKeuzeOpNaam() {
		BootcampConfig c = new BootcampConfig();
		c.zetTeam("A", Kleur.ROOD);
		c.zetTeam("b", Kleur.ROOD);
		c.zetTeam("C", Kleur.GEEL);
		assertEquals(2, c.aantalInTeam(Kleur.ROOD));
		c.zetTeam("a", null);
		assertEquals(1, c.aantalInTeam(Kleur.ROOD));
		assertNull(c.teamVan("A"));
	}
}
