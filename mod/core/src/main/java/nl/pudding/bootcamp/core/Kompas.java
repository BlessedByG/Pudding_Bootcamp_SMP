package nl.pudding.bootcamp.core;

/** Windrichtingen in Minecraft: noord is -z, oost is +x. */
public final class Kompas {
	private static final String[] NAMEN = {"noord", "noordoost", "oost", "zuidoost", "zuid", "zuidwest", "west", "noordwest"};

	private Kompas() {
	}

	/** De windrichting van een verplaatsing (dx, dz), in acht streken. */
	public static String richting(double dx, double dz) {
		// 0 graden is noord, met de klok mee.
		double graden = Math.toDegrees(Math.atan2(dx, -dz));
		int streek = Math.floorMod((int) Math.round(graden / 45.0), 8);
		return NAMEN[streek];
	}
}
