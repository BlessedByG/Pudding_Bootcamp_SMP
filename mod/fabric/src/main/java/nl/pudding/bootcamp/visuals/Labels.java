package nl.pudding.bootcamp.visuals;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import nl.pudding.bootcamp.Mc;

/**
 * Labels: een zwevende tekst boven een verzamelpunt of boven De Kring. Eén keer plaatsen bij het
 * bouwen; ze horen bij de wereld en blijven dus staan bij {@code /bc reset}.
 */
public final class Labels {
	private static final String TAG = "bootcamp_label";
	private static final double HOOGTE = 2.6;
	private static final double ZOEKAFSTAND = 6.0;

	private Labels() {
	}

	/** Plaatst een label boven de positie van de speler. */
	public static void plaats(ServerPlayer speler, String tekst) {
		ServerLevel wereld = Mc.wereld(speler.level().getServer());
		Display.TextDisplay label = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, wereld);
		label.setText(Mc.tekst(tekst, ChatFormatting.GOLD, ChatFormatting.BOLD));
		// Draait mee met wie ernaar kijkt, zodat hij van alle kanten leesbaar is.
		label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		label.setViewRange(2f);
		label.setPos(speler.getX(), speler.getY() + HOOGTE, speler.getZ());
		label.addTag(TAG);
		wereld.addFreshEntity(label);
	}

	/**
	 * Haalt het dichtstbijzijnde label weg.
	 *
	 * @return {@code true} als er een label binnen zes blokken stond
	 */
	public static boolean verwijderDichtstbij(ServerPlayer speler) {
		Entity dichtstbij = null;
		double beste = ZOEKAFSTAND * ZOEKAFSTAND;
		for (Entity e : Mc.wereld(speler.level().getServer()).getAllEntities()) {
			if (e.entityTags().contains(TAG)) {
				double afstand = e.distanceToSqr(speler);
				if (afstand < beste) {
					beste = afstand;
					dichtstbij = e;
				}
			}
		}
		if (dichtstbij == null) {
			return false;
		}
		dichtstbij.discard();
		return true;
	}
}
