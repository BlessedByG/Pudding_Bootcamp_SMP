package nl.pudding.bootcamp.teams;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Spel;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Het teammenu op de finishlijn van het doolhof: een vanilla kistmenu met een rij per kleur. Vooraan
 * een gekleurd wolblok met de teamnaam en het aantal ({@code Rood · 3/5}); alleen een klik op de wol
 * kiest dat team. Daarachter de hoofden van wie er al in zit. Een vol team is grijze wol. Items
 * verplaatsen kan niet: elke klik wordt afgevangen en de mod handelt hem zelf af. Allemaal
 * server-side, geen client-mod nodig.
 */
public final class Teammenu extends ChestMenu {
	private static final int RIJ = 9;
	private static final int RIJEN = Kleur.values().length;

	private final SimpleContainer inhoud;
	private final BiConsumer<ServerPlayer, Kleur> gekozen;

	private Teammenu(int id, Inventory inv, SimpleContainer inhoud, BiConsumer<ServerPlayer, Kleur> gekozen) {
		super(MenuType.GENERIC_9x4, id, inv, inhoud, RIJEN);
		this.inhoud = inhoud;
		this.gekozen = gekozen;
	}

	/**
	 * Opent het menu voor deze speler.
	 *
	 * @param gekozen wat er gebeurt als hij een kleur kiest die nog niet vol is
	 */
	public static void open(ServerPlayer speler, BiConsumer<ServerPlayer, Kleur> gekozen) {
		MinecraftServer server = speler.level().getServer();
		SimpleContainer inhoud = new SimpleContainer(RIJ * RIJEN);
		vul(server, inhoud);
		speler.openMenu(new SimpleMenuProvider((id, inv, p) -> new Teammenu(id, inv, inhoud, gekozen),
				Mc.tekst("Kies je team", ChatFormatting.DARK_GRAY)));
	}

	public static boolean heeftOpen(ServerPlayer speler) {
		return speler.containerMenu instanceof Teammenu;
	}

	private static void vul(MinecraftServer server, SimpleContainer inhoud) {
		int max = Teams.maximum(server);
		Map<Kleur, List<String>> namen = Teams.namen();
		for (Kleur k : Kleur.values()) {
			int rij = k.ordinal() * RIJ;
			int aantal = Teams.aantal(k);
			boolean vol = aantal >= max;
			ItemStack wol = new ItemStack(vol ? Items.WOOL.gray() : wol(k));
			wol.set(DataComponents.ITEM_NAME, Mc.tekst(k.naam() + " · " + aantal + "/" + max,
					vol ? ChatFormatting.GRAY : Mc.kleur(k), ChatFormatting.BOLD));
			inhoud.setItem(rij, wol);
			// De hoofden erachter; zijn het er meer dan passen, dan de laatste plek "+3 meer".
			List<String> leden = namen.get(k);
			boolean teVeel = leden.size() > RIJ - 1;
			int koppen = teVeel ? RIJ - 2 : leden.size();
			for (int i = 1; i < RIJ; i++) {
				inhoud.setItem(rij + i, ItemStack.EMPTY);
			}
			for (int i = 0; i < koppen; i++) {
				inhoud.setItem(rij + 1 + i, kop(server, leden.get(i), k));
			}
			if (teVeel) {
				ItemStack meer = new ItemStack(Items.PAPER);
				meer.set(DataComponents.ITEM_NAME, Mc.tekst("+" + (leden.size() - koppen) + " meer", Mc.kleur(k)));
				inhoud.setItem(rij + RIJ - 1, meer);
			}
		}
	}

	/** Het hoofd van een teamlid, met zijn skin; online met zijn eigen profiel, anders op naam. */
	private static ItemStack kop(MinecraftServer server, String naam, Kleur k) {
		ServerPlayer p = server.getPlayerList().getPlayerByName(naam);
		ItemStack kop = new ItemStack(Items.PLAYER_HEAD);
		kop.set(DataComponents.PROFILE, p != null ? ResolvableProfile.createResolved(p.getGameProfile())
				: ResolvableProfile.createUnresolved(naam));
		// Een hoofd met een profiel heet anders "Piet's Head"; een eigen naam gaat daarvoor.
		kop.set(DataComponents.CUSTOM_NAME, Mc.tekst(p != null ? Mc.naam(p) : naam, Mc.kleur(k))
				.withStyle(s -> s.withItalic(false)));
		return kop;
	}

	private static Item wol(Kleur k) {
		return switch (k) {
			case ROOD -> Items.WOOL.red();
			case BLAUW -> Items.WOOL.blue();
			case GROEN -> Items.WOOL.lime();
			case GEEL -> Items.WOOL.yellow();
		};
	}

	/** Elke seconde: de aantallen bijwerken voor wie het menu open heeft. */
	public static void ververs(MinecraftServer server) {
		for (ServerPlayer s : Mc.spelers(server)) {
			if (s.containerMenu instanceof Teammenu menu) {
				vul(server, menu.inhoud);
				menu.broadcastChanges();
			}
		}
	}

	@Override
	public void clicked(int slot, int knop, ContainerInput input, Player player) {
		// Niets mag verschuiven: geen super. Wat de client denkt te hebben verplaatst zetten we terug.
		if (player instanceof ServerPlayer speler) {
			Kleur k = kleurBij(slot);
			if (k != null) {
				if (Teams.vol(speler.level().getServer(), k)) {
					Mc.actionbar(speler, Mc.tekst(k.naam() + " is vol", ChatFormatting.RED));
				} else {
					// Eén tick later, niet midden in het klik-pakket: menu dicht, dan de keuze.
					Planner.na(1, () -> {
						if (speler.containerMenu == this) {
							speler.closeContainer();
						}
						gekozen.accept(speler, k);
					});
					setCarried(ItemStack.EMPTY);
					sendAllDataToRemote();
					return;
				}
			}
			setCarried(ItemStack.EMPTY);
			sendAllDataToRemote();
		}
	}

	/** Alleen de wol vooraan in een rij kiest een team; een hoofd niet. */
	private static Kleur kleurBij(int slot) {
		if (slot < 0 || slot >= RIJ * RIJEN || slot % RIJ != 0) {
			return null;
		}
		return Kleur.values()[slot / RIJ];
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
	public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
		return false;
	}

	@Override
	public boolean canDragTo(net.minecraft.world.inventory.Slot slot) {
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer speler) {
			// Dicht zonder te kiezen: het doolhof opent hem na twee seconden opnieuw.
			Spel.status(speler).menuDicht = speler.level().getServer().getTickCount();
			speler.inventoryMenu.sendAllDataToRemote();
		}
	}
}
