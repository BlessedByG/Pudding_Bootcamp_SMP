package nl.pudding.bootcamp.core;

/** De rol van een speler. De mod is de bron van waarheid; scoreboard-tags zijn spiegels. */
public enum Rol {
	/** Ronde 0 t/m 3: iedereen, geen PvP. */
	SPELER,
	/** Ronde 4: jaagt op de koning. */
	HUNTER,
	/** Draagt de kroon (ronde 4), of is finalist in de finale. */
	KING,
	/** Ronde 5: iedereen tegen iedereen. */
	FFA,
	/** Heeft een kroon en wacht op de finale. */
	FINALIST,
	/** Dood of klaar: op de tribune of bij het verzamelpunt, geen schade. */
	KIJKER,
	/** Host, camera, admins: de mod blijft van ze af. */
	STAFF;

	/** De scoreboard-tag die deze rol spiegelt, of {@code null} als er geen is. */
	public String tag() {
		return switch (this) {
			case KING, FINALIST -> "king";
			case HUNTER -> "hunter";
			case KIJKER -> "kijker";
			default -> null;
		};
	}
}
