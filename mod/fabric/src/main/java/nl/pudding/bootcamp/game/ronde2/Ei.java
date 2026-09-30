package nl.pudding.bootcamp.game.ronde2;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.EiBlok;
import nl.pudding.bootcamp.core.EiVerdeling;
import nl.pudding.bootcamp.core.Hussel;
import nl.pudding.bootcamp.core.Klassement;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.game.ronde1.Doolhof;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.schrik.Schrik;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 2: het Ei. De mod zet het Ei terug zoals het is vastgelegd en strooit de blokken op de
 * gewone deepslate. Iedereen begint aan het buiteneinde van een ketting, survival, met een diamond
 * pickaxe erbij. Alleen het Ei is te breken, alles zonder drop; neerzetten kan nergens, behalve de
 * TNT uit het Ei. Alles wat iemand vindt staat in de chat. De meeste punten wint.
 */
public final class Ei extends RondeLogica {
	/** Twee seconden staat er in de actionbar wat je net kreeg. */
	private static final int PLUS_TICKS = 40;
	private static final int PAARS = 0xAA00AA;
	/** In de custom data van de pickaxe: zijn Efficiency van voor de glowstone. */
	private static final String TURBO_TAG = "bootcamp_turbo";

	private final Klassement stand = new Klassement();
	private final Map<UUID, Integer> plus = new HashMap<>();
	private final Map<UUID, Integer> plusTot = new HashMap<>();
	private final Map<UUID, Integer> hasteTot = new HashMap<>();
	/** Glowstone: tot wanneer de pickaxe Efficiency V heeft. */
	private final Map<UUID, Integer> turboTot = new HashMap<>();
	private final Map<UUID, Integer> misselijkTot = new HashMap<>();
	/** Waar de mod blokken strooide: alleen daar doet een blok iets, niet in de versiering. */
	private final Set<Long> gestrooid = new HashSet<>();
	private final List<PrimedTnt> tnts = new ArrayList<>();
	/** Wie de lopende bevriezing veroorzaakte; alle anderen staan stil tot {@link #bevrorenTot}. */
	private UUID bevriezer;
	private int bevrorenTot;
	private boolean bevriezingLoopt;
	private boolean timerGestart;
	private int volgendeSpawn;

