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
 * De gedeelde kroon-primitieven: kroon geven en afnemen, Glowing, zweefkroon, finalist markeren,
 * de laatste hit bijhouden en de locator bar. Wat een kroonwissel in ronde 4 betekent (reset,
 * opstelling) staat in de ronde zelf.
 */
public final class Kroon {
	/** Standaardbereik van een speler op de locator bar; 0 is onzichtbaar. */
	private static final double LOCATOR_AAN = 6.0E7;

	private static final Map<UUID, UUID> LAATSTE_HIT = new HashMap<>();

	private Kroon() {
	}

	public static void init() {
		Reset.REGISTER.registreer("kronen", server -> {
			for (ServerPlayer s : Mc.spelers(server)) {
				haalKroonItemsWeg(s);
				toonOpLocator(s, true);
			}
			LAATSTE_HIT.clear();
		});
	}

	/**
	 * Maakt van deze speler een koning: de kroon als helm (zijn oude helm gaat naar zijn
	 * inventory), team {@code king}, Glowing, zweefkroon, zichtbaar op de locator bar.
	 */
	public static void geef(MinecraftServer server, ServerPlayer speler) {
		ItemStack oud = speler.getItemBySlot(EquipmentSlot.HEAD);
		if (!Items26.isKroon(oud)) {
			speler.setItemSlot(EquipmentSlot.HEAD, Items26.kroon(server.registryAccess()));
			if (!oud.isEmpty()) {
				Kits.geefOfDrop(speler, oud);
			}
		}
		Spel.zetRol(server, speler, Rol.KING);
		Mc.effect(speler, MobEffects.GLOWING, -1, 0);
		toonOpLocator(speler, true);
		Zweefkroon.aan(speler);
		speler.inventoryMenu.broadcastChanges();
	}

	/** Haalt de kroon weg. De rol verandert hier niet; dat doet wie dit aanroept. */
	public static void neemAf(ServerPlayer speler) {
		haalKroonItemsWeg(speler);
		speler.removeEffect(MobEffects.GLOWING);
		toonOpLocator(speler, false);
		Zweefkroon.uit(speler.getUUID());
		LAATSTE_HIT.remove(speler.getUUID());
	}

	/**
	 * Finalist: houdt zijn kroon en zijn zweefkroon, maar wacht op de tribune. Geen Glowing en
	 * niet op de locator bar tot de finale begint.
	 */
	public static void maakFinalist(MinecraftServer server, ServerPlayer speler) {
		if (!draagtKroon(speler)) {
			geef(server, speler);
		}
		Spel.zetRol(server, speler, Rol.FINALIST);
		speler.removeEffect(MobEffects.GLOWING);
		toonOpLocator(speler, false);
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

	/** Alleen koningen zenden op de locator bar (ronde 4 t/m 6); hunters en kijkers niet. */
	public static void toonOpLocator(ServerPlayer speler, boolean zichtbaar) {
		Mc.zetAttribute(speler, Attributes.WAYPOINT_TRANSMIT_RANGE, zichtbaar ? LOCATOR_AAN : 0.0);
	}

	// Laatste hit

	public static void onthoudHit(ServerPlayer koning, ServerPlayer aanvaller) {
		LAATSTE_HIT.put(koning.getUUID(), aanvaller.getUUID());
	}

	/** Wie deze koning als laatste raakte, of {@code null}. */
	public static UUID laatsteHit(ServerPlayer koning) {
		return LAATSTE_HIT.get(koning.getUUID());
	}

	public static void wisHits() {
		LAATSTE_HIT.clear();
	}
}
