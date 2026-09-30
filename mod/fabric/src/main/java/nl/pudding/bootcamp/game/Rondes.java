package nl.pudding.bootcamp.game;

import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.ronde1.Doolhof;
import nl.pudding.bootcamp.game.ronde2.Ei;
import nl.pudding.bootcamp.game.ronde3.MobArena;
import nl.pudding.bootcamp.game.ronde4.Quiz;
import nl.pudding.bootcamp.game.ronde5.ClownVsAll;
import nl.pudding.bootcamp.game.ronde6.Ffa;
import nl.pudding.bootcamp.game.ronde7.Finale;

/** Welke klasse bij welke ronde hoort. */
public final class Rondes {
	private Rondes() {
	}

	public static RondeLogica maak(Ronde ronde) {
		return switch (ronde) {
			case DOOLHOF -> new Doolhof();
			case EI -> new Ei();
			case MOBARENA -> new MobArena();
			case QUIZ -> new Quiz();
			case CLOWN -> new ClownVsAll();
			case FFA -> new Ffa();
			case FINALE -> new Finale();
			case BASISKAMP -> throw new IllegalArgumentException("het basiskamp is geen ronde; gebruik /bc reset");
		};
	}
}
