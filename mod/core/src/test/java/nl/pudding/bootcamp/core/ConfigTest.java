package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

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
		c.punten().put("quizlamp_rood", Punt.blok(new BlokPos(1, 2, 3)));
		c.doodteksten().add("Nog eentje");
		c.grapjes().add("Mis!");
		c.zetTeam("Speler7", Kleur.GROEN);
		c.zetUitverkoren("ClownPierce");
		c.zetPresentator("Pudding");
		c.instellingen().zetDoolhofTimer(20);
		c.instellingen().zetDoolhofHint(12);
		c.instellingen().zetHinttekst("De echte gang begint bij de lantaarn");
		c.instellingen().zetEiBlokken(EiBlok.NETHERITE, 9);
		c.instellingen().zetMobPunten("minecraft:ravager", 12);
		c.instellingen().zetAftekst("Weg!");
		c.instellingen().zetVeldHoogte(1);
		c.instellingen().zetPoortMelding(false);
		c.instellingen().zetValMobs(2, 6);

		BootcampConfig t = BootcampConfig.uitJson(c.naarJson());

		assertEquals(c.regios(), t.regios());
		assertEquals(2, t.regios().get("veld_1").aantalDelen());
		assertTrue(t.regios().get("vloer").isCilinder());
		assertEquals(c.punten(), t.punten());
		assertTrue(t.punten().get("quizlamp_rood").blok());
		assertEquals(new BlokPos(1, 2, 3), t.punten().get("quizlamp_rood").blokPos());
		assertEquals(c.doodteksten(), t.doodteksten());
		assertEquals(c.grapjes(), t.grapjes());
		assertEquals(Kleur.GROEN, t.teamVan("SPELER7"));
		assertTrue(t.isUitverkoren("CLOWNPIERCE"));
		assertTrue(t.isPresentator("pudding"));
		assertEquals(20, t.instellingen().doolhofTimer());
		assertEquals(12, t.instellingen().doolhofHint());
		assertEquals(4, t.instellingen().doolhofPoort());
		assertEquals("De echte gang begint bij de lantaarn", t.instellingen().hinttekst());
		assertEquals(9, t.instellingen().eiBlokken(EiBlok.NETHERITE));
		assertEquals(90, t.instellingen().eiBlokken(EiBlok.DIAMOND));
		assertEquals(12, t.instellingen().mobPunten("ravager"));
		assertEquals("Weg!", t.instellingen().aftekst());
		assertEquals(1, t.instellingen().veldHoogte());
		assertFalse(t.instellingen().poortMelding());
		assertEquals(2, t.instellingen().valMobsMin());
		assertEquals(6, t.instellingen().valMobsMax());
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
		assertFalse(c.isUitverkoren("iemand"));
		assertEquals(15, c.instellingen().doolhofTimer());
		assertNull(c.instellingen().hinttekst());
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
