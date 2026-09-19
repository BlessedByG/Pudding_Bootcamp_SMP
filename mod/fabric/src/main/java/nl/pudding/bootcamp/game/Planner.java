package nl.pudding.bootcamp.game;

import nl.pudding.bootcamp.Bootcamp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Drie seconden later": een wachttijd is een tick-teller, geen thread en geen sleep. Alles wat
 * gepland staat hoort bij de lopende ronde en vervalt bij {@code /bc stop} en {@code /bc reset}.
 */
public final class Planner {
	private static final class Taak {
		int ticksOver;
		final Runnable werk;

		Taak(int ticksOver, Runnable werk) {
			this.ticksOver = ticksOver;
			this.werk = werk;
		}
	}

	private static final List<Taak> TAKEN = new ArrayList<>();

	private Planner() {
	}

	public static void na(int ticks, Runnable werk) {
		TAKEN.add(new Taak(Math.max(1, ticks), werk));
	}

	public static void naSeconden(int seconden, Runnable werk) {
		na(seconden * 20, werk);
	}

	/** Elke servertick. */
	public static void tick() {
		if (TAKEN.isEmpty()) {
			return;
		}
		List<Taak> klaar = new ArrayList<>();
		for (Iterator<Taak> it = TAKEN.iterator(); it.hasNext(); ) {
			Taak t = it.next();
			if (--t.ticksOver <= 0) {
				it.remove();
				klaar.add(t);
			}
		}
		// Pas uitvoeren na de lus: een taak mag zelf weer iets plannen of alles wissen.
		for (Taak t : klaar) {
			try {
				t.werk.run();
			} catch (RuntimeException e) {
				Bootcamp.LOG.error("Een geplande taak faalde", e);
			}
		}
	}

	public static void wisAlles() {
		TAKEN.clear();
	}
}
