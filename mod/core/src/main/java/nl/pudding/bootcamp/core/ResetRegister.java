package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Het register achter {@code /bc reset}: elk onderdeel zet er zijn eigen opruimstap in. Reset
 * weigert niks en ruimt alles op: een stap die faalt houdt de rest niet tegen.
 *
 * @param <C> wat een stap meekrijgt (in de mod: de server)
 */
public final class ResetRegister<C> {
	@FunctionalInterface
	public interface Stap<C> {
		void ruimOp(C context) throws Exception;
	}

	public record Fout(String stap, Exception oorzaak) {
	}

	private final Map<String, Stap<C>> stappen = new LinkedHashMap<>();

	/** Registreert een stap. Dezelfde naam nog een keer vervangt de oude, op dezelfde plek. */
	public void registreer(String naam, Stap<C> stap) {
		stappen.put(naam, stap);
	}

	/** Draait alle stappen in volgorde van registratie en geeft terug wat er misging. */
	public List<Fout> draai(C context) {
		List<Fout> fouten = new ArrayList<>();
		for (Map.Entry<String, Stap<C>> e : stappen.entrySet()) {
			try {
				e.getValue().ruimOp(context);
			} catch (Exception ex) {
				fouten.add(new Fout(e.getKey(), ex));
			}
		}
		return fouten;
	}

	public List<String> namen() {
		return List.copyOf(stappen.keySet());
	}
}
