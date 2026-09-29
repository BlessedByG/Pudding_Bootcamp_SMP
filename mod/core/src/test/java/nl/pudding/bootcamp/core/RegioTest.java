package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegioTest {
	@Test
	void normaliseertHoeken() {
		Regio r = Regio.van(new BlokPos(10, 70, -5), new BlokPos(-3, 64, 20));
		assertEquals(new BlokPos(-3, 64, -5), r.min());
		assertEquals(new BlokPos(10, 70, 20), r.max());
	}

	@Test
	void centerEnGrootte() {
		// 64 breed op x (0..63), 80 op z (100..179): de border is vierkant, dus 80.
		Regio r = Regio.van(new BlokPos(0, 64, 100), new BlokPos(63, 64, 179));
		assertEquals(32.0, r.centerX());
		assertEquals(140.0, r.centerZ());
		assertEquals(64, r.breedteX());
		assertEquals(80, r.breedteZ());
		assertEquals(80, r.grootte());
	}

	@Test
	void eenBlokIsEenBijEen() {
		Regio r = Regio.van(new BlokPos(5, 64, 5), new BlokPos(5, 64, 5));
		assertEquals(1, r.grootte());
		assertEquals(5.5, r.centerX());
		assertEquals(1, r.aantalBlokken());
	}

	@Test
	void kolomNegeertHoogte() {
		// Twee hoeken op de grond geselecteerd: wie erop staat (y + 1) telt toch mee.
		Regio r = Regio.van(new BlokPos(0, 64, 0), new BlokPos(9, 64, 9));
		assertTrue(r.bevat(4.5, 4.5));
		assertTrue(r.bevatSpeler(4.5, 90, 4.5));
		assertFalse(r.bevatDoos(4.5, 65.0, 4.5));
		assertTrue(r.bevatDoos(4.5, 64.5, 4.5));
	}

	@Test
	void randenHorenErbij() {
		Regio r = Regio.van(new BlokPos(0, 64, 0), new BlokPos(9, 64, 9));
		assertTrue(r.bevat(0.0, 0.0));
		assertTrue(r.bevat(9.99, 9.99));
		assertFalse(r.bevat(10.0, 5.0));
		assertFalse(r.bevat(-0.01, 5.0));
	}

	@Test
	void negatieveCoordinaten() {
		Regio r = Regio.van(new BlokPos(-10, 64, -10), new BlokPos(-1, 64, -1));
		assertEquals(-5.0, r.centerX());
		assertTrue(r.bevat(-0.5, -0.5));
		assertFalse(r.bevat(0.0, -0.5));
	}

	@Test
	void tVormUitTweeDelen() {
		// De balk van de T: x 0..20, z 0..4. De poot: x 8..12, z 5..20.
		Regio t = Regio.van(new BlokPos(0, 64, 0), new BlokPos(20, 64, 4))
				.metDeel(new BlokPos(12, 64, 20), new BlokPos(8, 64, 5));
		assertEquals(2, t.aantalDelen());
		assertTrue(t.bevat(1, 1));
		assertTrue(t.bevat(10, 15));
		// Naast de poot, binnen de doos om alles heen: geen veld.
		assertFalse(t.bevat(2, 15));
		assertEquals(new BlokPos(0, 64, 0), t.omhullende().min());
		assertEquals(new BlokPos(20, 64, 20), t.omhullende().max());
		assertEquals(21, t.grootte());
	}

	@Test
	void overlappendeDelenMogen() {
		Regio r = Regio.van(new BlokPos(0, 64, 0), new BlokPos(9, 64, 9))
				.metDeel(new BlokPos(5, 64, 5), new BlokPos(14, 64, 14));
		assertTrue(r.bevat(7, 7));
		assertTrue(r.bevat(13, 13));
		assertEquals(15 * 15, r.aantalBlokken());
	}

	@Test
	void veldTeltTotEenPaarBlokkenBovenDeSelectie() {
		Regio veld = Regio.van(new BlokPos(0, 64, 0), new BlokPos(9, 64, 9));
		assertTrue(veld.bevatSpelerTot(5, 65, 5, 3));
		assertTrue(veld.bevatSpelerTot(5, 67.9, 5, 3));
		// Het balkon, zes blokken hoger: geen veld.
		assertFalse(veld.bevatSpelerTot(5, 70, 5, 3));
	}

	@Test
	void opDeTribuneTeltVanafDeVloer() {
		// Tribunevloer op y 65, een blok boven het veld, twee delen.
		Regio tribune = Regio.van(new BlokPos(10, 65, 0), new BlokPos(14, 65, 9)).metDeel(new BlokPos(8, 65, 0), new BlokPos(9, 65, 9));
		assertTrue(tribune.staatOp(12, 66, 5, 3));
		assertTrue(tribune.staatOp(8.5, 66, 5, 3));
		// Springen telt nog.
		assertTrue(tribune.staatOp(12, 68.9, 5, 3));
		assertFalse(tribune.staatOp(12, 69, 5, 3));
		// Onder de overhang, op het veld: niet op de tribune.
		assertFalse(tribune.staatOp(8.5, 64, 5, 3));
		assertFalse(tribune.staatOp(7.9, 66, 5, 3));
	}

	@Test
	void cilinderTeltOokDeHoogte() {
		// /clown vloer 60 op (100.5, 64, 200.5): straal 30, 5 hoog vanaf y 64.
		Regio vloer = Regio.cilinder(100.5, 200.5, 64, 60, 5);
		assertTrue(vloer.isCilinder());
		assertTrue(vloer.bevatSpeler(100.5, 64, 200.5));
		assertTrue(vloer.bevatSpeler(129, 68.9, 200.5));
		// Op de tribune: hoger dan 5 blokken.
		assertFalse(vloer.bevatSpeler(129, 69, 200.5));
		// Onder de vloer.
		assertFalse(vloer.bevatSpeler(100.5, 63.9, 200.5));
		// In de hoek van het vierkant, buiten de cirkel.
		assertFalse(vloer.bevatSpeler(125, 65, 225));
		assertEquals(60, vloer.grootte());
		assertEquals(100.5, vloer.centerX());
		assertEquals(new BlokPos(70, 64, 170), vloer.omhullende().min());
		assertEquals(new BlokPos(130, 68, 230), vloer.omhullende().max());
		assertThrows(IllegalArgumentException.class, () -> vloer.metDeel(new BlokPos(0, 0, 0), new BlokPos(1, 1, 1)));
	}
}
