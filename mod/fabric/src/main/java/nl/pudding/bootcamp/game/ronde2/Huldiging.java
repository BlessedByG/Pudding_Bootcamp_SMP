package nl.pudding.bootcamp.game.ronde2;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde3.WardenEi;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Na het Ei: de winnaar op het podium ({@code ei_podium}), de presentator ernaast
 * ({@code ei_presentator}), de rest verspreid over het plein
 * ({@code ei_plein}), vuurwerk boven het podium, en het Warden-ei in het item frame
 * ({@code ei_prijskader}). Alleen de winnaar of de presentator kan het ei eruit halen.
 */
public final class Huldiging {
	public static final String KADER = "ei_prijskader";
	private static final double KADER_AFSTAND = 5.0;
	private static final int PLEIN_POGINGEN = 50;

	/** Wie het Warden-ei uit het frame mag halen (naast de presentator); {@code null} als er geen winnaar is. */
	private static UUID prijsVoor;

	private Huldiging() {
	}

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				player instanceof ServerPlayer speler && isPrijsKader(entity) ? pak(speler, (ItemFrame) entity) : InteractionResult.PASS);
		// Ook met een klap valt het ei niet uit het frame: dat mag alleen via pakken.
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				player instanceof ServerPlayer speler && isPrijsKader(entity) ? pak(speler, (ItemFrame) entity) : InteractionResult.PASS);
	}

	private static boolean isPrijsKader(Entity entity) {
		return entity instanceof ItemFrame kader && WardenEi.is(kader.getItem());
	}

	/**
	 * Winnaar naar het podium, de presentator ernaast ({@code ei_presentator}) om de prijs uit te
	 * reiken, ook als hij staff is, en de rest verspreid over het plein, kijkend naar het podium.
	 */
	static void opstellen(MinecraftServer server, UUID winnaar) {
		Punt podium = Spel.punt("ei_podium");
		ServerLevel wereld = Mc.wereld(server);
		Regio plein = Spel.regio("ei_plein");
		Set<Long> bezet = new HashSet<>();
		ServerPlayer presentator = null;
		for (ServerPlayer s : Mc.spelers(server)) {
			if (Spel.isPresentator(s) && !s.getUUID().equals(winnaar) && Spel.punt("ei_presentator") != null) {
				presentator = s;
				Spel.naarPunt(s, "ei_presentator");
			}
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s == presentator) {
				continue;
			}
			if (s.getUUID().equals(winnaar) && podium != null) {
				Spel.naarPunt(s, "ei_podium");
				continue;
			}
			BlockPos plek = plein == null ? null : vrijePlek(wereld, plein, bezet);
			if (plek == null) {
				Spel.naarPunt(s, "v3");
				continue;
			}
			bezet.add(plek.asLong());
			double x = plek.getX() + 0.5;
			double z = plek.getZ() + 0.5;
			float yaw = podium == null ? s.getYRot() : (float) Math.toDegrees(Math.atan2(-(podium.x() - x), podium.z() - z));
			Mc.teleport(s, Punt.positie(x, plek.getY(), z, yaw, 0f));
		}
		if (podium != null && winnaar != null) {
			// Een tick later, als iedereen er staat: dan zien ze het ook.
			Planner.na(1, () -> Vuurwerk.goud(wereld, podium.x(), podium.y() + 2, podium.z()));
		}
	}

	/**
	 * Een willekeurige plek op het plein: de selectie is de vloer, je voeten staan er één blok boven,
	 * met twee blokken ruimte en niet op een plek die al bezet is.
	 */
	private static BlockPos vrijePlek(ServerLevel wereld, Regio plein, Set<Long> bezet) {
		List<Regio.Doos> delen = plein.delen();
		if (delen.isEmpty()) {
			return null;
		}
		for (int poging = 0; poging < PLEIN_POGINGEN; poging++) {
			Regio.Doos d = delen.get(Spel.RANDOM.nextInt(delen.size()));
			int x = d.min().x() + Spel.RANDOM.nextInt(d.breedteX());
			int z = d.min().z() + Spel.RANDOM.nextInt(d.breedteZ());
			BlockPos voeten = new BlockPos(x, d.max().y() + 1, z);
			if (bezet.contains(voeten.asLong())) {
				continue;
			}
			if (wereld.getBlockState(voeten).getCollisionShape(wereld, voeten).isEmpty()
					&& wereld.getBlockState(voeten.above()).getCollisionShape(wereld, voeten.above()).isEmpty()) {
				return voeten;
			}
		}
		return null;
	}

	/** Het Warden-ei in het frame; zonder frame krijgt de winnaar het meteen. */
	static void prijsKlaarzetten(MinecraftServer server, UUID winnaar) {
		prijsVoor = winnaar;
		if (winnaar == null) {
			return;
		}
		ItemFrame kader = kader(server);
		if (kader != null) {
			kader.setItem(WardenEi.maak(server));
			return;
		}
		Bootcamp.LOG.warn("Ei: geen item frame op {}; het Warden-ei gaat meteen naar de winnaar", KADER);
		ServerPlayer w = server.getPlayerList().getPlayer(winnaar);
		if (w != null) {
			Kits.geefOfDrop(w, WardenEi.maak(server));
		}
	}

	/** {@code /ei naarmobarena}: ligt het Warden-ei nog in het frame, dan krijgt de winnaar het alsnog. */
	static void prijsAlsnog(MinecraftServer server) {
		ItemFrame kader = kader(server);
		ServerPlayer w = prijsVoor == null ? null : server.getPlayerList().getPlayer(prijsVoor);
		if (kader != null && w != null && WardenEi.is(kader.getItem())) {
			pak(w, kader);
		}
	}

	private static ItemFrame kader(MinecraftServer server) {
		Punt p = Spel.punt(KADER);
		if (p == null) {
			return null;
		}
		BlokPos b = p.blokPos();
		BlockPos pos = new BlockPos(b.x(), b.y(), b.z());
		List<ItemFrame> kaders = Mc.wereld(server).getEntitiesOfClass(ItemFrame.class, new AABB(pos));
		return kaders.isEmpty() ? null : kaders.get(0);
	}

	/** Rechtsklik of klap op het frame met het Warden-ei: de winnaar of de presentator krijgt het. */
	private static InteractionResult pak(ServerPlayer speler, ItemFrame kader) {
		MinecraftServer server = speler.level().getServer();
		boolean mag = speler.getUUID().equals(prijsVoor) || Spel.isPresentator(speler);
		if (!mag) {
			String wie = prijsVoor == null ? "de presentator" : Spel.naamVan(prijsVoor) + " of de presentator";
			Mc.actionbar(speler, Mc.tekst("Alleen " + wie + " kan het Warden-ei pakken", ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		ItemStack ei = kader.getItem().copy();
		kader.setItem(ItemStack.EMPTY);
		Kits.geefOfDrop(speler, ei);
		speler.inventoryMenu.broadcastChanges();
		Mc.geluid(speler, SoundEvents.ITEM_PICKUP, 1f, 1f);
		Mc.chatAllen(server, Component.empty().append(Mc.tekst(Mc.naam(speler), Mc.kleur(Teams.keuze(speler))))
				.append(Mc.tekst(" pakt het Warden-ei", ChatFormatting.DARK_AQUA)));
		return InteractionResult.SUCCESS;
	}

	/**
	 * {@code /ei prijskader}: het item frame waar de speler naar kijkt (binnen 5 blokken) wordt het
	 * frame voor het Warden-ei.
	 *
	 * @return {@code null} als het gelukt is, anders waarom niet
	 */
	public static String zetKader(ServerPlayer speler) {
		Vec3 oog = speler.getEyePosition();
		Vec3 kijk = speler.getViewVector(1f);
		ItemFrame beste = null;
		double besteRichting = 0.95;
		for (ItemFrame f : Mc.wereld(speler.level().getServer()).getEntitiesOfClass(ItemFrame.class,
				speler.getBoundingBox().inflate(KADER_AFSTAND))) {
			Vec3 naar = f.position().subtract(oog);
			if (naar.length() > KADER_AFSTAND) {
				continue;
			}
			double richting = naar.normalize().dot(kijk);
			if (richting > besteRichting) {
				besteRichting = richting;
				beste = f;
			}
		}
		if (beste == null) {
			return "kijk naar een item frame, binnen " + (int) KADER_AFSTAND + " blokken";
		}
		BlockPos pos = beste.blockPosition();
		ConfigStore.get().punten().put(KADER, Punt.blok(new BlokPos(pos.getX(), pos.getY(), pos.getZ())));
		ConfigStore.bewaar();
		return null;
	}
}
