package nl.pudding.bootcamp;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.pudding.bootcamp.core.Punt;

import java.util.List;
import java.util.Set;

/**
 * De dunne adapterlaag naar Minecraft. Alles wat een 26.2-naam nodig heeft en op meer dan één
 * plek gebruikt wordt, staat hier, zodat een naam die verandert op één plek gefixt wordt.
 */
public final class Mc {
	private Mc() {
	}

	// Tekst

	public static MutableComponent tekst(String s) {
		return Component.literal(s);
	}

	public static MutableComponent tekst(String s, ChatFormatting... stijl) {
		return Component.literal(s).withStyle(stijl);
	}

	// Spelers

	public static List<ServerPlayer> spelers(MinecraftServer server) {
		return server.getPlayerList().getPlayers();
	}

	public static String naam(ServerPlayer speler) {
		return speler.getGameProfile().name();
	}

	/** De mod speelt zich af in de overworld. */
	public static ServerLevel wereld(MinecraftServer server) {
		return server.overworld();
	}

	/**
	 * Staff (host, camera, admins) staat in creative of spectator; de mod blijft van ze af. Alle
	 * anderen doen mee: de mod zet hen zelf in adventure of survival.
	 */
	public static boolean isStaff(ServerPlayer speler) {
		GameType type = speler.gameMode();
		return type == GameType.CREATIVE || type == GameType.SPECTATOR;
	}

	public static List<ServerPlayer> deelnemers(MinecraftServer server) {
		return spelers(server).stream().filter(s -> !isStaff(s)).toList();
	}

	// Teleport, heal

	/** Naar een punt uit de config. Een blok-punt zet je midden bovenop het blok. */
	public static void teleport(ServerPlayer speler, Punt p) {
		ServerLevel wereld = wereld(speler.level().getServer());
		if (p.blok()) {
			speler.teleportTo(wereld, p.x() + 0.5, p.y() + 1.0, p.z() + 0.5, Set.of(), speler.getYRot(), speler.getXRot(), true);
		} else {
			speler.teleportTo(wereld, p.x(), p.y(), p.z(), Set.of(), p.yaw(), p.pitch(), true);
		}
		speler.setDeltaMovement(Vec3.ZERO);
		speler.fallDistance = 0;
	}

	/** Full hp, honger vol, niet meer in brand. */
	public static void heal(ServerPlayer speler) {
		speler.setHealth(speler.getMaxHealth());
		speler.getFoodData().setFoodLevel(20);
		speler.getFoodData().setSaturation(5f);
		speler.clearFire();
	}
}
