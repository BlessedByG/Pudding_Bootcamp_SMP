package nl.pudding.bootcamp.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HusselTest {
	@Test
	void niemandHoudtZijnEigenPlek() {
		Random random = new Random(7);
		for (int n = 2; n <= 20; n++) {
			for (int keer = 0; keer < 50; keer++) {
				int[] uit = Hussel.verdeel(n, random);
				for (int i = 0; i < n; i++) {
					assertNotEquals(i, uit[i], "speler " + i + " van " + n + " houdt zijn plek");
				}
				// Elke plek precies één keer.
				int[] gesorteerd = uit.clone();
				Arrays.sort(gesorteerd);
				for (int i = 0; i < n; i++) {
					assertEquals(i, gesorteerd[i]);
				}
			}
		}
	}

	@Test
	void tweeSpelersRuilen() {
		assertArrayEquals(new int[] {1, 0}, Hussel.verdeel(2, new Random(1)));
	}

	@Test
	void eenOfGeenBlijftStaan() {
		assertArrayEquals(new int[] {0}, Hussel.verdeel(1, new Random(1)));
		assertArrayEquals(new int[0], Hussel.verdeel(0, new Random(1)));
	}
}
