package nl.pudding.bootcamp.game.ronde4;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.teams.Teammenu;

import java.util.List;

/**
 * Het beurtmenu van de presentator (het kompas): één rij met de wol van elk team en zijn stand
 * ({@code Rood · 5}), van links naar rechts groen, geel, rood, blauw. Een klik geeft dat team
 * meteen de beurt, zonder rad (R4.7), en sluit het menu. Het team dat al aan de beurt is glinstert.
 * Items verplaatsen kan niet.
 */
public final class BeurtMenu extends ChestMenu {
	private static final int RIJ = 9;
	/** De teams van links naar rechts, op de plekken van {@link #PLEK}. */
	private static final List<Kleur> VOLGORDE = List.of(Kleur.GROEN, Kleur.GEEL, Kleur.ROOD, Kleur.BLAUW);
	private static final int[] PLEK = {1, 3, 5, 7};

	private final Quiz quiz;

	private BeurtMenu(int id, Inventory inv, SimpleContainer inhoud, Quiz quiz) {
		super(MenuType.GENERIC_9x1, id, inv, inhoud, 1);
		this.quiz = quiz;
	}

	static void open(ServerPlayer speler, Quiz quiz) {
		SimpleContainer inhoud = new SimpleContainer(RIJ);
		for (int i = 0; i < VOLGORDE.size(); i++) {
			Kleur k = VOLGORDE.get(i);
			ItemStack wol = new ItemStack(Teammenu.wol(k));
			wol.set(DataComponents.ITEM_NAME, Mc.tekst(k.naam() + " · " + quiz.punten(k), Mc.kleur(k), ChatFormatting.BOLD));
			if (k == quiz.aanDeBeurt()) {
				wol.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
			}
			inhoud.setItem(PLEK[i], wol);
		}
		speler.openMenu(new SimpleMenuProvider((id, inv, p) -> new BeurtMenu(id, inv, inhoud, quiz),
				Mc.tekst("Wie is er aan de beurt?", ChatFormatting.DARK_GRAY)));
	}

	@Override
	public void clicked(int slot, int knop, ContainerInput input, Player player) {
		// Niets mag verschuiven: geen super. De mod handelt de klik zelf af.
		if (player instanceof ServerPlayer speler) {
			Kleur k = teamBij(slot);
			setCarried(ItemStack.EMPTY);
			if (k != null && Spel.isPresentator(speler)) {
				speler.closeContainer();
				String fout = quiz.geefBeurt(speler.level().getServer(), k);
				if (fout != null) {
					Mc.actionbar(speler, Mc.tekst(fout, ChatFormatting.RED));
				}
				return;
			}
			broadcastChanges();
			sendAllDataToRemote();
		}
	}

	private static Kleur teamBij(int slot) {
		for (int i = 0; i < PLEK.length; i++) {
			if (PLEK[i] == slot) {
				return VOLGORDE.get(i);
			}
		}
		return null;
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
