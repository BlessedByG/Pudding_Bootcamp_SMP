package nl.pudding.bootcamp.game;

import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.ronde1.Doolhof;
import nl.pudding.bootcamp.game.ronde2.Horde;
import nl.pudding.bootcamp.game.ronde3.Ei;
import nl.pudding.bootcamp.game.ronde4.King;
import nl.pudding.bootcamp.game.ronde5.Ffa;
import nl.pudding.bootcamp.game.ronde6.Finale;

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
			case FFA -> new Ffa();
			case FINALE -> new Finale();
			case BASISKAMP -> throw new IllegalArgumentException("het basiskamp is geen ronde; gebruik /bc reset");
		};
	}
}
