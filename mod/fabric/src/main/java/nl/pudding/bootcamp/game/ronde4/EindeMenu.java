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

/**
 * De tweede bevestiging voor de barrier van de presentator: een kistmenu van één rij. Links
 * {@code Ja, de quiz is klaar} (dan hetzelfde als {@code /quiz einde}), rechts {@code Nee, verder},
 * en in het midden de stand. Items verplaatsen kan niet.
 */
public final class EindeMenu extends ChestMenu {
	private static final int JA = 2;
	private static final int STAND = 4;
	private static final int NEE = 6;

	private final Quiz quiz;

	private EindeMenu(int id, Inventory inv, SimpleContainer inhoud, Quiz quiz) {
		super(MenuType.GENERIC_9x1, id, inv, inhoud, 1);
		this.quiz = quiz;
	}

	static void open(ServerPlayer speler, Quiz quiz) {
		SimpleContainer inhoud = new SimpleContainer(9);
		inhoud.setItem(JA, knop(Items.WOOL.lime(), "Ja, de quiz is klaar", ChatFormatting.GREEN));
		inhoud.setItem(STAND, knop(Items.PAPER, stand(quiz), ChatFormatting.WHITE));
		inhoud.setItem(NEE, knop(Items.WOOL.red(), "Nee, verder met de quiz", ChatFormatting.RED));
		speler.openMenu(new SimpleMenuProvider((id, inv, p) -> new EindeMenu(id, inv, inhoud, quiz),
				Mc.tekst("De quiz beëindigen?", ChatFormatting.DARK_GRAY)));
	}

	/** {@code Rood 7 · Blauw 5 · Groen 3 · Geel 2}. */
	private static String stand(Quiz quiz) {
		StringBuilder sb = new StringBuilder();
		for (Kleur k : Kleur.values()) {
			if (!sb.isEmpty()) {
				sb.append(" · ");
			}
			sb.append(k.naam()).append(' ').append(quiz.punten(k));
		}
		return sb.toString();
	}

	private static ItemStack knop(Item soort, String naam, ChatFormatting kleur) {
		ItemStack knop = new ItemStack(soort);
		knop.set(DataComponents.ITEM_NAME, Mc.tekst(naam, kleur, ChatFormatting.BOLD));
		return knop;
	}

	@Override
	public void clicked(int slot, int knop, ContainerInput input, Player player) {
		// Niets mag verschuiven: geen super. De mod handelt de klik zelf af.
		if (!(player instanceof ServerPlayer speler)) {
			return;
		}
		setCarried(ItemStack.EMPTY);
		if (slot == JA && Spel.isPresentator(speler)) {
			speler.closeContainer();
			String fout = quiz.einde(speler.level().getServer());
			if (fout != null) {
				// Bij een gelijke stand: een beslisvraag en een punt met de emerald, of de commander kiest.
				speler.sendSystemMessage(Mc.tekst("Nog niet klaar: " + fout + ".", ChatFormatting.RED));
			}
			return;
		}
		if (slot == NEE) {
			speler.closeContainer();
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
