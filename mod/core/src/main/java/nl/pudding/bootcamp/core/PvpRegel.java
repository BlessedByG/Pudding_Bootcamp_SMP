package nl.pudding.bootcamp.core;

/**
 * Mag een speler een andere speler raken? Eén regel voor de hele avond, niet via teams (R0.10).
 * De bron is de speler, ook via een pijl of een andere projectile.
 *
 * <table>
 * <tr><td>0 t/m 4</td><td>nooit (de mob arena is PvE)</td></tr>
 * <tr><td>5 Clown vs All</td><td>alleen als aanvaller of slachtoffer de kroonhouder is</td></tr>
 * <tr><td>6 FFA</td><td>altijd</td></tr>
 * <tr><td>7 finale</td><td>altijd (alleen de twee finalisten doen mee)</td></tr>
 * <tr><td>opstelling of countdown</td><td>nooit</td></tr>
 * </table>
 */
public final class PvpRegel {
	private PvpRegel() {
	}

	/**
	 * @param ronde           de lopende ronde ({@link Ronde#BASISKAMP} tussen twee rondes)
	 * @param stil            er loopt een opstelling of countdown
	 * @param aanvaller       de rol van de aanvaller
	 * @param slachtoffer     de rol van het slachtoffer
	 */
	public static boolean mag(Ronde ronde, boolean stil, Rol aanvaller, Rol slachtoffer) {
		if (stil || !aanvaller.doetMee() || !slachtoffer.doetMee()) {
			return false;
		}
		return switch (ronde) {
			case CLOWN -> aanvaller == Rol.KROON || slachtoffer == Rol.KROON;
			case FFA, FINALE -> true;
			default -> false;
		};
	}
}
