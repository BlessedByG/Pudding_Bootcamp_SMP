package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Het geheime schema van de mob arena. Aantal beurten = het grootste team. Per team een gelote
 * volgorde; met n beurten speelt in beurt b in arena 1 speler b en in arena 2 speler b + ⌊n/2⌋
 * (rondom). Zo speelt iedereen één keer in elke arena, en niemand twee beurten achter elkaar waar
 * het aantal spelers dat toelaat. Een kleiner team heeft plekken zonder speler: extra beurten, die
 * pas bij de start van die beurt gevuld worden.
 */
public final class MobSchema {
	public static final int ARENAS = 2;

	private final int beurten;
	private final Map<Kleur, List<UUID>> volgorde;

	private MobSchema(int beurten, Map<Kleur, List<UUID>> volgorde) {
		this.beurten = beurten;
		this.volgorde = volgorde;
	}

	/** Loot een schema. Teams zonder spelers doen niet mee. */
	public static MobSchema loot(Map<Kleur, List<UUID>> teams, RandomGenerator random) {
		Map<Kleur, List<UUID>> volgorde = new EnumMap<>(Kleur.class);
		int n = 0;
		for (Kleur k : Kleur.values()) {
			List<UUID> leden = new ArrayList<>(teams.getOrDefault(k, List.of()));
			schud(leden, random);
			volgorde.put(k, List.copyOf(leden));
			n = Math.max(n, leden.size());
		}
		return new MobSchema(n, volgorde);
	}

	/** Een schema met een vaste volgorde, voor tests. */
	public static MobSchema vast(Map<Kleur, List<UUID>> volgorde) {
		Map<Kleur, List<UUID>> v = new EnumMap<>(Kleur.class);
		int n = 0;
		for (Kleur k : Kleur.values()) {
			List<UUID> leden = List.copyOf(volgorde.getOrDefault(k, List.of()));
			v.put(k, leden);
			n = Math.max(n, leden.size());
		}
		return new MobSchema(n, v);
	}

	public int beurten() {
		return beurten;
	}

	public Map<Kleur, List<UUID>> volgorde() {
		return volgorde;
	}

	/** De verschuiving van arena 2: ⌊n/2⌋, minstens 1 als er meer dan één beurt is. */
	public int verschuiving() {
		return beurten <= 1 ? 0 : Math.max(1, beurten / 2);
	}

	/**
	 * Wie volgens het schema speelt in deze beurt (vanaf 0) en arena (1 of 2); {@code null} is een
	 * extra beurt: het team heeft daar geen speler.
	 */
	public UUID gepland(int beurt, int arena, Kleur kleur) {
		List<UUID> leden = volgorde.get(kleur);
		if (beurten == 0 || leden == null) {
			return null;
		}
		int index = arena == 1 ? beurt : Math.floorMod(beurt + verschuiving(), beurten);
		return index < leden.size() ? leden.get(index) : null;
	}

	/** Een plek in een beurt: welke arena, welk team, wie (of {@code null} = leeg). */
	public record Plek(int arena, Kleur kleur, UUID speler, boolean extra) {
	}

	/**
	 * De opstelling bij de start van een beurt. Wie gepland staat en nog mag, speelt. Een geplande
	 * speler die af is of er niet is laat zijn plek leeg. Een extra beurt krijgt een willekeurige
	 * speler van dat team die nog niet af is, er is, en niet al in deze beurt staat; is er niemand,
	 * dan blijft hij leeg.
	 *
	 * @param af      wie af is in deze mob arena
	 * @param aanwezig wie online is en meedoet
	 */
	public List<Plek> opstelling(int beurt, Set<UUID> af, Collection<UUID> aanwezig, RandomGenerator random) {
		List<Plek> uit = new ArrayList<>();
		Set<UUID> ingezet = new HashSet<>();
		// Eerst de geplande spelers, zodat een extra beurt nooit iemand pakt die al gepland staat.
		for (int arena = 1; arena <= ARENAS; arena++) {
			for (Kleur k : Kleur.values()) {
				UUID s = gepland(beurt, arena, k);
				if (s != null && !af.contains(s) && aanwezig.contains(s)) {
					ingezet.add(s);
				}
			}
		}
		for (int arena = 1; arena <= ARENAS; arena++) {
			for (Kleur k : Kleur.values()) {
				if (volgorde.get(k).isEmpty()) {
					continue;
				}
				UUID s = gepland(beurt, arena, k);
				if (s != null) {
					boolean mag = !af.contains(s) && aanwezig.contains(s);
					uit.add(new Plek(arena, k, mag ? s : null, false));
					continue;
				}
				List<UUID> kandidaten = new ArrayList<>();
				for (UUID lid : volgorde.get(k)) {
					if (!af.contains(lid) && aanwezig.contains(lid) && !ingezet.contains(lid)) {
						kandidaten.add(lid);
					}
				}
				UUID gekozen = kandidaten.isEmpty() ? null : kandidaten.get(random.nextInt(kandidaten.size()));
				if (gekozen != null) {
					ingezet.add(gekozen);
				}
				uit.add(new Plek(arena, k, gekozen, true));
			}
		}
		return uit;
	}

	private static <T> void schud(List<T> lijst, RandomGenerator random) {
		for (int i = lijst.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			T t = lijst.get(i);
			lijst.set(i, lijst.get(j));
			lijst.set(j, t);
		}
	}
}
