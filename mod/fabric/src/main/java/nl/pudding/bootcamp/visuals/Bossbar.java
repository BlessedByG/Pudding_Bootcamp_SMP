package nl.pudding.bootcamp.visuals;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.game.Reset;

import java.util.UUID;

/** Eén bossbar voor iedereen: kort, altijd hetzelfde formaat. */
public final class Bossbar {
	private static final ServerBossEvent BAR = new ServerBossEvent(
			UUID.nameUUIDFromBytes("bootcamp:bossbar".getBytes()),
			Mc.tekst(BossbarTekst.BASISKAMP), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

	private Bossbar() {
	}

	public static void init() {
		Reset.REGISTER.registreer("bossbar", server -> basiskamp());
	}

	public static void basiskamp() {
		zet(BossbarTekst.BASISKAMP, BossEvent.BossBarColor.WHITE, 1f);
	}

	public static void zet(String tekst, BossEvent.BossBarColor kleur, float vulling) {
		BAR.setName(Mc.tekst(tekst));
		BAR.setColor(kleur);
		BAR.setProgress(Mth.clamp(vulling, 0f, 1f));
	}

	/** Elke seconde: wie nieuw is ziet de bar ook. */
	public static void iedereenErbij(MinecraftServer server) {
		for (ServerPlayer s : Mc.spelers(server)) {
			BAR.addPlayer(s);
		}
	}

	public static void verwijder(ServerPlayer speler) {
		BAR.removePlayer(speler);
	}
}
