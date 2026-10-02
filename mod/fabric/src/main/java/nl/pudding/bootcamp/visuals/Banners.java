package nl.pudding.bootcamp.visuals;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import nl.pudding.bootcamp.Mc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Banners: een foto op een doek van 3 bij 5 blokken aan een houten stok, die zacht heen en weer
 * wiegt. Het is een item display met {@code item_model} {@code bootcamp:banner_<n>} uit het resource
 * pack (BouwPack maakt het model van {@code aanleveren/banner_<n>}). Eén keer ophangen bij het bouwen;
 * ze horen bij de wereld en blijven dus staan bij {@code /bc reset}.
 */
public final class Banners {
	private static final String TAG = "bootcamp_banner";
	public static final int MAX_NR = 99;
	private static final double KIJKAFSTAND = 32.0;
	private static final double ZOEKAFSTAND = 8.0;
	/** Het model is 16 eenheden hoog (het doek); 5 keer geschaald is dat 5 blokken. */
	private static final float SCHAAL = 5f;
	/** Het midden van de stok boven het midden van het model (modeleenheid 16,5 van de 16 per blok), geschaald. */
	private static final float STOK = (16.5f / 16f - 0.5f) * SCHAAL;
	/** De stok is 1 modeleenheid dik: de helft daarvan, geschaald, zit tussen het blok en het midden van de stok. */
	private static final double STOK_HALF = 0.5 / 16 * SCHAAL;
	/** Zo lang duurt één zwaai (heen of terug), in ticks, en zo ver gaat hij, in graden. */
	private static final int ZWAAI = 50;
	private static final float HOEK = 3f;

	private static final Set<Display.ItemDisplay> GELADEN = Collections.newSetFromMap(new IdentityHashMap<>());

	private Banners() {
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Display.ItemDisplay d && d.entityTags().contains(TAG)) {
				GELADEN.add(d);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof Display.ItemDisplay d) {
				GELADEN.remove(d);
			}
		});
	}

	/**
	 * Hangt banner {@code nr} onder het blok waar de speler naar kijkt (tot 32 blokken), midden onder
	 * dat blok, met de foto naar de speler toe (op een kwartslag afgerond).
	 *
	 * @return {@code null} als het gelukt is, anders waarom niet
	 */
	public static String plaats(ServerPlayer speler, int nr) {
		HitResult hit = speler.pick(KIJKAFSTAND, 0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return "kijk naar het blok waar de banner onder moet hangen (tot " + (int) KIJKAFSTAND + " blokken)";
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		ServerLevel wereld = Mc.wereld(speler.level().getServer());
		ItemStack foto = new ItemStack(Items.PAPER);
		foto.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("bootcamp", "banner_" + nr));
		Display.ItemDisplay banner = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, wereld);
		banner.setItemStack(foto);
		banner.setItemTransform(ItemDisplayContext.NONE);
		banner.setTransformation(transformatie(0f));
		// Altijd goed te zien, ook onder een dak.
		banner.setBrightnessOverride(new Brightness(15, 15));
		banner.setViewRange(4f);
		banner.setPos(pos.getX() + 0.5, pos.getY() - STOK_HALF, pos.getZ() + 0.5);
		banner.setYRot(Math.round(speler.getYRot() / 90f) * 90f);
		banner.addTag(TAG);
		banner.addTag(TAG + "_" + nr);
		wereld.addFreshEntity(banner);
		return null;
	}

	/**
	 * Haalt de banner weg waarvan het midden het dichtst bij de speler is (binnen acht blokken).
	 *
	 * @return het nummer van de banner, of 0 als er geen in de buurt hing
	 */
	public static int verwijderDichtstbij(ServerPlayer speler) {
		Display.ItemDisplay dichtstbij = null;
		double beste = ZOEKAFSTAND * ZOEKAFSTAND;
		for (Display.ItemDisplay b : GELADEN) {
			double afstand = speler.distanceToSqr(b.getX(), b.getY() - SCHAAL / 2, b.getZ());
			if (!b.isRemoved() && afstand < beste) {
				beste = afstand;
				dichtstbij = b;
			}
		}
		if (dichtstbij == null) {
			return 0;
		}
		int nr = nummer(dichtstbij);
		dichtstbij.discard();
		GELADEN.remove(dichtstbij);
		return nr;
	}

	/** De nummers van de banners die nu geladen zijn, voor {@code /bc banner lijst}. */
	public static List<Integer> nummers() {
		List<Integer> uit = new ArrayList<>();
		for (Display.ItemDisplay b : GELADEN) {
			if (!b.isRemoved()) {
				uit.add(nummer(b));
			}
		}
		uit.sort(null);
		return uit;
	}

	private static int nummer(Display.ItemDisplay b) {
		for (String tag : b.entityTags()) {
			if (tag.startsWith(TAG + "_")) {
				try {
					return Integer.parseInt(tag.substring(TAG.length() + 1));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	/**
	 * Elke servertick: elke banner zwaait om de {@link #ZWAAI} ticks de andere kant op; de client
	 * schuift er vloeiend naartoe. Elke banner heeft zijn eigen moment, zodat ze niet in de maat gaan.
	 */
	public static void tick(MinecraftServer server) {
		for (Display.ItemDisplay b : GELADEN) {
			int t = server.getTickCount() + Math.floorMod(b.getUUID().hashCode(), 2 * ZWAAI);
			if (b.isRemoved() || t % ZWAAI != 0) {
				continue;
			}
			boolean heen = (t / ZWAAI) % 2 == 0;
			// De client begint alleen opnieuw te schuiven als de startwaarde verandert: om en om 0 en -1
			// (een tick eerder begonnen, dat zie je niet).
			b.setTransformationInterpolationDelay(heen ? 0 : -1);
			b.setTransformationInterpolationDuration(ZWAAI);
			b.setTransformation(transformatie(heen ? HOEK : -HOEK));
		}
	}

	/**
	 * Vijf keer zo groot, gekanteld om de stok: de stok blijft op de plek van de entity, het doek
	 * zwaait eronder heen en weer.
	 */
	private static Transformation transformatie(float graden) {
		Quaternionf draai = new Quaternionf().rotateX((float) Math.toRadians(graden));
		Vector3f stokNaarNul = new Vector3f(0, -STOK, 0).rotate(draai);
		return new Transformation(stokNaarNul, draai, new Vector3f(SCHAAL, SCHAAL, SCHAAL), null);
	}
}
