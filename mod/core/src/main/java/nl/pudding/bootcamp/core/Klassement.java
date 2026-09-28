package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Een stand per speler: de punten van het Ei, de kills van de FFA. De volgorde is die van de mod,
 * ook bij een gelijke score: wie die score het eerst had, staat hoger. Wie nog 0 heeft, staat er
 * niet in.
 */
public final class Klassement {
	public record Regel(UUID speler, int score, long sinds) {
	}

	private final Map<UUID, Regel> regels = new HashMap<>();
	private long teller;

	/** Telt er {@code erbij} bij op en geeft de nieuwe score terug. */
	public int voegToe(UUID speler, int erbij) {
		Regel oud = regels.get(speler);
		int score = (oud == null ? 0 : oud.score()) + erbij;
		long sinds = erbij != 0 || oud == null ? ++teller : oud.sinds();
		regels.put(speler, new Regel(speler, score, sinds));
		return score;
	}

	public int score(UUID speler) {
		Regel r = regels.get(speler);
		return r == null ? 0 : r.score();
	}

	/** Iedereen met meer dan 0, de beste eerst. */
	public List<Regel> volgorde() {
		List<Regel> uit = new ArrayList<>();
		for (Regel r : regels.values()) {
			if (r.score() > 0) {
				uit.add(r);
			}
		}
		uit.sort(Comparator.comparingInt(Regel::score).reversed().thenComparingLong(Regel::sinds));
		return uit;
	}

	public List<Regel> top(int n) {
		List<Regel> v = volgorde();
		return v.subList(0, Math.min(n, v.size()));
	}

	/** De plek in de volgorde vanaf 1, of 0 met een score van 0. */
	public int plek(UUID speler) {
		List<Regel> v = volgorde();
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i).speler().equals(speler)) {
				return i + 1;
			}
		}
		return 0;
	}

	public Optional<Regel> winnaar() {
		List<Regel> v = volgorde();
		return v.isEmpty() ? Optional.empty() : Optional.of(v.get(0));
	}

	public void wis() {
		regels.clear();
		teller = 0;
	}
}