	public static void init() {
		Huldiging.init();
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer speler) || Mc.isStaff(speler) || !(Spel.actief() instanceof Ei ei)) {
				return true;
			}
			return ei.breek(speler, pos, state);
		});
		// Neerzetten kan in het Ei nergens: geen blokken, geen emmers. Alleen de TNT uit het Ei, en
		// die gaat niet als blok neer maar meteen af.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(player instanceof ServerPlayer speler) || Mc.isStaff(speler) || !(Spel.actief() instanceof Ei ei)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Items.TNT) && Items26.heeftTag(stack, Items26.EI_TAG)) {
				ei.zetTnt(speler, stack, hit.getBlockPos().relative(hit.getDirection()));
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.SUCCESS;
			}
			if (isNeerzetten(stack)) {
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer speler && !Mc.isStaff(speler) && Spel.actief() instanceof Ei
					&& player.getItemInHand(hand).getItem() instanceof BucketItem) {
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	private static boolean isNeerzetten(ItemStack stack) {
		return stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem;
	}

	@Override
	public Ronde ronde() {
		return Ronde.EI;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		int deepslate = EiOpslag.deepslate(server);
		if (deepslate < 0) {
			return "ontbreekt: /ei vastleggen";
		}
		int totaal = Spel.instellingen().eiTotaal();
		if (totaal > deepslate) {
			return "samen " + totaal + " puntenblokken, maar het Ei heeft maar " + deepslate + " deepslate-plekken (/ei blokken)";
		}
		String fout = Kits.controleer(server, "ei");
		if (fout != null) {
			return fout;
		}
		return Spel.buitenRegio("eigebied", Spel.reeks("ei_spawn_"));
	}

	@Override
	public void start(MinecraftServer server) {
		// Eerst het Ei terug en de puntenblokken erin; dat is klaar voordat de countdown afloopt.
		EiOpslag.Vastlegging v = EiOpslag.get(server);
		Map<Integer, BlockState> extra = strooi(v);
		for (int i : extra.keySet()) {
			gestrooid.add(v.pos(i).asLong());
		}
		EiOpslag.herstel(server, extra, null);
		Spelregels.locatorBar(server, false);
		Spel.maakSpelers(server, true);
		List<String> spawns = Spel.reeks("ei_spawn_");
		List<ServerPlayer> spelers = Mc.deelnemers(server);
		for (ServerPlayer s : spelers) {
			String spawn = spawns.get(Math.floorMod(volgendeSpawn++, spawns.size()));
			Spel.status(s).eiSpawn = spawn;
			Spel.naarPunt(s, spawn);
		}
		// De pickaxe erbij, gemarkeerd: aan het eind gaat hij weer weg.
		Kits.geefAan(server, "ei", spelers, stack -> Items26.markeer(stack, Items26.EI_TAG, null));
		// Binnenin het Ei wordt het snel donker: Night Vision voor iedereen die meedoet.
		for (ServerPlayer s : spelers) {
			hotbarVoorHetEi(s);
			nachtzicht(s);
		}
		Border.zet(server, Spel.regio("eigebied"));
		toonSidebar(server);
		Aftelling.start(Regels.COUNTDOWN, "Het Ei opent over", () -> {
			timerGestart = true;
			Spel.startTimer(Spel.instellingen().eiTimer() * 60);
		});
	}

	/** Uit de vastgelegde deepslate trekt de mod de plekken voor de puntenblokken. */
	private static Map<Integer, BlockState> strooi(EiOpslag.Vastlegging v) {
		Map<EiBlok, int[]> getrokken = EiVerdeling.trek(v.deepslate().length, Spel.instellingen().eiBlokken(), Spel.RANDOM);
		Map<Integer, BlockState> uit = new HashMap<>();
		getrokken.forEach((blok, plekken) -> {
			BlockState state = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(blok.blok())).defaultBlockState();
			for (int p : plekken) {
				uit.put(v.deepslate()[p], state);
			}
		});
		return uit;
	}

	// Breken

	/** @return {@code true} als vanilla het blok gewoon mag breken (nooit, in het Ei) */
	private boolean breek(ServerPlayer speler, BlockPos pos, BlockState state) {
		SpelerStatus st = Spel.status(speler);
		Regio ei = Spel.regio("ei");
		if (!timerGestart || !Spel.timerLoopt() || st.rol != Rol.SPELER || ei == null
				|| !ei.omhullende().bevatDoos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			return false;
		}
		// De kettingen zijn om naar het Ei te klimmen, ook waar ze in de doos van het Ei liggen.
		if (state.getBlock() instanceof ChainBlock) {
			return false;
		}
		// Alles zonder drop: er is niks om mee te nemen en je inventory loopt niet vol.
		speler.level().removeBlock(pos, false);
		if (!gestrooid.remove(pos.asLong())) {
			return false;
		}
		EiBlok blok = EiBlok.vanBlok(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		if (blok == null) {
			return false;
		}
		MinecraftServer server = speler.level().getServer();
		switch (blok) {
			case NETHERITE, DIAMOND, GOLD -> punten(server, speler, blok);
			case REDSTONE -> redstone(server, speler);
			case EMERALD -> emerald(server, speler);
			case TNT -> tnt(server, speler);
			case GLOWSTONE -> glowstone(server, speler);
			case SLIME -> slime(server, speler);
			case TARGET -> hussel(server, speler);
		}
		return false;
	}

	/** Alles wat iemand vindt, staat in de chat. */
	private static void chat(MinecraftServer server, ServerPlayer speler, Component wat) {
		Mc.chatAllen(server, Component.empty().append(naam(speler)).append(wat));
	}

	private void punten(MinecraftServer server, ServerPlayer speler, EiBlok blok) {
		int erbij = blok.punten();
		stand.voegToe(speler.getUUID(), erbij);
		plus.put(speler.getUUID(), erbij);
		plusTot.put(speler.getUUID(), server.getTickCount() + PLUS_TICKS);
		Mc.geluid(speler, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f);
		ChatFormatting kleur = switch (blok) {
			case NETHERITE -> ChatFormatting.LIGHT_PURPLE;
			case DIAMOND -> ChatFormatting.AQUA;
			default -> ChatFormatting.GOLD;
		};
		chat(server, speler, Mc.tekst(" hakte " + blok.id() + " (+" + erbij + ")", kleur));
		actionbar(server, speler);
		toonSidebar(server);
	}

	private void redstone(MinecraftServer server, ServerPlayer speler) {
		if (Regels.redstoneGok(Spel.RANDOM) == Regels.Gok.HASTE) {
			Mc.effect(speler, MobEffects.HASTE, Regels.EI_HASTE, 1);
			hasteTot.put(speler.getUUID(), server.getTickCount() + Regels.EI_HASTE * 20);
			Mc.geluid(speler, SoundEvents.BEACON_POWER_SELECT, 1f, 1f);
			Mc.title(speler, Mc.tekst("HASTE", ChatFormatting.GOLD, ChatFormatting.BOLD),
					Mc.tekst(Regels.EI_HASTE + " seconden sneller hakken", ChatFormatting.YELLOW), 0, 40, 10);
			chat(server, speler, Mc.tekst(" hakte redstone: " + Regels.EI_HASTE + " seconden Haste", ChatFormatting.RED));
			actionbar(server, speler);
			return;
		}
		chat(server, speler, Mc.tekst(" hakte redstone: iedereen bevroren", ChatFormatting.RED));
		// Een nieuwe bevriezing vervangt een lopende: nu is deze hakker de enige die los is.
		bevriezer = speler.getUUID();
		bevrorenTot = server.getTickCount() + Regels.EI_BEVRIEZING * 20;
		bevriezingLoopt = true;
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (s == speler) {
				Opstelling.ontdooi(s);
				s.removeEffect(MobEffects.MINING_FATIGUE);
			} else {
				Opstelling.bevries(s);
				Mc.effect(s, MobEffects.MINING_FATIGUE, Regels.EI_BEVRIEZING, 4);
			}
		}
		Mc.titleAllen(server, Mc.tekst("BEVROREN", ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("door ", ChatFormatting.WHITE)).append(naam(speler)), 0, 50, 10);
		Mc.geluidAllen(server, SoundEvents.GLASS_BREAK, 1f, 1f);
	}

	private void emerald(MinecraftServer server, ServerPlayer speler) {
		List<ServerPlayer> anderen = new ArrayList<>(Spel.levend(server, Rol.SPELER));
		anderen.remove(speler);
		if (anderen.isEmpty()) {
			Spel.melding(speler, Mc.tekst("Niemand om te laten schrikken", ChatFormatting.GRAY), 2);
			actionbar(server, speler);
			return;
		}
		ServerPlayer ander = anderen.get(Spel.RANDOM.nextInt(anderen.size()));
		Schrik.op(ander);
		Spel.melding(speler, Component.empty().append(Mc.tekst("Jumpscare naar ", ChatFormatting.GREEN)).append(naam(ander)), 2);
		actionbar(server, speler);
		// Groot in beeld wie wie liet schrikken; de foto staat zelf als title, dus die krijgt het na de foto.
		MutableComponent wie = Component.empty().append(naam(speler)).append(Mc.tekst(" → ", ChatFormatting.GREEN)).append(naam(ander));
		Component sub = Mc.tekst("JUMPSCARE", ChatFormatting.GREEN, ChatFormatting.BOLD);
		UUID anderId = ander.getUUID();
		Mc.titleAllenBehalve(server, List.of(anderId), wie, sub, 0, 50, 10);
		chat(server, speler, Component.empty().append(Mc.tekst(" liet ", ChatFormatting.GREEN)).append(naam(ander))
				.append(Mc.tekst(" schrikken", ChatFormatting.GREEN)));
		String breker = Mc.naam(speler);
		Kleur kleur = Teams.keuze(speler);
		// Als de foto wegvaagt: van wie hij kwam.
		Planner.na(Schrik.IN_BEELD, () -> {
			ServerPlayer a = server.getPlayerList().getPlayer(anderId);
			if (a != null && Spel.actief() == this) {
				Mc.title(a, wie, sub, 0, 50, 10);
				Spel.melding(a, Component.empty().append(Mc.tekst("Met dank aan ", ChatFormatting.GREEN))
						.append(Mc.tekst(breker, Mc.kleur(kleur))), 3);
				actionbar(server, a);
			}
		});
	}

	/** TNT: een TNT in je inventory. Zet je hem neer, dan gaat hij meteen af ({@link #zetTnt}). */
	private void tnt(MinecraftServer server, ServerPlayer speler) {
		ItemStack stack = new ItemStack(Items.TNT);
		// Gemarkeerd: aan het eind van het Ei gaat hij weg, net als de pickaxe.
		Items26.markeer(stack, Items26.EI_TAG, null);
		if (!speler.getInventory().add(stack)) {
			speler.drop(stack, false);
		}
		Mc.geluid(speler, SoundEvents.ITEM_PICKUP, 1f, 0.8f);
		Mc.title(speler, Mc.tekst("TNT", ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst("zet hem neer: hij gaat meteen af", ChatFormatting.YELLOW), 0, 40, 10);
		chat(server, speler, Mc.tekst(" hakte TNT", ChatFormatting.RED));
	}

	/** De TNT uit het Ei: meteen aan, en hij blijft hangen waar hij neergezet is. */
	private void zetTnt(ServerPlayer speler, ItemStack stack, BlockPos plek) {
		ServerLevel wereld = (ServerLevel) speler.level();
		if (!timerGestart || !wereld.getBlockState(plek).canBeReplaced()) {
			return;
		}
		PrimedTnt tnt = new PrimedTnt(wereld, plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, speler);
		// Geen sprongetje en niet vallen: aan de zijkant van het Ei valt hij anders naar beneden.
		tnt.setDeltaMovement(Vec3.ZERO);
		tnt.setNoGravity(true);
		wereld.addFreshEntity(tnt);
		wereld.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1f, 1f);
		stack.shrink(1);
		tnts.add(tnt);
	}

	/**
	 * De knal van een TNT uit het Ei. Vanilla TNT ontploft niet (gamerule): de mod doet het zelf.
	 * Spelers krijgen gewone TNT-schade en een duw; van de blokken gaat alleen gewone deepslate in
	 * het Ei weg, zonder drop. Speciale blokken, de schil en de kettingen blijven staan.
	 */
	private void knal(MinecraftServer server, double x, double y, double z) {
		ServerLevel wereld = Mc.wereld(server);
		wereld.explode(null, x, y + 0.0625, z, 4.0f, Level.ExplosionInteraction.NONE);
		Mc.particles(wereld, ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0);
		Regio ei = Spel.regio("ei");
		if (ei == null) {
			return;
		}
		Regio.Doos doos = ei.omhullende();
		BlockPos midden = BlockPos.containing(x, y, z);
		int r = Regels.EI_TNT_STRAAL;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos p = midden.offset(dx, dy, dz);
					if (dx * dx + dy * dy + dz * dz <= r * r && doos.bevatDoos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)
							&& wereld.getBlockState(p).is(Blocks.DEEPSLATE)) {
						wereld.removeBlock(p, false);
					}
				}
			}
		}
	}

	/** Glowstone: even Efficiency V op de pickaxe en Haste II; een nieuwe verlengt. */
	private void glowstone(MinecraftServer server, ServerPlayer speler) {
		turbo(speler, true);
		turboTot.put(speler.getUUID(), server.getTickCount() + Regels.EI_GLOWSTONE * 20);
		Mc.effect(speler, MobEffects.HASTE, Regels.EI_GLOWSTONE, 1);
		Mc.geluid(speler, SoundEvents.BEACON_ACTIVATE, 1f, 1.5f);
		Mc.title(speler, Mc.tekst("TURBO", ChatFormatting.YELLOW, ChatFormatting.BOLD),
				Mc.tekst(Regels.EI_GLOWSTONE + " seconden Efficiency V en Haste II", ChatFormatting.YELLOW), 0, 40, 10);
		chat(server, speler, Mc.tekst(" hakte glowstone: " + Regels.EI_GLOWSTONE + " seconden Efficiency V", ChatFormatting.YELLOW));
		actionbar(server, speler);
	}

	/**
	 * Efficiency V op de pickaxe uit het Ei, of terug naar wat hij had. Het oude niveau staat in de
	 * custom data van de pickaxe zelf, zodat het ook klopt als hij verschoven is.
	 */
	private static void turbo(ServerPlayer speler, boolean aan) {
		Holder<Enchantment> efficiency = speler.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.EFFICIENCY);
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.is(ItemTags.PICKAXES) || !Items26.heeftTag(s, Items26.EI_TAG)) {
				continue;
			}
			if (aan) {
				if (Items26.tagWaarde(s, TURBO_TAG) == null) {
					Items26.markeer(s, TURBO_TAG, String.valueOf(EnchantmentHelper.getItemEnchantmentLevel(efficiency, s)));
				}
				EnchantmentHelper.updateEnchantments(s, e -> e.set(efficiency, 5));
			} else {
				String oud = Items26.tagWaarde(s, TURBO_TAG);
				if (oud == null) {
					continue;
				}
				int niveau = Integer.parseInt(oud);
				EnchantmentHelper.updateEnchantments(s, e -> e.set(efficiency, niveau));
				Items26.wisTag(s, TURBO_TAG);
			}
		}
		speler.inventoryMenu.broadcastChanges();
	}

	/** Slime: Nausea voor iedereen behalve wie hem hakte. */
	private void slime(MinecraftServer server, ServerPlayer speler) {
		int tot = server.getTickCount() + Regels.EI_MISSELIJK * 20;
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (s != speler) {
				Mc.effect(s, MobEffects.NAUSEA, Regels.EI_MISSELIJK, 0);
				misselijkTot.put(s.getUUID(), tot);
			}
		}
		Mc.titleAllenBehalve(server, List.of(speler.getUUID()), Mc.tekst("MISSELIJK", ChatFormatting.GREEN, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("door ", ChatFormatting.WHITE)).append(naam(speler)), 0, 50, 10);
		Mc.geluidAllen(server, SoundEvents.SLIME_SQUISH, 1f, 0.8f);
		Spel.melding(speler, Mc.tekst("Iedereen is " + Regels.EI_MISSELIJK + " seconden misselijk", ChatFormatting.GREEN), 2);
		chat(server, speler, Mc.tekst(" hakte slime: iedereen misselijk", ChatFormatting.GREEN));
		actionbar(server, speler);
	}

	/** Target: iedereen die meedoet staat ineens op de plek van een ander, niemand op zijn eigen plek. */
	private void hussel(MinecraftServer server, ServerPlayer speler) {
		List<ServerPlayer> spelers = new ArrayList<>(Spel.levend(server, Rol.SPELER));
		if (spelers.size() < 2) {
			Spel.melding(speler, Mc.tekst("Niemand om mee te husselen", ChatFormatting.GRAY), 2);
			actionbar(server, speler);
			return;
		}
		List<Punt> plekken = new ArrayList<>();
		for (ServerPlayer s : spelers) {
			plekken.add(Punt.positie(s.getX(), s.getY(), s.getZ(), s.getYRot(), s.getXRot()));
		}
		int[] naar = Hussel.verdeel(spelers.size(), Spel.RANDOM);
		for (int i = 0; i < spelers.size(); i++) {
			Mc.teleport(spelers.get(i), plekken.get(naar[i]));
		}
		Mc.titleAllen(server, Mc.tekst("GEHUSSELD", ChatFormatting.RED, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst("door ", ChatFormatting.WHITE)).append(naam(speler)), 0, 50, 10);
		Mc.geluidAllen(server, SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
		chat(server, speler, Mc.tekst(" hakte een target: iedereen gehusseld", ChatFormatting.RED));
	}

	/**
	 * Alleen de pickaxe in de hotbar: wat er uit het doolhof in zat gaat naar de inventory, en de
	 * spullen uit {@code ei.json} komen vooraan, met slot 1 geselecteerd. Los van waar de kit ze zet.
	 * Er gaat niets verloren: past het niet in de inventory, dan blijft het staan, en vooraan zetten
	 * is ruilen.
	 */
	private static void hotbarVoorHetEi(ServerPlayer speler) {
		Inventory inv = speler.getInventory();
		int vrij = Inventory.SELECTION_SIZE;
		for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty() || Items26.heeftTag(s, Items26.EI_TAG)) {
				continue;
			}
			while (vrij < Inventory.INVENTORY_SIZE && !inv.getItem(vrij).isEmpty()) {
				vrij++;
			}
			if (vrij >= Inventory.INVENTORY_SIZE) {
				break;
			}
			inv.setItem(vrij, s);
			inv.setItem(i, ItemStack.EMPTY);
		}
		int plek = 0;
		for (int i = 0; i < Inventory.INVENTORY_SIZE && plek < Inventory.SELECTION_SIZE; i++) {
			ItemStack s = inv.getItem(i);
			if (!Items26.heeftTag(s, Items26.EI_TAG)) {
				continue;
			}
			if (i != plek) {
				inv.setItem(i, inv.getItem(plek));
				inv.setItem(plek, s);
			}
			plek++;
		}
		inv.setSelectedSlot(0);
		speler.connection.send(new ClientboundSetHeldSlotPacket(0));
		speler.inventoryMenu.broadcastChanges();
	}

	/** Night Vision zonder deeltjes, zolang het Ei duurt. */
	private static void nachtzicht(ServerPlayer speler) {
		Mc.effect(speler, MobEffects.NIGHT_VISION, -1, 0);
	}

	private static MutableComponent naam(ServerPlayer speler) {
		return Mc.tekst(Mc.naam(speler), Mc.kleur(Teams.keuze(speler)));
	}

	// Actionbar, sidebar, bossbar

	/** {@code Bevroren · 12 · +10 · 85 punten · #4}; een melding (jumpscare) staat er even in de plaats van. */
	private void actionbar(MinecraftServer server, ServerPlayer speler) {
		Component melding = Spel.melding(speler);
		if (melding != null) {
			Mc.actionbar(speler, melding);
			return;
		}
		int nu = server.getTickCount();
		UUID id = speler.getUUID();
		MutableComponent regel = Component.empty();
		if (bevriezingLoopt && !id.equals(bevriezer) && nu < bevrorenTot) {
			regel.append(Mc.tekst("Bevroren · " + secondenTot(bevrorenTot, nu) + " · ", ChatFormatting.AQUA));
		}
		Integer turbo = turboTot.get(id);
		if (turbo != null && nu < turbo) {
			regel.append(Mc.tekst("Efficiency V · " + secondenTot(turbo, nu) + " · ", ChatFormatting.YELLOW));
		}
		Integer haste = hasteTot.get(id);
		if (haste != null && nu < haste) {
			regel.append(Mc.tekst("Haste · " + secondenTot(haste, nu) + " · ", ChatFormatting.GOLD));
		}
		Integer p = plus.get(id);
		Integer pTot = plusTot.get(id);
		if (p != null && pTot != null && nu < pTot) {
			if (p >= EiBlok.NETHERITE.punten()) {
				regel.append(Component.literal("+" + p).withStyle(s -> s.withColor(PAARS).withBold(true)));
			} else {
				regel.append(Mc.tekst("+" + p, ChatFormatting.GREEN, ChatFormatting.BOLD));
			}
			regel.append(Mc.tekst(" · ", ChatFormatting.WHITE));
		}
		int score = stand.score(id);
		int plek = stand.plek(id);
		regel.append(Mc.tekst(score + " punten" + (plek > 0 ? " · #" + plek : ""), ChatFormatting.WHITE));
		Mc.actionbar(speler, regel);
	}

	private static int secondenTot(int tot, int nu) {
		return (tot - nu + 19) / 20;
	}

	private void toonSidebar(MinecraftServer server) {
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Klassement.Regel r : stand.top(10)) {
			String naam = Spel.naamVan(r.speler());
			ServerPlayer s = server.getPlayerList().getPlayer(r.speler());
			Kleur k = s == null ? null : Teams.keuze(s);
			regels.add(new Sidebar.Regel(Mc.tekst(naam, Mc.kleur(k)), r.score()));
		}
		Sidebar.toonScores(server, "Het Ei · top 10", regels);
	}

	/** TNT uit het Ei: bij fuse 0 haalt vanilla hem weg zonder knal, dan knallen wij. */
	@Override
	public void tick(MinecraftServer server) {
		for (Iterator<PrimedTnt> it = tnts.iterator(); it.hasNext(); ) {
			PrimedTnt tnt = it.next();
			if (!tnt.isRemoved()) {
				continue;
			}
			it.remove();
			if (tnt.getRemovalReason() != null && tnt.getRemovalReason().shouldDestroy()) {
				knal(server, tnt.getX(), tnt.getY(), tnt.getZ());
			}
		}
	}

	@Override
	public void seconde(MinecraftServer server) {
		int nu = server.getTickCount();
		// Glowstone voorbij: de pickaxe terug naar zijn eigen Efficiency.
		for (Map.Entry<UUID, Integer> e : new ArrayList<>(turboTot.entrySet())) {
			if (nu >= e.getValue()) {
				turboTot.remove(e.getKey());
				ServerPlayer s = server.getPlayerList().getPlayer(e.getKey());
				if (s != null) {
					turbo(s, false);
					Mc.geluid(s, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
				}
			}
		}
		// Haste en bevriezing voorbij: een pling.
		for (Map.Entry<UUID, Integer> e : new ArrayList<>(hasteTot.entrySet())) {
			if (nu >= e.getValue()) {
				hasteTot.remove(e.getKey());
				ServerPlayer s = server.getPlayerList().getPlayer(e.getKey());
				if (s != null) {
					Mc.geluid(s, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
				}
			}
		}
		if (bevriezingLoopt && nu >= bevrorenTot) {
			bevriezingLoopt = false;
			for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
				if (Spel.status(s).bevroren) {
					Opstelling.ontdooi(s);
					s.removeEffect(MobEffects.MINING_FATIGUE);
					Mc.geluid(s, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
				}
			}
		}
		if (!timerGestart) {
			return;
		}
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			actionbar(server, s);
		}
		int timer = Spel.timer();
		BossEvent.BossBarColor kleur = timer <= Regels.LAATSTE_MINUUT ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN;
		Bossbar.zet(BossbarTekst.ei(timer), kleur, Spel.timerDeel());
		Doolhof.laatsteTellen(server, timer);
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server);
	}

	private void einde(MinecraftServer server) {
		Klassement.Regel winnaar = stand.winnaar().orElse(null);
		Spel.einde(server);
		Bossbar.basiskamp();
		if (winnaar != null) {
			ServerPlayer w = server.getPlayerList().getPlayer(winnaar.speler());
			String naam = Spel.naamVan(winnaar.speler()).toUpperCase(java.util.Locale.ROOT);
			MutableComponent titel = Component.empty();
			if (w != null) {
				titel.append(Mc.kop(w)).append(Component.literal(" "));
			}
			titel.append(Mc.tekst(naam + " WINT HET EI", ChatFormatting.GOLD, ChatFormatting.BOLD));
			Mc.titleAllen(server, titel, Mc.tekst(winnaar.score() + " punten", ChatFormatting.YELLOW), 10, 80, 20);
		} else {
			Mc.titleAllen(server, Mc.tekst("HET EI IS VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD),
					Mc.tekst("niemand hakte punten", ChatFormatting.GRAY), 10, 80, 20);
		}
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		Spel.maakSpelers(server, false);
		// De huldiging: winnaar op het podium, de rest op het plein, het Warden-ei in het frame.
		UUID winnaarId = winnaar == null ? null : winnaar.speler();
		Huldiging.opstellen(server, winnaarId);
		Huldiging.prijsKlaarzetten(server, winnaarId);
		toonSidebar(server);
	}

	/** Van het Ei of een ketting gevallen: geheald terug op je eigen startplek, met je punten en spullen. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		String spawn = Spel.status(speler).eiSpawn;
		Spel.naarPunt(speler, spawn != null ? spawn : "ei_spawn_1");
		// De heal bij een dood haalt alle effecten weg.
		nachtzicht(speler);
		if (bevriezingLoopt && !speler.getUUID().equals(bevriezer)) {
			// De heal haalt de effecten weg; de bevriezing blijft staan.
			Mc.effect(speler, MobEffects.MINING_FATIGUE, Math.max(1, (bevrorenTot - server.getTickCount()) / 20), 4);
		}
		// Haste en Nausea lopen ook gewoon door.
		int nu = server.getTickCount();
		UUID id = speler.getUUID();
		nogEven(speler, MobEffects.HASTE, Math.max(hasteTot.getOrDefault(id, 0), turboTot.getOrDefault(id, 0)), nu, 1);
		nogEven(speler, MobEffects.NAUSEA, misselijkTot.getOrDefault(id, 0), nu, 0);
	}

	private static void nogEven(ServerPlayer speler, Holder<MobEffect> effect, int tot, int nu, int niveau) {
		if (nu < tot) {
			Mc.effect(speler, effect, Math.max(1, (tot - nu) / 20), niveau);
		}
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		Teams.zorgVoorTeam(server, speler);
		Spel.zetRol(server, speler, Rol.SPELER);
		speler.setGameMode(GameType.SURVIVAL);
		if (st.eiSpawn == null) {
			List<String> spawns = Spel.reeks("ei_spawn_");
			st.eiSpawn = spawns.isEmpty() ? "ei_spawn_1" : spawns.get(Math.floorMod(volgendeSpawn++, spawns.size()));
			// Nieuw in deze ronde: de pickaxe alsnog.
			Kits.geefAan(server, "ei", List.of(speler), stack -> Items26.markeer(stack, Items26.EI_TAG, null));
			hotbarVoorHetEi(speler);
		}
		Spel.naarPunt(speler, st.eiSpawn);
		nachtzicht(speler);
		// Liep de glowstone af terwijl hij weg was: de pickaxe alsnog terug.
		if (!turboTot.containsKey(speler.getUUID())) {
			turbo(speler, false);
		}
		// Een bevriezing die nog loopt geldt ook voor hem; een die voorbij is niet meer.
		int nu = server.getTickCount();
		if (bevriezingLoopt && nu < bevrorenTot && !speler.getUUID().equals(bevriezer)) {
			Opstelling.bevries(speler);
			Mc.effect(speler, MobEffects.MINING_FATIGUE, Math.max(1, (bevrorenTot - nu) / 20), 4);
		} else {
			speler.removeEffect(MobEffects.MINING_FATIGUE);
		}
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		Klassement.Regel w = stand.winnaar().orElse(null);
		return "Ei: " + stand.volgorde().size() + " spelers met punten" + (w == null ? "" : ", bovenaan " + Spel.naamVan(w.speler()) + " met " + w.score())
				+ (EiOpslag.bezig() ? ", het Ei wordt nog teruggezet" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		for (ServerPlayer s : Mc.deelnemers(server)) {
			// De pickaxe is alleen voor het Ei.
			Items26.haalWeg(s, Items26.EI_TAG);
			s.setGameMode(GameType.ADVENTURE);
			s.removeEffect(MobEffects.HASTE);
			s.removeEffect(MobEffects.NIGHT_VISION);
			s.removeEffect(MobEffects.MINING_FATIGUE);
			s.removeEffect(MobEffects.NAUSEA);
		}
		// TNT die nog brandt gaat weg zonder knal.
		tnts.forEach(Entity::discard);
		tnts.clear();
		Sidebar.weg(server);
	}
}
