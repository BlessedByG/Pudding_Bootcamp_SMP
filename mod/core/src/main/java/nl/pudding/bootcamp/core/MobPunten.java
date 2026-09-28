package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * De teamstand van de mob arena: punten per kill uit {@code /mobarena punten}, plus een kill voor
 * de tiebreak. Winnaar: de meeste punten; gelijk, dan de meeste kills; ook gelijk, dan samen.
 */
public final class MobPunten {
	private final Map<Kleur, Integer> punten = new EnumMap<>(Kleur.class);
	private final Map<Kleur, Integer> kills = new EnumMap<>(Kleur.class);

	public MobPunten() {
		for (Kleur k : Kleur.values()) {
			punten.put(k, 0);
			kills.put(k, 0);
		}
	}

	/** Een kill voor dit team; geeft de punten terug die het opleverde. */
	public int kill(Kleur team, String mobType, Instellingen instellingen) {
		int p = instellingen.mobPunten(mobType);
		punten.merge(team, p, Integer::sum);
		kills.merge(team, 1, Integer::sum);
		return p;
	}

	public int punten(Kleur team) {
		return punten.get(team);
	}

	public int kills(Kleur team) {
		return kills.get(team);
	}

	/** De teams op volgorde: punten, dan kills. */
	public List<Kleur> volgorde() {
		List<Kleur> uit = new ArrayList<>(List.of(Kleur.values()));
		uit.sort((a, b) -> {
			int c = Integer.compare(punten.get(b), punten.get(a));
			return c != 0 ? c : Integer.compare(kills.get(b), kills.get(a));
		});
		return uit;
	}

	/** De winnaar, of meerdere als ze op punten én kills gelijk staan. */
	public List<Kleur> winnaars(List<Kleur> meedoend) {
		List<Kleur> uit = new ArrayList<>();
		for (Kleur k : volgorde()) {
			if (!meedoend.contains(k)) {
				continue;
			}
			if (uit.isEmpty()) {
				uit.add(k);
			} else {
				Kleur eerste = uit.get(0);
				if (punten.get(k).equals(punten.get(eerste)) && kills.get(k).equals(kills.get(eerste))) {
					uit.add(k);
				}
			}
		}
		return uit;
	}

	public void wis() {
		for (Kleur k : Kleur.values()) {
			punten.put(k, 0);
			kills.put(k, 0);
		}
	}
}
