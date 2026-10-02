package nl.pudding.bootcamp.game.ronde2;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.teams.Teams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Een kistmenu met de koppen van de andere spelers, met hun naam in de teamkleur: klik er een aan.
 * Voor de emerald (wie laat je schrikken) en de lapis (wie stuur je terug naar de start) in het Ei.
 * Items verplaatsen kan niet. Dicht zonder te kiezen: het Ei opent hem opnieuw.
 */
final class KiesMenu extends ChestMenu {
	private static final int RIJ = 9;

	private final List<UUID> spelers;
	private final BiConsumer<ServerPlayer, UUID> gekozen;

	private KiesMenu(int id, Inventory inv, SimpleContainer inhoud, int rijen, List<UUID> spelers,
			BiConsumer<ServerPlayer, UUID> gekozen) {
		super(type(rijen), id, inv, inhoud, rijen);
		this.spelers = spelers;
		this.gekozen = gekozen;
	}

	private static MenuType<ChestMenu> type(int rijen) {
		return switch (rijen) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			default -> MenuType.GENERIC_9x6;
		};
	}

	/**
	 * Opent het menu.
	 *
	 * @param keuzes  de spelers waaruit je kiest (hooguit 54)
	 * @param gekozen wat er gebeurt als hij een kop aanklikt: de kiezer en wie hij koos
	 */
	static void open(ServerPlayer speler, Component titel, List<ServerPlayer> keuzes, BiConsumer<ServerPlayer, UUID> gekozen) {
		int rijen = Math.max(1, Math.min(6, (keuzes.size() + RIJ - 1) / RIJ));
		SimpleContainer inhoud = new SimpleContainer(rijen * RIJ);
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < keuzes.size() && i < rijen * RIJ; i++) {
			ServerPlayer p = keuzes.get(i);
			ItemStack kop = new ItemStack(Items.PLAYER_HEAD);
			kop.set(DataComponents.PROFILE, ResolvableProfile.createResolved(p.getGameProfile()));
			Kleur k = Teams.keuze(p);
			kop.set(DataComponents.ITEM_NAME, k == null ? Mc.tekst(Mc.naam(p), ChatFormatting.WHITE, ChatFormatting.BOLD)
					: Mc.tekst(Mc.naam(p), Mc.kleur(k), ChatFormatting.BOLD));
			inhoud.setItem(i, kop);
			ids.add(p.getUUID());
		}
		speler.openMenu(new SimpleMenuProvider((id, inv, pl) -> new KiesMenu(id, inv, inhoud, rijen, ids, gekozen), titel));
	}

	static boolean heeftOpen(ServerPlayer speler) {
		return speler.containerMenu instanceof KiesMenu;
	}

	@Override
	public void clicked(int slot, int knop, ContainerInput input, Player player) {
		// Niets mag verschuiven: geen super. De mod handelt de klik zelf af.
		if (!(player instanceof ServerPlayer speler)) {
			return;
		}
		setCarried(ItemStack.EMPTY);
		if (slot >= 0 && slot < spelers.size()) {
			UUID wie = spelers.get(slot);
			speler.closeContainer();
			gekozen.accept(speler, wie);
			return;
		}
		broadcastChanges();
		sendAllDataToRemote();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return false;
	}

	@Override
	public boolean canDragTo(Slot slot) {
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer speler) {
			speler.inventoryMenu.sendAllDataToRemote();
		}
	}
}
