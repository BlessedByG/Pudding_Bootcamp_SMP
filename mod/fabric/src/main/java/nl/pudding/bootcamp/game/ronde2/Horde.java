package nl.pudding.bootcamp.game.ronde2;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.HordeVerloop;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.WavesDef;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ronde 2: overleef als groep vijf waves mobs. Doodgaan is kijker tot het einde van de ronde, met
 * je spullen bewaard. Aan het eind gaat iedereen door naar {@code v3}, dood of levend.
 */
public final class Horde extends RondeLogica {
	/** Entity-tag op elke mob van de horde, zodat {@code kill @e[tag=horde]} een wave forceert. */
	public static final String TAG = "horde";
	private static final String POORT = "arena";
	private static final int AANTAL_SPAWNPUNTEN = 4;
	private static final double VOLGBEREIK = 64.0;
	private static final double SPREIDING = 2.0;

	private WavesDef waves;
	private HordeVerloop verloop;
	private final List<Mob> mobs = new ArrayList<>();
	private final List<String> gesneuveld = new ArrayList<>();
	private int spelersBijStart;
	private int volgendSpawnpunt;

	public static void init() {
		Reset.REGISTER.registreer("horde-mobs", Horde::ruimMobsOp);
		// Horde-mobs zijn persistent en worden dus met de wereld opgeslagen. Na een crash midden in
		// ronde 2 mogen ze niet terugkomen zodra iemand de arena inloopt.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reden, vanSchijf) ->
				!(vanSchijf && entity.entityTags().contains(TAG) && !(Spel.actief() instanceof Horde)));
	}

	@Override
	public Ronde ronde() {
		return Ronde.HORDE;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (Mc.wereld(server).getDifficulty() == Difficulty.PEACEFUL) {
			return "de server staat op peaceful: daar spawnen geen mobs. Zet de difficulty op easy of hoger";
		}
		try {
			waves = leesWaves();
		} catch (IllegalArgumentException e) {
			Bootcamp.LOG.error("Horde: {}", e.getMessage());
			return e.getMessage();
		}
		// Elk mob-type en elk stuk gear moet bestaan vóórdat de ronde begint, niet halverwege wave 3.
		for (WavesDef.Wave wave : waves.waves()) {
			for (WavesDef.Mob mob : wave.mobs()) {
				if (type(mob.type()) == null) {
					return WavesDef.BESTAND + ": '" + mob.type() + "' is geen entity";
				}
				for (Map.Entry<String, String> g : mob.gear().entrySet()) {
					try {
						Items26.parse(server.registryAccess(), g.getValue(), 1);
					} catch (CommandSyntaxException e) {
						return WavesDef.BESTAND + ": '" + g.getValue() + "' is geen geldig item (" + e.getMessage() + ")";
					}
				}
			}
		}
		String fout = Kits.controleer(server, "horde");
		if (fout == null) {
			fout = Spel.buitenRegio("arena", "arena_spawn", "mob_1", "mob_2", "mob_3", "mob_4");
		}
		if (fout == null) {
			fout = Spel.binnenRegio("arena", "tribune_horde_1", "tribune_horde_2");
		}
		return fout;
	}

	private static WavesDef leesWaves() {
		Path pad = Standaardbestanden.map().resolve(WavesDef.BESTAND);
		try {
			return WavesDef.uitJson(Files.readString(pad, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalArgumentException(WavesDef.BESTAND + ": niet te lezen in " + pad + " (" + e.getMessage() + ")");
		}
	}

	@Override
	public void start(MinecraftServer server) {
		ruimMobsOp(server);
		Poorten.dichtAlsHijBestaat(server, POORT);
		Spelregels.locatorBar(server, false);
		Spel.maakSpelers(server, false);
		List<ServerPlayer> spelers = Mc.deelnemers(server);
		spelersBijStart = spelers.size();
		for (ServerPlayer s : spelers) {
			Spel.naarPunt(s, "arena_spawn");
		}
		Border.zet(server, Spel.regio("arena"));
		Kits.geefAan(server, "horde", spelers);
		verloop = new HordeVerloop(waves.waves().size());
		Sidebar.toon(server, "Gesneuveld", gesneuveld);
		Aftelling.start(5, "De horde komt over", () -> {
			Poorten.openAlsHijBestaat(server, POORT);
			Spel.startTimer(ronde().duurSeconden());
		});
	}

	@Override
	public void seconde(MinecraftServer server) {
		if (!Spel.timerLoopt() || verloop == null) {
			return;
		}
		mobs.removeIf(m -> !m.isAlive());
		int levend = Spel.levend(server, Rol.SPELER).size();

		switch (verloop.seconde(mobs.size(), levend)) {
			case START_WAVE -> startWave(server);
			case GEWONNEN -> {
				einde(server, "DE HORDE IS VERSLAGEN", ChatFormatting.GREEN);
				return;
			}
			case IEDEREEN_DOOD -> {
				einde(server, "DE HORDE WINT", ChatFormatting.RED);
				return;
			}
			case NIKS -> {
			}
		}
		Bossbar.zet(BossbarTekst.horde(verloop.wave(), mobs.size()), BossEvent.BossBarColor.RED,
				waveTotaal() <= 0 ? 0f : (float) mobs.size() / waveTotaal());
	}

	private int waveTotaal() {
		int w = verloop.wave();
		return w <= 0 ? 0 : waves.waves().get(w - 1).totaal(spelersBijStart);
	}

	private void startWave(MinecraftServer server) {
		WavesDef.Wave wave = waves.waves().get(verloop.wave() - 1);
		ServerLevel wereld = Mc.wereld(server);
		List<ServerPlayer> doelen = Spel.levend(server, Rol.SPELER);
		for (WavesDef.Mob def : wave.mobs()) {
			int aantal = Regels.schaalMobs(def.aantal(), spelersBijStart, wave.schaal());
			for (int i = 0; i < aantal; i++) {
				spawn(server, wereld, def, doelen);
			}
		}
		Mc.titleAllen(server, Mc.tekst("WAVE " + verloop.wave(), ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst(wave.naam(), ChatFormatting.GRAY));
		Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
	}

	private void spawn(MinecraftServer server, ServerLevel wereld, WavesDef.Mob def, List<ServerPlayer> doelen) {
		EntityType<?> type = type(def.type());
		Entity entity = type == null ? null : type.create(wereld, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob mob)) {
			Bootcamp.LOG.warn("Horde: {} is geen mob en wordt overgeslagen", def.type());
			if (entity != null) {
				entity.discard();
			}
			return;
		}
		// Verspreid over de vier punten, met wat ruimte zodat ze niet in elkaar staan.
		Punt p = Spel.punt("mob_" + (Math.floorMod(volgendSpawnpunt++, AANTAL_SPAWNPUNTEN) + 1));
		double x = p.x() + (p.blok() ? 0.5 : 0) + (Spel.RANDOM.nextDouble() * 2 - 1) * SPREIDING;
		double z = p.z() + (p.blok() ? 0.5 : 0) + (Spel.RANDOM.nextDouble() * 2 - 1) * SPREIDING;
		double y = p.y() + (p.blok() ? 1 : 0);
		mob.snapTo(x, y, z, Spel.RANDOM.nextFloat() * 360f, 0f);

		mob.finalizeSpawn(wereld, wereld.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), EntitySpawnReason.EVENT, null);
		mob.setBaby(false);
		mob.setPersistenceRequired();
		mob.addTag(TAG);

		for (Map.Entry<String, String> g : def.gear().entrySet()) {
			EquipmentSlot slot = EquipmentSlot.byName(g.getKey());
			try {
				mob.setItemSlot(slot, Items26.parse(server.registryAccess(), g.getValue(), 1));
				mob.setDropChance(slot, 0f);
			} catch (CommandSyntaxException e) {
				// In magStarten al gecontroleerd; het bestand is tijdens de ronde aangepast.
				Bootcamp.LOG.warn("Horde: gear '{}' is ongeldig", g.getValue());
			}
		}
		// De ruïne-arena ligt in de open lucht: zonder iets op hun hoofd branden zombies en
		// skeletons overdag weg. Een knoop zie je niet en houdt de zon tegen.
		if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.STONE_BUTTON));
			mob.setDropChance(EquipmentSlot.HEAD, 0f);
		}

		// Vanaf de rand van een arena van 50 zien de meeste mobs de spelers in het midden niet.
		AttributeInstance bereik = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (bereik != null) {
			bereik.setBaseValue(VOLGBEREIK);
		}
		if (!doelen.isEmpty()) {
			mob.setTarget(doelen.get(Spel.RANDOM.nextInt(doelen.size())));
		}

		wereld.addFreshEntity(mob);
		mobs.add(mob);
	}

	private static EntityType<?> type(String id) {
		Identifier sleutel = Identifier.tryParse(id);
		if (sleutel == null) {
			return null;
		}
		return BuiltInRegistries.ENTITY_TYPE.get(sleutel).map(Holder::value).orElse(null);
	}

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		sneuvel(server, speler, true);
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		gesneuveld.add((gesneuveld.size() + 1) + ". " + Mc.naam(speler));
		Sidebar.toon(server, "Gesneuveld", gesneuveld);
	}

	private void sneuvel(MinecraftServer server, ServerPlayer speler, boolean doodtekst) {
		SpelerStatus st = Spel.status(speler);
		st.dood = true;
		gesneuveld.add((gesneuveld.size() + 1) + ". " + st.naam);
		Sidebar.toon(server, "Gesneuveld", gesneuveld);
		Tribune.maakKijker(server, speler, Tribune.Spullen.BEWAREN, doodtekst);
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server, "DE TIJD IS OM", ChatFormatting.GOLD);
	}

	private void einde(MinecraftServer server, String titel, ChatFormatting kleur) {
		List<ServerPlayer> overlevers = Spel.levend(server, Rol.SPELER);
		Spel.stop(server);

		// Iedereen gaat door, dood of levend. De doden hebben in end() hun spullen al terug.
		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.status(s).klaar = true;
			Spel.naarPunt(s, "v3");
		}
		// Bonus: wie de ronde overleeft krijgt een ender pearl.
		for (ServerPlayer s : overlevers) {
			Kits.geefOfDrop(s, new ItemStack(Items.ENDER_PEARL));
		}
		Mc.titleAllen(server, Mc.tekst(titel, kleur, ChatFormatting.BOLD),
				Mc.tekst(overlevers.size() + " overlevers krijgen een ender pearl", ChatFormatting.WHITE));
	}

	@Override
	public void end(MinecraftServer server) {
		ruimMobsOp(server);
		mobs.clear();
		// Ook bij /bc stop: wie dood was krijgt zijn bewaarde spullen terug.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Tribune.geefBewaardTerug(s);
		}
		Sidebar.weg(server);
		Poorten.dichtAlsHijBestaat(server, POORT);
	}

	/** Alle mobs met de tag, ook van een vorige serverrun, plus de vexes die evokers hebben opgeroepen. */
	public static void ruimMobsOp(MinecraftServer server) {
		Regio arena = Spel.regio("arena");
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			boolean vexInArena = e instanceof Vex && arena != null && arena.bevat(e.getX(), e.getZ());
			if (e.entityTags().contains(TAG) || vexInArena) {
				weg.add(e);
			}
		}
		weg.forEach(Entity::discard);
	}
}
