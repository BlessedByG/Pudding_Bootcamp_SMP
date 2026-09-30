package nl.pudding.bootcamp.game.ronde3;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;

/**
 * Het Warden-ei: de prijs voor de winnaar van het Ei. Een warden spawn egg met een eigen vlag; vanilla
 * spawnt er nooit een warden mee. Wie hem tijdens de mob arena vanaf de tribune inzet, maakt van de
 * volgende wave de warden ({@link MobArena#wardenInzetten}). Er bestaat er maar één.
 */
public final class WardenEi {
	/** De vlag op het ei, en de tag op de warden die eruit komt. */
	public static final String TAG = "bootcamp_warden";
	private static final String SPEC = "minecraft:warden_spawn_egg[minecraft:custom_data={" + TAG + ":1b},"
			+ "minecraft:item_name={\"text\":\"Warden-ei\",\"color\":\"dark_aqua\"},"
			+ "minecraft:lore=[{\"text\":\"Zet hem in tijdens de mob arena,\",\"color\":\"gray\",\"italic\":false},"
			+ "{\"text\":\"vanaf de tribune: de volgende wave is de warden\",\"color\":\"gray\",\"italic\":false}],"
			+ "minecraft:rarity=\"epic\"]";

	/** Zet de mod een sonic boom om naar de ingestelde schade, dan niet nog eens onderscheppen. */
	private static boolean boomBezig;

	private WardenEi() {
	}

	public static void init() {
		// Rechtsklik in de lucht, op een blok of op een entity: nooit de vanilla warden.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			return is(stack) && player instanceof ServerPlayer speler ? gebruik(speler, stack) : InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			return is(stack) && player instanceof ServerPlayer speler ? gebruik(speler, stack) : InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			return is(stack) && player instanceof ServerPlayer speler ? gebruik(speler, stack) : InteractionResult.PASS;
		});
		// De sonic boom doet vast 10 schade (door armor heen); die van onze warden de ingestelde schade.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, bron, schade) -> {
			if (boomBezig || !(entity instanceof ServerPlayer speler) || !bron.is(DamageTypes.SONIC_BOOM)
					|| !(bron.getEntity() instanceof Warden w) || !w.entityTags().contains(TAG)) {
				return true;
			}
			int boom = Spel.instellingen().warden(Instellingen.WardenWaarde.BOOM);
			if (boom > 0) {
				boomBezig = true;
				try {
					speler.hurtServer((ServerLevel) speler.level(), bron, boom);
				} finally {
					boomBezig = false;
				}
			}
			return false;
		});
	}

	public static boolean is(ItemStack stack) {
		return Items26.heeftTag(stack, TAG);
	}

	public static ItemStack maak(MinecraftServer server) {
		try {
			return Items26.parse(server.registryAccess(), SPEC, 1);
		} catch (CommandSyntaxException e) {
			throw new IllegalStateException("Het Warden-ei is niet te maken: " + e.getMessage(), e);
		}
	}

	public static boolean heeft(ServerPlayer speler) {
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (is(inv.getItem(i))) {
				return true;
			}
		}
		return is(speler.containerMenu.getCarried());
	}

	public static void geef(MinecraftServer server, ServerPlayer speler) {
		Kits.geefOfDrop(speler, maak(server));
		speler.inventoryMenu.broadcastChanges();
	}

	/**
	 * Inzetten mag alleen tijdens de mob arena, als deelnemer op de tribune: niet wie zelf aan de
	 * beurt is, niet vanuit de kooi, niet als staff. Lukt het niet, dan houdt hij het ei.
	 */
	private static InteractionResult gebruik(ServerPlayer speler, ItemStack stack) {
		MinecraftServer server = speler.level().getServer();
		String fout = waaromNiet(speler);
		if (fout == null && Spel.actief() instanceof MobArena ronde) {
			fout = ronde.wardenInzetten(server, speler);
			if (fout == null) {
				stack.shrink(1);
				Bootcamp.LOG.info("Mob arena: {} zette het Warden-ei in", Mc.naam(speler));
			}
		}
		if (fout != null) {
			Mc.actionbar(speler, Mc.tekst(fout, ChatFormatting.RED));
		}
		// De client denkt misschien dat er een warden spawnde of dat het ei weg is: zet hem recht.
		speler.inventoryMenu.sendAllDataToRemote();
		return InteractionResult.FAIL;
	}

	private static String waaromNiet(ServerPlayer speler) {
		if (!(Spel.actief() instanceof MobArena)) {
			return "Bewaar het Warden-ei voor de mob arena";
		}
		if (Mc.isStaff(speler)) {
			return "Alleen een speler op de tribune kan het Warden-ei inzetten";
		}
		SpelerStatus st = Spel.status(speler);
		if (st.arena > 0) {
			return "Je bent zelf aan de beurt: zet hem in vanaf de tribune";
		}
		if (st.kooi > 0) {
			return "Niet vanuit de kooi: wacht tot je op de tribune staat";
		}
		if (!Tribune.opMobTribune(speler.getX(), speler.getY(), speler.getZ())) {
			return "Ga op de tribune staan om het Warden-ei in te zetten";
		}
		return null;
	}
}
