package nl.pudding.bootcamp.core;

import java.util.ArrayList;
import java.util.List;

/**
 * De regeerperiodes van ronde 4 voor de sidebar: {@code Clown 4:12 · Speler X 0:38}. Elke
 * kroonwissel begint een nieuwe periode; dezelfde speler kan er dus meerdere hebben.
 */
public final class Regeerperiodes {
	public record Periode(String naam, int seconden) {
		public String tekst() {
			return naam + " " + Tijd.mss(seconden);
		}
	}

	private final List<Periode> periodes = new ArrayList<>();

	public void nieuweKoning(String naam) {
		periodes.add(new Periode(naam, 0));
	}

	/** Eén seconde erbij voor de huidige koning. */
	public void seconde() {
		if (!periodes.isEmpty()) {
			int i = periodes.size() - 1;
			Periode p = periodes.get(i);
			periodes.set(i, new Periode(p.naam(), p.seconden() + 1));
		}
	}

	/** In volgorde van regeren, de huidige koning als laatste. */
	public List<Periode> periodes() {
		return List.copyOf(periodes);
	}

	/** De langste periode, voor de eretitel achteraf; {@code null} als er nog geen koning was. */
	public Periode langste() {
		Periode beste = null;
		for (Periode p : periodes) {
			if (beste == null || p.seconden() > beste.seconden()) {
				beste = p;
			}
		}
		return beste;
	}

	public void wis() {
		periodes.clear();
	}
}
