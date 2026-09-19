package nl.pudding.bootcamp.game;

import nl.pudding.bootcamp.core.Ronde;

/** Welke klasse bij welke ronde hoort. Elke ronde zet hier haar eigen regel in. */
public final class Rondes {
	private Rondes() {
	}

	public static RondeLogica maak(Ronde ronde) {
		return switch (ronde) {
			case DOOLHOF -> new LegeRonde(Ronde.DOOLHOF, "doolhof_start", "doolhof", "v2");
			case HORDE -> new LegeRonde(Ronde.HORDE, "arena_spawn", "arena", "v3");
			case EI -> new LegeRonde(Ronde.EI, "ei_start", "eibos", "kring");
			case KING -> new LegeRonde(Ronde.KING, "troon", "vloer", null);
			case FFA -> new LegeRonde(Ronde.FFA, "hunter_1", "vloer", null);
			case FINALE -> new LegeRonde(Ronde.FINALE, "finale_1", "finale", null);
			case BASISKAMP -> throw new IllegalArgumentException("het basiskamp is geen ronde; gebruik /bc reset");
		};
	}
}
