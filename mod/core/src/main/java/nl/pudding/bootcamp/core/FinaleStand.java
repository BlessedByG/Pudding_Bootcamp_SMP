package nl.pudding.bootcamp.core;

import java.util.UUID;

/** R6.2: de finale is best of 3; wie er twee wint, wint. */
public final class FinaleStand {
	private final UUID een;
	private final UUID twee;
	private int winstEen;
	private int winstTwee;

	public FinaleStand(UUID een, UUID twee) {
		this.een = een;
		this.twee = twee;
	}

	/** Het potje gaat naar de tegenstander van de verliezer. */
	public void potjeVerlorenDoor(UUID verliezer) {
		if (winnaar() != null) {
			return;
		}
		if (verliezer.equals(een)) {
			winstTwee++;
		} else if (verliezer.equals(twee)) {
			winstEen++;
		}
	}

	/** De winnaar van de finale, of {@code null} zolang niemand twee potjes heeft. */
	public UUID winnaar() {
		if (winstEen >= Regels.FINALE_WINST) {
			return een;
		}
		if (winstTwee >= Regels.FINALE_WINST) {
			return twee;
		}
		return null;
	}

	public UUID tegenstander(UUID speler) {
		return speler.equals(een) ? twee : een;
	}

	public boolean isFinalist(UUID speler) {
		return speler.equals(een) || speler.equals(twee);
	}

	public UUID een() {
		return een;
	}

	public UUID twee() {
		return twee;
	}

	public int winstEen() {
		return winstEen;
	}

	public int winstTwee() {
		return winstTwee;
	}

	public int potje() {
		return winstEen + winstTwee + 1;
	}
}
