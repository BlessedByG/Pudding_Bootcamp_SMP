package nl.pudding.bootcamp.crown;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Opstelling: spelers staan bevroren op hun startpunt tot de countdown op nul staat. Bevriezen is
 * {@code MOVEMENT_SPEED} en {@code JUMP_STRENGTH} op 0, plus een blokkade op pearls, wind charges
 * en chorus fruit zolang de vlag staat. Rondkijken en je inventory sorteren kan gewoon.
 */
public final class Opstelling {
	private static final double LOOPSNELHEID = 0.1;
	private static final double SPRINGKRACHT = 0.42;

	private static boolean actief;

	private Opstelling() {
	}

	public static void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer speler && Spel.status(speler).bevroren) {
				ItemStack stack = player.getItemInHand(hand);
				if (stack.is(Items.ENDER_PEARL) || stack.is(Items.WIND_CHARGE) || stack.is(Items.CHORUS_FRUIT)) {
					Mc.actionbar(speler, Mc.tekst("Nog even wachten", ChatFormatting.RED));
					// De client denkt dat hij gegooid heeft; zet zijn inventory weer recht.
					speler.inventoryMenu.sendAllDataToRemote();
					return InteractionResult.FAIL;
				}
			}
			return InteractionResult.PASS;
		});
		Reset.REGISTER.registreer("bevriezing", server -> {
			actief = false;
			for (ServerPlayer s : Mc.spelers(server)) {
				ontdooi(s);
			}
		});
	}

	/**
	 * Bevriest deze spelers en telt af voor iedereen; bij nul zijn ze los.
	 *
	 * @param bijNul wat er daarna gebeurt (de timer starten, bijvoorbeeld)
	 */
	public static void start(MinecraftServer server, Collection<ServerPlayer> bevriezen, int seconden, String label, Runnable bijNul) {
		actief = true;
		// Een pearl die al onderweg is zou zijn eigenaar na de teleport alsnog van zijn startpunt halen.
		List<Entity> pearls = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e instanceof ThrownEnderpearl) {
				pearls.add(e);
			}
		}
		pearls.forEach(Entity::discard);
		for (ServerPlayer s : bevriezen) {
			bevries(s);
		}
		Aftelling.start(seconden, label, () -> {
			losIedereen(server);
			if (bijNul != null) {
				bijNul.run();
			}
		});
	}

	/** Zolang een opstelling loopt doet niemand elkaar schade. */
	public static boolean actief() {
		return actief;
	}

	public static void losIedereen(MinecraftServer server) {
		actief = false;
		for (ServerPlayer s : Mc.spelers(server)) {
			if (Spel.status(s).bevroren) {
				ontdooi(s);
			}
		}
	}

	public static void bevries(ServerPlayer speler) {
		Spel.status(speler).bevroren = true;
		Mc.zetAttribute(speler, Attributes.MOVEMENT_SPEED, 0.0);
		Mc.zetAttribute(speler, Attributes.JUMP_STRENGTH, 0.0);
	}

	public static void ontdooi(ServerPlayer speler) {
		SpelerStatus status = Spel.status(speler);
		status.bevroren = false;
		Mc.zetAttribute(speler, Attributes.MOVEMENT_SPEED, LOOPSNELHEID);
		Mc.zetAttribute(speler, Attributes.JUMP_STRENGTH, SPRINGKRACHT);
	}

	/** Wie inlogt en niet bevroren hoort te zijn, loopt weer normaal (attributes overleven een relog). */
	public static void herstelBijJoin(ServerPlayer speler) {
		if (Spel.status(speler).bevroren && actief) {
			bevries(speler);
		} else {
			ontdooi(speler);
		}
	}
}
