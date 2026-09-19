package nl.pudding.bootcamp.game;

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;

/**
 * De worldborder per ronde. Altijd eerst teleporteren, dan de border zetten.
 *
 * <p>Kijkers staan vaak buiten de border (de tribune tijdens de finale, {@code v2} tijdens het
 * doolhof). Schade krijgen ze daar niet, maar de client kleurt het hele scherm rood voor wie
 * buiten de border staat, en dat wil je niet op zeventien streams tegelijk. Daarom krijgt een
 * kijker een eigen border-pakket met een border zo groot als de wereld. De server stuurt bij elke
 * wijziging de echte border naar iedereen; alle wijzigingen lopen via deze klasse, dus direct
 * daarna gaat het eigen pakket er weer achteraan.
 */
public final class Border {
	private static final WorldBorder ONZICHTBAAR = new WorldBorder();

	private Border() {
	}

	/** Deze speler ziet geen border (kijkers en wachtende finalisten). */
	public static void verberg(ServerPlayer speler) {
		speler.connection.send(new ClientboundInitializeBorderPacket(ONZICHTBAAR));
	}

	/** Deze speler ziet de echte border weer. */
	public static void toonEcht(ServerPlayer speler) {
		speler.connection.send(new ClientboundInitializeBorderPacket(Mc.wereld(speler.level().getServer()).getWorldBorder()));
	}

	private static void naWijziging(MinecraftServer server) {
		for (ServerPlayer s : Mc.spelers(server)) {
			Rol rol = Spel.rol(s);
			if (rol == Rol.KIJKER || rol == Rol.FINALIST) {
				verberg(s);
			}
		}
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
		naWijziging(server);
	}

	/** Krimpt vanaf de huidige grootte. */
	public static void krimp(MinecraftServer server, double naar, int seconden) {
		ServerLevel wereld = Mc.wereld(server);
		WorldBorder border = wereld.getWorldBorder();
		border.lerpSizeBetween(border.getSize(), naar, seconden * 20L, wereld.getGameTime());
		naWijziging(server);
	}

	public static void weg(MinecraftServer server) {
		WorldBorder border = Mc.wereld(server).getWorldBorder();
		border.setCenter(0, 0);
		border.setSize(WorldBorder.MAX_SIZE);
		naWijziging(server);
	}
}
