package nl.pudding.bootcamp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

	// Titles, actionbar, geluid, particles

	/** Een title voor één speler. Tijden in ticks. */
	public static void title(ServerPlayer speler, Component titel, Component ondertitel, int in, int blijf, int uit) {
		speler.connection.send(new ClientboundSetTitlesAnimationPacket(in, blijf, uit));
		speler.connection.send(new ClientboundSetSubtitleTextPacket(ondertitel == null ? Component.empty() : ondertitel));
		speler.connection.send(new ClientboundSetTitleTextPacket(titel));
	}

	public static void title(ServerPlayer speler, Component titel, Component ondertitel) {
		title(speler, titel, ondertitel, 5, 50, 15);
	}

	public static void titleAllen(MinecraftServer server, Component titel, Component ondertitel) {
		for (ServerPlayer s : spelers(server)) {
			title(s, titel, ondertitel);
		}
	}

	public static void actionbar(ServerPlayer speler, Component tekst) {
		speler.connection.send(new ClientboundSetActionBarTextPacket(tekst));
	}

	/** Een geluid voor één speler, op zijn eigen positie: overal even hard. */
	public static void geluid(ServerPlayer speler, Holder<SoundEvent> geluid, float volume, float toonhoogte) {
		speler.connection.send(new ClientboundSoundPacket(geluid, SoundSource.MASTER,
				speler.getX(), speler.getY(), speler.getZ(), volume, toonhoogte, speler.getRandom().nextLong()));
	}

	public static void geluid(ServerPlayer speler, SoundEvent geluid, float volume, float toonhoogte) {
		geluid(speler, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(geluid), volume, toonhoogte);
	}

	public static void geluidAllen(MinecraftServer server, Holder<SoundEvent> geluid, float volume, float toonhoogte) {
		for (ServerPlayer s : spelers(server)) {
			geluid(s, geluid, volume, toonhoogte);
		}
	}

	public static void geluidAllen(MinecraftServer server, SoundEvent geluid, float volume, float toonhoogte) {
		geluidAllen(server, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(geluid), volume, toonhoogte);
	}

	/** Particles die iedereen ziet, ook van ver. */
	public static void particles(ServerLevel wereld, ParticleOptions soort, double x, double y, double z,
			int aantal, double spreiding, double snelheid) {
		wereld.sendParticles(soort, true, true, x, y, z, aantal, spreiding, spreiding, spreiding, snelheid);
	}

	// Attributes en effecten

	public static void zetAttribute(ServerPlayer speler, Holder<Attribute> attribute, double waarde) {
		AttributeInstance a = speler.getAttribute(attribute);
		if (a != null) {
			a.setBaseValue(waarde);
		}
	}

	/** Een effect zonder particles om de speler heen. */
	public static void effect(ServerPlayer speler, Holder<MobEffect> effect, int seconden, int niveau) {
		int ticks = seconden < 0 ? MobEffectInstance.INFINITE_DURATION : seconden * 20;
		speler.addEffect(new MobEffectInstance(effect, ticks, niveau, false, false));
	}
}
