package nl.pudding.bootcamp.game;

import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.ronde1.Doolhof;
import nl.pudding.bootcamp.game.ronde2.Horde;
import nl.pudding.bootcamp.game.ronde3.Ei;
import nl.pudding.bootcamp.game.ronde4.King;

/** Welke klasse bij welke ronde hoort. Elke ronde zet hier haar eigen regel in. */
public final class Rondes {
	private Rondes() {
	}

	public static RondeLogica maak(Ronde ronde) {
		return switch (ronde) {
			case DOOLHOF -> new Doolhof();
			case HORDE -> new Horde();
			case EI -> new Ei();
			case KING -> new King();
			case FFA -> new LegeRonde(Ronde.FFA, "hunter_1", "vloer", null);
			case FINALE -> new LegeRonde(Ronde.FINALE, "finale_1", "finale", null);
			case BASISKAMP -> throw new IllegalArgumentException("het basiskamp is geen ronde; gebruik /bc reset");
		};
	}
}
