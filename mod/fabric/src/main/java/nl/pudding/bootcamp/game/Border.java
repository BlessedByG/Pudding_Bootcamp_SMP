package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Regio;

/** De worldborder per ronde. Altijd eerst teleporteren, dan de border zetten. */
public final class Border {
	private Border() {
	}

	public static void init() {
		Reset.REGISTER.registreer("border", Border::weg);
	}

	/** Om een regio: center uit de regio, grootte is de langste zijde. */
	public static void zet(MinecraftServer server, Regio regio) {
		zet(server, regio, regio.grootte());
	}

	/** Om het midden van een regio, met een eigen grootte (de finale: 20). */
	public static void zet(MinecraftServer server, Regio regio, double grootte) {
		WorldBorder border = Mc.wereld(server).getWorldBorder();
		border.setCenter(regio.centerX(), regio.centerZ());
		border.setSize(grootte);
		// Geen rood scherm voor wie dicht bij de rand staat; de schade blijft.
		border.setWarningBlocks(0);
		border.setWarningTime(0);
	}

	/** Krimpt vanaf de huidige grootte. */
	public static void krimp(MinecraftServer server, double naar, int seconden) {
		ServerLevel wereld = Mc.wereld(server);
		WorldBorder border = wereld.getWorldBorder();
		border.lerpSizeBetween(border.getSize(), naar, seconden * 20L, wereld.getGameTime());
	}

	public static void weg(MinecraftServer server) {
		WorldBorder border = Mc.wereld(server).getWorldBorder();
		border.setCenter(0, 0);
		border.setSize(WorldBorder.MAX_SIZE);
	}
}
