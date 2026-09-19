package nl.pudding.bootcamp.core;

import nl.pudding.bootcamp.core.HordeVerloop.Gebeurtenis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HordeVerloopTest {
	@Test
	void eersteSecondeStartWaveEen() {
		HordeVerloop h = new HordeVerloop(5);
		assertEquals(Gebeurtenis.START_WAVE, h.seconde(0, 20));
		assertEquals(1, h.wave());
	}

	@Test
	void volgendeWaveBijNulMobs() {
		HordeVerloop h = new HordeVerloop(5);
		h.seconde(0, 20);
		assertEquals(Gebeurtenis.NIKS, h.seconde(20, 20));
		assertEquals(Gebeurtenis.NIKS, h.seconde(3, 20));
		assertEquals(Gebeurtenis.START_WAVE, h.seconde(0, 20));
		assertEquals(2, h.wave());
	}

	@Test
	void volgendeWaveNaTweeMinuten() {
		HordeVerloop h = new HordeVerloop(5);
		h.seconde(0, 20);
		for (int i = 1; i < HordeVerloop.MAX_WAVE_SECONDEN; i++) {
			assertEquals(Gebeurtenis.NIKS, h.seconde(4, 20), "seconde " + i);
		}
		// Twee mobs zitten vast achter een muur: de klok vangt het op.
		assertEquals(Gebeurtenis.START_WAVE, h.seconde(2, 20));
		assertEquals(2, h.wave());
	}

	@Test
	void laatsteWaveWachtOpDeMobs() {
		HordeVerloop h = new HordeVerloop(2);
		h.seconde(0, 20);
		h.seconde(0, 20);
		assertEquals(2, h.wave());
		for (int i = 0; i < 500; i++) {
			assertEquals(Gebeurtenis.NIKS, h.seconde(1, 20));
		}
		assertEquals(2, h.wave());
	}

	@Test
	void gewonnen() {
		HordeVerloop h = new HordeVerloop(5);
		for (int w = 1; w <= 5; w++) {
			assertEquals(Gebeurtenis.START_WAVE, h.seconde(0, 12));
			assertEquals(w, h.wave());
			h.seconde(10, 12);
		}
		assertEquals(Gebeurtenis.GEWONNEN, h.seconde(0, 12));
		assertTrue(h.voorbij());
		assertEquals(Gebeurtenis.NIKS, h.seconde(0, 12));
	}

	@Test
	void iedereenDood() {
		HordeVerloop h = new HordeVerloop(5);
		h.seconde(0, 20);
		h.seconde(15, 3);
		assertEquals(Gebeurtenis.IEDEREEN_DOOD, h.seconde(15, 0));
		assertTrue(h.voorbij());
	}
}
