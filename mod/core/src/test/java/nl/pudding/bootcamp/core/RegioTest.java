package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
