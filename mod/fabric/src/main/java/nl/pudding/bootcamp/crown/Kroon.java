package nl.pudding.bootcamp.crown;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.visuals.Zweefkroon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * De gedeelde kroon-primitieven: kroon geven en afnemen, Glowing, zweefkroon, de laatste hit
 * bijhouden en de locator bar. Wat een kroonwissel in ronde 5 betekent (reset, opstelling) staat in
 * de ronde zelf. Na de kroning houdt de King zijn kroon en zweefkroon tot {@code /bc reset}.
 */
public final class Kroon {
	/** Standaardbereik van een speler op de locator bar; 0 is onzichtbaar. */
	private static final double LOCATOR_AAN = 6.0E7;

	private static final Map<UUID, UUID> LAATSTE_HIT = new HashMap<>();
	/** De King of the SMP Bootcamp na de kroning, tot {@code /bc reset}. */
	private static UUID king;

	private Kroon() {
	}

	public static void init() {
		Reset.REGISTER.registreer("kronen", server -> {
			for (ServerPlayer s : Mc.spelers(server)) {
				haalKroonItemsWeg(s);
				toonOpLocator(s, true);
			}
			LAATSTE_HIT.clear();
			king = null;
		});
	}

	/**
	 * Maakt van deze speler de kroonhouder: de kroon als helm (zijn oude helm gaat naar zijn
	 * inventory), team {@code kroon}, Glowing, zweefkroon, zichtbaar op de locator bar.
	 */
	public static void geef(MinecraftServer server, ServerPlayer speler) {
		zetKroonOp(server, speler);
		Spel.zetRol(server, speler, Rol.KROON);
		Mc.effect(speler, MobEffects.GLOWING, -1, 0);
		toonOpLocator(speler, true);
		Zweefkroon.aan(speler);
		speler.inventoryMenu.broadcastChanges();
	}

	/** Alleen de kroon als helm; de oude helm gaat naar de inventory. */
	public static void zetKroonOp(MinecraftServer server, ServerPlayer speler) {
		ItemStack oud = speler.getItemBySlot(EquipmentSlot.HEAD);
		if (!Items26.isKroon(oud)) {
			speler.setItemSlot(EquipmentSlot.HEAD, Items26.kroon(server.registryAccess()));
			if (!oud.isEmpty()) {
				Kits.geefOfDrop(speler, oud);
			}
		}
	}

	/** Haalt de kroon weg. De rol verandert hier niet; dat doet wie dit aanroept. */
	public static void neemAf(ServerPlayer speler) {
		haalKroonItemsWeg(speler);
		speler.removeEffect(MobEffects.GLOWING);
		toonOpLocator(speler, false);
		Zweefkroon.uit(speler.getUUID());
		LAATSTE_HIT.remove(speler.getUUID());
	}

	public static boolean draagtKroon(ServerPlayer speler) {
		return Items26.isKroon(speler.getItemBySlot(EquipmentSlot.HEAD));
	}

	private static void haalKroonItemsWeg(ServerPlayer speler) {
		if (draagtKroon(speler)) {
			speler.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		}
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (Items26.isKroon(inv.getItem(i))) {
				inv.setItem(i, ItemStack.EMPTY);
			}
		}
		speler.inventoryMenu.broadcastChanges();
	}

	/** Alleen de kroonhouder zendt op de locator bar; jagers en kijkers niet. */
	public static void toonOpLocator(ServerPlayer speler, boolean zichtbaar) {
		Mc.zetAttribute(speler, Attributes.WAYPOINT_TRANSMIT_RANGE, zichtbaar ? LOCATOR_AAN : 0.0);
	}

	// De King na de kroning

	public static void zetKing(UUID id) {
		king = id;
	}

	public static UUID king() {
		return king;
	}

	public static boolean isKing(ServerPlayer speler) {
		return speler.getUUID().equals(king);
	}

	// Laatste hit

	public static void onthoudHit(ServerPlayer kroonhouder, ServerPlayer aanvaller) {
		LAATSTE_HIT.put(kroonhouder.getUUID(), aanvaller.getUUID());
	}

	/** Wie deze kroonhouder als laatste raakte, of {@code null}. */
	public static UUID laatsteHit(UUID kroonhouder) {
		return LAATSTE_HIT.get(kroonhouder);
	}

	public static void wisHits() {
		LAATSTE_HIT.clear();
	}
}
