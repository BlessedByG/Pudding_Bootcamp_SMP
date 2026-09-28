package nl.pudding.bootcamp.visuals;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.game.Reset;

import java.util.UUID;

/**
 * Eén bossbar voor iedereen: kort, altijd hetzelfde formaat. Na de kroning blijft
 * {@code Pudding Bootcamp · King: <naam>} staan tot {@code /bc reset}.
 */
public final class Bossbar {
	private static final ServerBossEvent BAR = new ServerBossEvent(
			UUID.nameUUIDFromBytes("bootcamp:bossbar".getBytes()),
			Mc.tekst(BossbarTekst.BASISKAMP), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

	private static String king;

	private Bossbar() {
	}

	public static void init() {
		Reset.REGISTER.registreer("bossbar", server -> {
			king = null;
			basiskamp();
		});
	}

	/** Tussen de rondes: {@code Pudding Bootcamp}, of na de kroning de King. */
	public static void basiskamp() {
		if (king != null) {
			BAR.setName(Mc.tekst(BossbarTekst.king(king), ChatFormatting.GOLD, ChatFormatting.BOLD));
			BAR.setColor(BossEvent.BossBarColor.YELLOW);
			BAR.setProgress(1f);
			return;
		}
		zet(BossbarTekst.BASISKAMP, BossEvent.BossBarColor.WHITE, 1f);
	}

	/** Na de kroning, tot {@code /bc reset}. */
	public static void king(String naam) {
		king = naam;
		basiskamp();
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
