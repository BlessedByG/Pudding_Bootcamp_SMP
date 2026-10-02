package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * De stand van de quiz: punten per team, wie er aan de beurt is en de reeks goede antwoorden van
 * dat team. Goed: +1 en het team blijft aan de beurt. Fout of een nieuwe draai: niemand aan de
 * beurt, de reeks op nul.
 */
public final class QuizStand {
	/** Vanaf zoveel op rij staat de reeks in de subtitle. */
	public static final int REEKS_VANAF = 2;

	private final Map<Kleur, Integer> punten = new EnumMap<>(Kleur.class);
	private Kleur aanDeBeurt;
	private int reeks;

	public QuizStand() {
		for (Kleur k : Kleur.values()) {
			punten.put(k, 0);
		}
	}

	public Kleur aanDeBeurt() {
		return aanDeBeurt;
	}

	public int reeks() {
		return reeks;
	}

	public int punten(Kleur k) {
		return punten.get(k);
	}

	/** Het rad begint te draaien: niemand aan de beurt. */
	public void draai() {
		aanDeBeurt = null;
		reeks = 0;
	}

	/** Het rad is geland op dit team. */
	public void geland(Kleur k) {
		aanDeBeurt = k;
		reeks = 0;
	}

	/**
	 * R4.7: de presentator geeft een team zelf de beurt, zonder rad. Een ander team begint op reeks
	 * nul; het team dat al aan de beurt is houdt zijn reeks.
	 */
	public void geefBeurt(Kleur k) {
		if (k != aanDeBeurt) {
			aanDeBeurt = k;
			reeks = 0;
		}
	}

	/** @return false als er niemand aan de beurt is */
	public boolean goed() {
		if (aanDeBeurt == null) {
			return false;
		}
		punten.merge(aanDeBeurt, 1, Integer::sum);
		reeks++;
		return true;
	}

	/** @return false als er niemand aan de beurt is */
	public boolean fout() {
		if (aanDeBeurt == null) {
			return false;
		}
		aanDeBeurt = null;
		reeks = 0;
		return true;
	}

	/** {@code /quiz punt}: punten erbij of eraf, om een verkeerde klik recht te zetten. */
	public void punt(Kleur k, int aantal) {
		punten.merge(k, aantal, Integer::sum);
	}

	/** Het team of de teams met de meeste punten. Eén team: de winnaar; meer: gelijkspel. */
	public List<Kleur> leiders() {
		int max = Integer.MIN_VALUE;
		List<Kleur> uit = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			int p = punten.get(k);
			if (p > max) {
				max = p;
				uit.clear();
				uit.add(k);
			} else if (p == max) {
				uit.add(k);
			}
		}
		return uit;
	}

	/** De subtitle bij GOED!: {@code +1 Rood}, vanaf twee op rij {@code +1 Rood · 3 op rij}. */
	public String goedTekst() {
		String t = "+1 " + (aanDeBeurt == null ? "" : aanDeBeurt.naam());
		return reeks >= REEKS_VANAF ? t + " · " + reeks + " op rij" : t;
	}

	/** De hulpregel voor de presentator. */
	public String presentatorTekst() {
		if (aanDeBeurt == null) {
			return "Draai het rad";
		}
		return "Aan de beurt: " + aanDeBeurt.naam() + (reeks >= REEKS_VANAF ? " · " + reeks + " op rij" : "");
	}
}
