package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * R1.5: het teamoverzicht in de sidebar van het doolhof. Per kleur een kop en daaronder wie erin
 * zit. De sidebar laat er maar vijftien regels zien; past het niet met één naam per regel, dan
 * komen er twee, drie of meer naast elkaar, zo weinig als kan.
 */
public final class TeamOverzicht {
	/** Meer regels laat de sidebar van de client niet zien. */
	public static final int MAX_REGELS = 15;

	/** Een kopregel ({@code namen} leeg) of een regel met namen van die kleur. */
	public record Regel(Kleur kleur, boolean kop, List<String> namen) {
	}

	private TeamOverzicht() {
	}

	/** @param leden per kleur de namen, in de volgorde waarin ze moeten staan */
	public static List<Regel> regels(Map<Kleur, List<String>> leden) {
		int meeste = 1;
		for (Kleur k : Kleur.values()) {
			meeste = Math.max(meeste, leden.getOrDefault(k, List.of()).size());
		}
		int perRegel = 1;
		// Met evenveel naast elkaar als het grootste team is het hooguit acht regels: dat past altijd.
		while (perRegel < meeste && aantalRegels(leden, perRegel) > MAX_REGELS) {
			perRegel++;
		}
		List<Regel> uit = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			uit.add(new Regel(k, true, List.of()));
			List<String> namen = leden.getOrDefault(k, List.of());
			for (int i = 0; i < namen.size(); i += perRegel) {
				uit.add(new Regel(k, false, List.copyOf(namen.subList(i, Math.min(i + perRegel, namen.size())))));
			}
		}
		return uit;
	}

	private static int aantalRegels(Map<Kleur, List<String>> leden, int perRegel) {
		int n = 0;
		for (Kleur k : Kleur.values()) {
			int namen = leden.getOrDefault(k, List.of()).size();
			n += 1 + (namen + perRegel - 1) / perRegel;
		}
		return n;
	}
}
