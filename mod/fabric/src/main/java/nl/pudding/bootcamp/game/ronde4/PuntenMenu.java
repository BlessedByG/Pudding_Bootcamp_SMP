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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.teams.Teammenu;

/**
 * Het puntenmenu van de presentator: een kistmenu met een rij per team. Vooraan de wol met de
 * stand ({@code Rood · 5}), daarachter −2, −1, +1 en +2. Een klik past de stand meteen aan, ook
 * voor een team dat niet aan de beurt is: voor wie bij Pudding slijmt. Items verplaatsen kan niet.
 */
public final class PuntenMenu extends ChestMenu {
	private static final int RIJ = 9;
	private static final int RIJEN = Kleur.values().length;
	private static final int[] KNOPPEN = {-2, -1, 1, 2};
	/** Op welke plek in de rij elke knop staat; de wol staat op 0. */
	private static final int[] PLEK = {2, 3, 5, 6};

	private final SimpleContainer inhoud;
	private final Quiz quiz;

	private PuntenMenu(int id, Inventory inv, SimpleContainer inhoud, Quiz quiz) {
		super(MenuType.GENERIC_9x4, id, inv, inhoud, RIJEN);
		this.inhoud = inhoud;
		this.quiz = quiz;
	}

	static void open(ServerPlayer speler, Quiz quiz) {
		SimpleContainer inhoud = new SimpleContainer(RIJ * RIJEN);
		vul(inhoud, quiz);
		speler.openMenu(new SimpleMenuProvider((id, inv, p) -> new PuntenMenu(id, inv, inhoud, quiz),
				Mc.tekst("Punten geven of afpakken", ChatFormatting.DARK_GRAY)));
	}

	private static void vul(SimpleContainer inhoud, Quiz quiz) {
		for (Kleur k : Kleur.values()) {
			int rij = k.ordinal() * RIJ;
			ItemStack wol = new ItemStack(Teammenu.wol(k));
			wol.set(DataComponents.ITEM_NAME, Mc.tekst(k.naam() + " · " + quiz.punten(k), Mc.kleur(k), ChatFormatting.BOLD));
			inhoud.setItem(rij, wol);
			for (int i = 0; i < KNOPPEN.length; i++) {
				inhoud.setItem(rij + PLEK[i], knop(KNOPPEN[i], k));
			}
		}
	}

	private static ItemStack knop(int aantal, Kleur k) {
		Item glas = switch (aantal) {
			case -2 -> Items.STAINED_GLASS_PANE.red();
			case -1 -> Items.STAINED_GLASS_PANE.orange();
			case 1 -> Items.STAINED_GLASS_PANE.lime();
			default -> Items.STAINED_GLASS_PANE.green();
		};
		ItemStack knop = new ItemStack(glas, Math.abs(aantal));
		String tekst = (aantal > 0 ? "+" : "") + aantal + " " + (aantal > 0 ? "voor " : "van ") + k.naam();
		knop.set(DataComponents.ITEM_NAME, Mc.tekst(tekst, aantal > 0 ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD));
		return knop;
	}

	@Override
	public void clicked(int slot, int knop, ContainerInput input, Player player) {
		// Niets mag verschuiven: geen super. De mod handelt de klik zelf af.
		if (player instanceof ServerPlayer speler) {
			Integer aantal = aantalBij(slot);
			if (aantal != null && Spel.isPresentator(speler)) {
				quiz.menuPunt(speler.level().getServer(), speler, Kleur.values()[slot / RIJ], aantal);
				vul(inhoud, quiz);
			}
			setCarried(ItemStack.EMPTY);
			broadcastChanges();
			sendAllDataToRemote();
		}
	}

	private static Integer aantalBij(int slot) {
		if (slot < 0 || slot >= RIJ * RIJEN) {
			return null;
		}
		int kolom = slot % RIJ;
		for (int i = 0; i < PLEK.length; i++) {
			if (PLEK[i] == kolom) {
				return KNOPPEN[i];
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
