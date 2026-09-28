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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Opstelling: spelers staan bevroren op hun startplek tot de countdown op nul staat. Bevriezen is
 * {@code MOVEMENT_SPEED} en {@code JUMP_STRENGTH} op 0, plus een blokkade op alles waarmee je
 * beweegt of schiet (bogen, crossbows, tridents, pearls, wind charges, chorus fruit) zolang de vlag
 * staat. Rondkijken, je inventory sorteren en eten kan gewoon.
 *
 * <p>Ronde 5 en 6 beginnen met een opstelling zonder countdown ({@link #wacht}): pas
 * {@code /clown go} of {@code /ffa go} start de tien seconden ({@link #go}).
 */
public final class Opstelling {
	private static final double LOOPSNELHEID = 0.1;
	private static final double SPRINGKRACHT = 0.42;

	private static boolean actief;
	private static boolean wachtOpGo;
	/** Wie in de lopende opstelling staat, ook als hij net is uitgelogd. */
	private static final Set<UUID> BEVROREN = new HashSet<>();

	private Opstelling() {
	}

	public static void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer speler && Spel.status(speler).bevroren) {
				ItemStack stack = player.getItemInHand(hand);
				if (geblokkeerd(stack)) {
					Mc.actionbar(speler, Mc.tekst("Nog even wachten", ChatFormatting.RED));
					// De client denkt dat hij gegooid of gespannen heeft; zet zijn inventory weer recht.
					speler.inventoryMenu.sendAllDataToRemote();
					return InteractionResult.FAIL;
				}
			}
			return InteractionResult.PASS;
		});
		Reset.REGISTER.registreer("bevriezing", server -> {
			actief = false;
			wachtOpGo = false;
			BEVROREN.clear();
			for (ServerPlayer s : Mc.spelers(server)) {
				ontdooi(s);
			}
		});
	}

	private static boolean geblokkeerd(ItemStack stack) {
		return stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT)
				|| stack.is(Items.ENDER_PEARL) || stack.is(Items.WIND_CHARGE) || stack.is(Items.CHORUS_FRUIT);
	}

	/**
	 * Bevriest deze spelers en telt meteen af; bij nul zijn ze los.
	 *
	 * @param bijNul wat er daarna gebeurt
	 */
	public static void start(MinecraftServer server, Collection<ServerPlayer> bevriezen, int seconden, String label, Runnable bijNul) {
		wacht(server, bevriezen);
		go(server, seconden, label, bijNul);
	}

	/** Bevriest deze spelers zonder countdown: ze wachten op {@link #go}. */
	public static void wacht(MinecraftServer server, Collection<ServerPlayer> bevriezen) {
		actief = true;
		wachtOpGo = true;
		// Een pearl die al onderweg is zou zijn eigenaar na de teleport alsnog van zijn startplek halen.
		List<Entity> pearls = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e instanceof ThrownEnderpearl) {
				pearls.add(e);
			}
		}
		pearls.forEach(Entity::discard);
		BEVROREN.clear();
		for (ServerPlayer s : bevriezen) {
			bevries(s);
			BEVROREN.add(s.getUUID());
		}
	}

	/** Start de countdown van een wachtende opstelling. */
	public static void go(MinecraftServer server, int seconden, String label, Runnable bijNul) {
		wachtOpGo = false;
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

	/** Staat er een opstelling klaar die op {@code /clown go} of {@code /ffa go} wacht? */
	public static boolean wachtOpGo() {
		return actief && wachtOpGo;
	}

	public static void losIedereen(MinecraftServer server) {
		actief = false;
		wachtOpGo = false;
		BEVROREN.clear();
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

	/**
	 * Wie inlogt en niet bevroren hoort te zijn, loopt weer normaal (attributes overleven een relog).
	 * Bevroren blijft alleen wie in een opstelling staat die nog loopt.
	 */
	public static void herstelBijJoin(ServerPlayer speler) {
		if (actief && BEVROREN.contains(speler.getUUID())) {
			bevries(speler);
		} else {
			ontdooi(speler);
		}
	}
}
