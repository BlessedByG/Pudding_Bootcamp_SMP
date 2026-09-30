package nl.pudding.bootcamp.core;

/** De rol van een speler. De mod is de bron van waarheid; scoreboard-tags zijn spiegels. */
public enum Rol {
	/** Ronde 0 t/m 4: iedereen, geen PvP. */
	SPELER,
	/** Ronde 5: jaagt op de kroonhouder. */
	JAGER,
	/** Ronde 5: draagt de kroon. */
	KROON,
	/** Ronde 6: iedereen tegen iedereen; in de finale de twee finalisten. */
	FFA,
	/** Af: op de tribune of in de kooi, geen schade, inventory leeg. */
	KIJKER,
	/** Host, camera, admins (creative of spectator): de mod blijft van ze af. */
	STAFF;

	/** De scoreboard-tag die deze rol spiegelt, of {@code null} als er geen is. */
	public String tag() {
		return switch (this) {
			case KROON -> "kroon";
			case JAGER -> "jager";
			case FFA -> "ffa";
			case KIJKER -> "kijker";
			default -> null;
		};
	}

	/** Doet deze speler nog mee in de lopende ronde (geen kijker, geen staff)? */
	public boolean doetMee() {
		return this == SPELER || this == JAGER || this == KROON || this == FFA;
	}
}
