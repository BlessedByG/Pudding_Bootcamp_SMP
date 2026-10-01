package nl.pudding.bootcamp.game.ronde3;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.MobPunten;
import nl.pudding.bootcamp.core.MobSchema;
import nl.pudding.bootcamp.core.MobVerloop;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.WavesDef;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 3: de mob arena. Eén veld; per beurt staan er van elk team twee spelers tegelijk in, elk op
 * een eigen startplek. Tot de countdown voorbij is staan ze stil. Vijf waves, punten per kill voor
 * het team. Wie sneuvelt gaat de kooi in, is af voor de rest van de ronde en is zijn spullen kwijt.
 * Het schema is geheim: je merkt pas dat je aan de beurt bent als je naar je plek wordt
 * geteleporteerd. Met het Warden-ei ({@link WardenEi}) komt er meteen een warden bij in de lopende
 * wave.
 */
public final class MobArena extends RondeLogica {
	public static final String TAG = "bootcamp_mob";
	private static final double VOLGBEREIK = 64.0;
	private static final double SPREIDING = 1.5;
	private static final int TARGET_ELKE_TICKS = 2;
	private static final int VELD_ELKE_TICKS = 10;
	/** Zolang dit er is graaft de warden zich niet in; elke paar ticks opnieuw gezet. */
	private static final long GRAAF_NIET = 1200L;

	private enum Fase {
		/** Wacht op {@code /mobarena volgende}. */
		WACHT,
		/** De countdown voor de waves; wie aan de beurt is staat stil. */
		COUNTDOWN,
		WAVES,
		/** Tien seconden na de beurt: vieren, dan naar de tribune. */
		VIEREN,
		/** Na de laatste beurt: de winnaar. */
		EINDE
	}

	private WavesDef waves;
	private MobSchema schema;
	private final MobPunten punten = new MobPunten();
	private MobVerloop verloop;
	private Fase fase = Fase.WACHT;
	/** De lopende beurt, vanaf 0. */
	private int beurt = -1;
	private final List<Mob> mobs = new ArrayList<>();
	private int spawnTeller;
	private final List<Kleur> meedoend = new ArrayList<>();
	/** Wie deze beurt in het veld staat, met zijn kleur en startplek. */
	private final List<MobSchema.Plek> opstelling = new ArrayList<>();

	public static void init() {
		Reset.REGISTER.registreer("mobs van de mob arena", MobArena::ruimMobsOp);
		// Mobs van de mob arena zijn persistent en worden met de wereld opgeslagen. Na een crash mogen
		// ze niet terugkomen zodra iemand de arena inloopt.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reden, vanSchijf) ->
				!(vanSchijf && entity.entityTags().contains(TAG) && !(Spel.actief() instanceof MobArena)));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, bron) -> {
			// Ook de vexes van een evoker: elk ander type is 1 punt.
			if ((entity.entityTags().contains(TAG) || entity instanceof Vex) && Spel.actief() instanceof MobArena ronde) {
				ronde.kill(entity, bron);
			}
		});
		// Wie op zijn beurt wacht, mag vanaf de tribune niet meeschieten.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, bron, schade) -> {
			if (!entity.entityTags().contains(TAG) || !(Spel.actief() instanceof MobArena)) {
				return true;
			}
			ServerPlayer aanvaller = Tribune.aanvaller(bron);
			return aanvaller == null || Mc.isStaff(aanvaller) || Spel.status(aanvaller).arena > 0;
		});
		WardenEi.init();
	}

	@Override
	public Ronde ronde() {
		return Ronde.MOBARENA;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (Mc.wereld(server).getDifficulty() == Difficulty.PEACEFUL) {
			return "de server staat op peaceful: daar spawnen geen mobs. Zet de difficulty op easy of hoger";
		}
		try {
			waves = leesWaves();
		} catch (IllegalArgumentException e) {
			Bootcamp.LOG.error("Mob arena: {}", e.getMessage());
			return e.getMessage();
		}
		// Elk mob-type en elk stuk gear moet bestaan vóórdat de ronde begint, niet halverwege wave 3.
		for (WavesDef.Wave wave : waves.waves()) {
			for (WavesDef.Mob mob : wave.mobs()) {
				if (type(mob.type()) == null) {
					return WavesDef.BESTAND + ": '" + mob.type() + "' is geen entity";
				}
				for (String g : mob.gear().values()) {
					try {
						Items26.parse(server.registryAccess(), g, 1);
					} catch (CommandSyntaxException e) {
						return WavesDef.BESTAND + ": '" + g + "' is geen geldig item (" + e.getMessage() + ")";
					}
				}
			}
		}
		List<String> zonder = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (Teams.keuze(s) == null) {
				zonder.add(Mc.naam(s));
			}
		}
		if (!zonder.isEmpty()) {
			return "deze spelers hebben geen team (/bc team <speler> <kleur>): " + String.join(", ", zonder);
		}
		List<String> punten = new ArrayList<>(Spel.reeks("mob_"));
		punten.addAll(List.of("kooi", "warden"));
		punten.addAll(startplekken());
		String fout = Spel.buitenRegio("mobarena", punten);
		if (fout != null) {
			return fout;
		}
		return tribuneKlopt();
	}

	/** {@code start_rood_1}, {@code start_rood_2}, ... voor elke kleur. */
	private static List<String> startplekken() {
		List<String> uit = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			for (int n = 1; n <= MobSchema.PER_BEURT; n++) {
				uit.add(startplek(k, n));
			}
		}
		return uit;
	}

	private static String startplek(Kleur k, int nummer) {
		return "start_" + k.id() + "_" + nummer;
	}

	/**
	 * Op regio {@code tribune_mob} wordt niemand van het veld gezet. Een tribuneplek ernaast zet wie
	 * er staat elke halve seconde terug als hij in het veld valt; een startplek erop zet wie aan de
	 * beurt is steeds terug naar diezelfde plek.
	 */
	private static String tribuneKlopt() {
		List<String> naast = new ArrayList<>();
		for (String t : Spel.reeks("tribune_mob_")) {
			Punt p = Spel.punt(t);
			if (!Tribune.opMobTribune(p.x(), p.y(), p.z())) {
				naast.add(t);
			}
		}
		if (!naast.isEmpty()) {
			return "deze tribuneplekken liggen niet op regio tribune_mob: " + String.join(", ", naast)
					+ ". Selecteer de vloer van de tribune (/bc region save tribune_mob, en add voor meer delen)";
		}
		List<String> erop = new ArrayList<>();
		for (String naam : startplekken()) {
			Punt p = Spel.punt(naam);
			if (p != null && Tribune.opMobTribune(p.x(), p.y(), p.z())) {
				erop.add(naam);
			}
		}
		return erop.isEmpty() ? null : "deze startplekken liggen op regio tribune_mob, waar wie aan de beurt is juist af moet blijven: "
				+ String.join(", ", erop);
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
		Spelregels.locatorBar(server, false);
		// Geen kit: iedereen speelt met wat hij uit het doolhof heeft.
		Map<Kleur, List<UUID>> teams = new EnumMap<>(Kleur.class);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.zetRol(server, s, Rol.SPELER);
			s.setGameMode(GameType.ADVENTURE);
			Mc.heal(s);
			Tribune.naarTribune(s, Ronde.MOBARENA);
			Kleur k = Teams.keuze(s);
			if (k != null) {
				teams.computeIfAbsent(k, x -> new ArrayList<>()).add(s.getUUID());
			}
		}
		for (Kleur k : Kleur.values()) {
			if (teams.containsKey(k)) {
				meedoend.add(k);
			}
		}
		Border.zet(server, Spel.regio("mobarena"));
		schema = MobSchema.loot(teams, Spel.RANDOM);
		Bootcamp.LOG.info("Mob arena: schema geloot, {} beurten", schema.beurten());
		toonSidebar(server);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		startBeurt(server);
	}

	// Beurten

	/** {@code /mobarena volgende}: weigert zolang de huidige beurt nog loopt. */
	public String volgende(MinecraftServer server) {
		if (fase != Fase.WACHT) {
			return switch (fase) {
				case COUNTDOWN, WAVES -> "beurt " + (beurt + 1) + " loopt nog";
				case VIEREN -> "beurt " + (beurt + 1) + " is net klaar; nog even vieren";
				default -> "de mob arena is voorbij";
			};
		}
		if (beurt + 1 >= schema.beurten()) {
			return "alle " + schema.beurten() + " beurten zijn gespeeld";
		}
		startBeurt(server);
		return null;
	}

	private void startBeurt(MinecraftServer server) {
		beurt++;
		opstelling.clear();
		Set<UUID> af = new HashSet<>();
		List<UUID> aanwezig = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			aanwezig.add(s.getUUID());
		}
		for (SpelerStatus st : Spel.alleStatussen()) {
			if (st.dood) {
				af.add(st.id);
			}
		}
		opstelling.addAll(schema.opstelling(beurt, af, aanwezig, Spel.RANDOM));
		for (MobSchema.Plek p : opstelling) {
			ServerPlayer s = p.speler() == null ? null : server.getPlayerList().getPlayer(p.speler());
			if (s == null) {
				continue;
			}
			SpelerStatus st = Spel.status(s);
			st.arena = 1;
			st.tribunepunt = null;
			Spel.naarPunt(s, startplek(p.kleur(), p.nummer()));
			Mc.heal(s);
			// Glowing in de teamkleur: de outline volgt de kleur van het team.
			Mc.effect(s, MobEffects.GLOWING, -1, 0);
			// Stil tot de countdown voorbij is; rondkijken kan wel.
			Opstelling.bevries(s);
		}
		fase = Fase.COUNTDOWN;
		Mc.titleAllen(server, Mc.tekst("BEURT " + (beurt + 1), ChatFormatting.GOLD, ChatFormatting.BOLD), null, 5, 40, 10);
		Bossbar.zet(BossbarTekst.mobarena(beurt + 1, schema.beurten(), 0), BossEvent.BossBarColor.RED, 1f);
		// Eerst twee seconden BEURT n in beeld, dan de countdown (die zet zijn eigen titles).
		int dezeBeurt = beurt;
		Planner.naSeconden(2, () -> {
			if (Spel.actief() != this || beurt != dezeBeurt) {
				return;
			}
			Aftelling.start(Regels.COUNTDOWN, "De beurt begint over", () -> {
				verloop = new MobVerloop(waves.waves().size());
				fase = Fase.WAVES;
				for (ServerPlayer s : Mc.deelnemers(server)) {
					if (Spel.status(s).bevroren) {
						Opstelling.ontdooi(s);
					}
				}
			});
		});
	}

	@Override
	public void seconde(MinecraftServer server) {
		// Meldingen in de actionbar blijven zolang ze lopen (na een kill, na een dood).
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Component m = Spel.melding(s);
			if (m != null) {
				Mc.actionbar(s, m);
			}
		}
		if (fase != Fase.WAVES || verloop == null) {
			return;
		}
		mobs.removeIf(m -> !m.isAlive());
		// Zolang de warden leeft, is de wave pas klaar als hij dood is: geen limiet van twee minuten.
		if (wardenLeeft()) {
			verloop.zonderLimiet();
		}
		for (MobVerloop.Gebeurtenis g : verloop.seconde(mobs.size(), spelersInVeld(server))) {
			switch (g.soort()) {
				case START_WAVE -> startWave(server);
				case WAVE_GEFORCEERD -> ruimVeld(server);
				case BEURT_KLAAR -> {
					beurtKlaar(server);
					return;
				}
			}
		}
		int totaal = totaalDezeWave();
		Bossbar.zet(BossbarTekst.mobarena(beurt + 1, schema.beurten(), verloop.wave()), BossEvent.BossBarColor.RED,
				totaal <= 0 ? 0f : Math.min(1f, (float) mobs.size() / totaal));
	}

	/** Hoeveel mobs de lopende wave had, met de warden erbij als die meedoet. */
	private int totaalDezeWave() {
		if (verloop.wave() <= 0) {
			return 0;
		}
		return waves.waves().get(verloop.wave() - 1).totaal() + (wardenLeeft() ? 1 : 0);
	}

	/** Loopt de warden van het Warden-ei nog rond in het veld? */
	private boolean wardenLeeft() {
		return mobs.stream().anyMatch(m -> m instanceof Warden && m.isAlive());
	}

	private static int spelersInVeld(MinecraftServer server) {
		int n = 0;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			if (st.arena > 0 && !st.dood) {
				n++;
			}
		}
		return n;
	}

	private void startWave(MinecraftServer server) {
		ServerLevel wereld = Mc.wereld(server);
		WavesDef.Wave wave = waves.waves().get(verloop.wave() - 1);
		// Eerst een rookwolk op elk spawnpunt, dan de mobs.
		for (String p : Spel.reeks("mob_")) {
			Punt pt = Spel.punt(p);
			double x = pt.x() + (pt.blok() ? 0.5 : 0);
			double y = pt.y() + (pt.blok() ? 1 : 0);
			double z = pt.z() + (pt.blok() ? 0.5 : 0);
			Mc.particles(wereld, ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 40, 0.6, 0.02);
			wereld.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
		}
		for (WavesDef.Mob def : wave.mobs()) {
			for (int i = 0; i < def.aantal(); i++) {
				spawn(server, wereld, def);
			}
		}
		Mc.titleAllen(server, Mc.tekst("WAVE " + verloop.wave(), ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst(wave.naam(), ChatFormatting.GRAY), 5, 40, 10);
		Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
	}

	private void spawn(MinecraftServer server, ServerLevel wereld, WavesDef.Mob def) {
		List<String> spawns = Spel.reeks("mob_");
		if (spawns.isEmpty()) {
			return;
		}
		EntityType<?> type = type(def.type());
		Entity entity = type == null ? null : type.create(wereld, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob mob)) {
			Bootcamp.LOG.warn("Mob arena: {} is geen mob en wordt overgeslagen", def.type());
			if (entity != null) {
				entity.discard();
			}
			return;
		}
		// Om en om over de spawns, met wat ruimte zodat ze niet in elkaar staan.
		Punt p = Spel.punt(spawns.get(Math.floorMod(spawnTeller++, spawns.size())));
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
				Bootcamp.LOG.warn("Mob arena: gear '{}' is ongeldig", g.getValue());
			}
		}
		// Zonder iets op hun hoofd branden zombies en skeletons overdag weg. Een knoop zie je niet.
		if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.STONE_BUTTON));
			mob.setDropChance(EquipmentSlot.HEAD, 0f);
		}
		AttributeInstance bereik = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (bereik != null) {
			bereik.setBaseValue(VOLGBEREIK);
		}
		ServerPlayer doel = dichtstbijzijnde(server, mob);
		if (doel != null) {
			mob.setTarget(doel);
		}
		wereld.addFreshEntity(mob);
		mobs.add(mob);
	}

	/**
	 * De warden uit het Warden-ei: op punt {@code warden}, uit de grond (spawnreden TRIGGERED geeft de
	 * graaf-animatie), met de levens en klap uit {@code /mobarena warden}.
	 */
	private boolean spawnWarden(MinecraftServer server, ServerLevel wereld) {
		Punt p = Spel.punt("warden");
		if (p == null || !(type("minecraft:warden") instanceof EntityType<?> type)
				|| !(type.create(wereld, EntitySpawnReason.TRIGGERED) instanceof Warden warden)) {
			Bootcamp.LOG.warn("Mob arena: de warden kon niet spawnen (punt warden ontbreekt?)");
			return false;
		}
		double x = p.x() + (p.blok() ? 0.5 : 0);
		double y = p.y() + (p.blok() ? 1 : 0);
		double z = p.z() + (p.blok() ? 0.5 : 0);
		warden.snapTo(x, y, z, p.yaw(), 0f);
		warden.finalizeSpawn(wereld, wereld.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), EntitySpawnReason.TRIGGERED, null);
		Instellingen i = Spel.instellingen();
		AttributeInstance leven = warden.getAttribute(Attributes.MAX_HEALTH);
		if (leven != null) {
			leven.setBaseValue(i.warden(Instellingen.WardenWaarde.LEVEN));
		}
		warden.setHealth(i.warden(Instellingen.WardenWaarde.LEVEN));
		AttributeInstance klap = warden.getAttribute(Attributes.ATTACK_DAMAGE);
		if (klap != null) {
			klap.setBaseValue(i.warden(Instellingen.WardenWaarde.KLAP));
		}
		AttributeInstance bereik = warden.getAttribute(Attributes.FOLLOW_RANGE);
		if (bereik != null) {
			bereik.setBaseValue(VOLGBEREIK);
		}
		warden.setPersistenceRequired();
		warden.addTag(TAG);
		warden.addTag(WardenEi.TAG);
		wereld.addFreshEntity(warden);
		mobs.add(warden);
		return true;
	}

	private static EntityType<?> type(String id) {
		Identifier sleutel = Identifier.tryParse(id);
		if (sleutel == null) {
			return null;
		}
		return BuiltInRegistries.ENTITY_TYPE.get(sleutel).map(Holder::value).orElse(null);
	}

	/** De dichtstbijzijnde speler die aan de beurt is en nog leeft. */
	private static ServerPlayer dichtstbijzijnde(MinecraftServer server, Entity mob) {
		ServerPlayer beste = null;
		double afstand = VOLGBEREIK * VOLGBEREIK;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			if (st.arena == 0 || st.dood) {
				continue;
			}
			double d = s.distanceToSqr(mob);
			if (d < afstand) {
				afstand = d;
				beste = s;
			}
		}
		return beste;
	}

	/** Mag deze mob dit doel hebben? Alleen spelers die aan de beurt zijn. */
	private static boolean goedDoel(LivingEntity doel) {
		if (!(doel instanceof ServerPlayer s)) {
			return !(doel instanceof Player);
		}
		if (Mc.isStaff(s)) {
			return false;
		}
		SpelerStatus st = Spel.status(s);
		return st.arena > 0 && !st.dood;
	}

	// Elke paar ticks: doelen en het veld

	@Override
	public void tick(MinecraftServer server) {
		int t = server.getTickCount();
		if (t % TARGET_ELKE_TICKS == 0 && fase == Fase.WAVES) {
			houdKijkersBuiten(server);
		}
		if (t % VELD_ELKE_TICKS == 0 && (fase == Fase.WAVES || fase == Fase.COUNTDOWN)) {
			houdSpelersInHetVeld(server);
		}
	}

	/**
	 * Mobs laten kijkers met rust: een mob met een kijker, staff of een speler die niet aan de beurt
	 * is als doel verliest dat doel en kiest de dichtstbijzijnde speler die aan de beurt is. Ook de
	 * vexes van een evoker, en de warden. Het publiek krijgt ook geen Darkness van de warden.
	 */
	private void houdKijkersBuiten(MinecraftServer server) {
		for (Mob m : mobs) {
			richt(server, m);
		}
		Regio arena = Spel.regio("mobarena");
		if (arena != null) {
			Regio.Doos d = arena.omhullende();
			AABB doos = new AABB(d.min().x(), d.min().y() - 16, d.min().z(), d.max().x() + 1, d.max().y() + 32, d.max().z() + 1);
			for (Vex vex : Mc.wereld(server).getEntitiesOfClass(Vex.class, doos)) {
				richt(server, vex);
			}
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (Spel.status(s).arena == 0 && s.hasEffect(MobEffects.DARKNESS)) {
				s.removeEffect(MobEffects.DARKNESS);
			}
		}
	}

	private static void richt(MinecraftServer server, Mob m) {
		if (!m.isAlive()) {
			return;
		}
		if (m instanceof Warden w) {
			richtWarden(server, w);
			return;
		}
		LivingEntity doel = m.getTarget();
		if (doel != null && !goedDoel(doel)) {
			m.setTarget(null);
			doel = null;
		}
		if (doel == null) {
			ServerPlayer nieuw = dichtstbijzijnde(server, m);
			if (nieuw != null) {
				m.setTarget(nieuw);
			}
		}
	}

	/**
	 * De warden kiest zijn doel met woede, niet met {@code setTarget}. Woede op wie niet aan de beurt
	 * is gaat weg, op de dichtstbijzijnde speler in het veld gaat hij omhoog. En hij graaft zich niet
	 * in zolang de wave loopt.
	 */
	private static void richtWarden(MinecraftServer server, Warden w) {
		w.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, GRAAF_NIET);
		LivingEntity doel = w.getTarget();
		if (doel != null && !goedDoel(doel)) {
			w.clearAnger(doel);
			w.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
			doel = null;
		}
		if (doel == null) {
			ServerPlayer nieuw = dichtstbijzijnde(server, w);
			if (nieuw != null) {
				w.increaseAngerAt(nieuw, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
				w.setAttackTarget(nieuw);
			}
		}
	}

	/** Een speler die aan de beurt is en het veld uit komt of de tribune op loopt, gaat terug naar zijn plek. */
	private void houdSpelersInHetVeld(MinecraftServer server) {
		Regio veld = Spel.regio("veld");
		if (veld == null) {
			return;
		}
		for (MobSchema.Plek p : opstelling) {
			ServerPlayer s = p.speler() == null ? null : server.getPlayerList().getPlayer(p.speler());
			if (s == null) {
				continue;
			}
			SpelerStatus st = Spel.status(s);
			if (st.arena > 0 && !st.dood
					&& (!veld.bevat(s.getX(), s.getZ()) || Tribune.opMobTribune(s.getX(), s.getY(), s.getZ()))) {
				Spel.naarPunt(s, startplek(p.kleur(), p.nummer()));
			}
		}
	}

	// Het Warden-ei

	/**
	 * Het Warden-ei is ingezet: de warden komt meteen uit de grond, bovenop de lopende wave. Die wave
	 * (en dus de beurt) is pas klaar als de warden ook dood is. Kan alleen terwijl er gevochten wordt;
	 * anders houdt hij het ei.
	 *
	 * @return {@code null} als het gelukt is, anders waarom niet (dan houdt hij het ei)
	 */
	public String wardenInzetten(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.COUNTDOWN) {
			return "wacht tot de beurt begonnen is";
		}
		if (fase != Fase.WAVES || verloop == null || verloop.beurtKlaar()) {
			return "er loopt geen beurt";
		}
		if (wardenLeeft()) {
			return "er loopt al een warden rond";
		}
		if (!spawnWarden(server, Mc.wereld(server))) {
			return "de warden kan niet spawnen (punt warden ontbreekt?)";
		}
		verloop.zonderLimiet();
		String van = Mc.naam(speler);
		Kleur k = Teams.keuze(speler);
		Mc.titleAllen(server, Mc.tekst("WARDEN-EI", ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
				Component.empty().append(Mc.tekst(van, Mc.kleur(k)))
						.append(Mc.tekst(" zet hem in: de warden komt eraan", ChatFormatting.WHITE)), 5, 60, 10);
		Mc.geluidAllen(server, SoundEvents.WARDEN_EMERGE, 1f, 1f);
		Mc.chatAllen(server, Component.empty().append(Mc.tekst(van, Mc.kleur(k)))
				.append(Mc.tekst(" zette het Warden-ei in", ChatFormatting.DARK_AQUA)));
		return null;
	}

	// Kills en doden

	private void kill(LivingEntity mob, DamageSource bron) {
		MinecraftServer server = mob.level().getServer();
		ServerPlayer killer = Tribune.aanvaller(bron);
		if (killer == null && mob.getLastHurtByPlayer() instanceof ServerPlayer p) {
			killer = p;
		}
		if (killer == null || Mc.isStaff(killer) || Spel.status(killer).arena == 0) {
			return;
		}
		Kleur team = Teams.keuze(killer);
		if (team == null) {
			return;
		}
		String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
		int erbij = punten.kill(team, type, Spel.instellingen());
		killDeeltjes(mob, team);
		Mc.geluid(killer, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f);
		Spel.melding(killer, Component.empty()
				.append(Mc.tekst("+" + erbij, ChatFormatting.GREEN, ChatFormatting.BOLD))
				.append(Mc.tekst(" · " + team.naam() + " " + punten.punten(team), Mc.kleur(team))), 2);
		Mc.actionbar(killer, Spel.melding(killer));
		String kort = Instellingen.mobSleutel(type);
		if (kort.equals("evoker") || kort.equals("ravager") || kort.equals("warden")) {
			Mc.chatAllen(server, Component.empty().append(Mc.tekst(Mc.naam(killer), Mc.kleur(team)))
					.append(Mc.tekst(" killde de " + kort + " (+" + erbij + ")", ChatFormatting.GOLD)));
		}
		toonSidebar(server);
	}

	/**
	 * Deeltjes in de teamkleur van de killer rond de mob: een wolk zo groot als de mob en een ring om
	 * zijn voeten. Grotere mobs krijgen er meer. Iedereen ziet ze, ook de tribune.
	 */
	private static void killDeeltjes(LivingEntity mob, Kleur team) {
		if (!(mob.level() instanceof ServerLevel wereld)) {
			return;
		}
		DustParticleOptions stof = new DustParticleOptions(team.rgb(), 1.5f);
		double breed = mob.getBbWidth();
		double hoog = mob.getBbHeight();
		int wolk = (int) Math.min(80, 16 + breed * hoog * 12);
		wereld.sendParticles(stof, true, true, mob.getX(), mob.getY() + hoog / 2, mob.getZ(), wolk,
				breed / 2 + 0.2, hoog / 2, breed / 2 + 0.2, 0);
		double straal = Math.max(0.8, breed / 2 + 0.5);
		int ring = (int) Math.min(48, Math.round(straal * 16));
		for (int i = 0; i < ring; i++) {
			double hoek = Math.PI * 2 * i / ring;
			wereld.sendParticles(stof, true, true, mob.getX() + Math.cos(hoek) * straal, mob.getY() + 0.1,
					mob.getZ() + Math.sin(hoek) * straal, 1, 0, 0, 0, 0);
		}
	}

	/** Gesneuveld: kijker in de kooi tot het einde van de beurt, af voor de rest. Het Warden-ei houdt hij. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		SpelerStatus st = Spel.status(speler);
		st.dood = true;
		if (st.arena == 0) {
			// Kan eigenlijk niet: wie wacht krijgt geen schade. Dan gewoon terug naar de tribune.
			st.dood = false;
			Tribune.naarTribune(speler, Ronde.MOBARENA);
			return;
		}
		st.kooi = 1;
		boolean wardenEi = WardenEi.heeft(speler);
		// De kooi, inventory leeg: wie af is speelt niet meer, en na de ronde levert iedereen toch alles in.
		Tribune.maakKijkerOp(server, speler, "kooi", Tribune.Spullen.LEGEN, true);
		if (wardenEi) {
			WardenEi.geef(server, speler);
		}
		UUID id = speler.getUUID();
		Planner.na(60, () -> {
			ServerPlayer s = server.getPlayerList().getPlayer(id);
			if (s != null && Spel.actief() == this) {
				Spel.melding(s, Mc.tekst(Spel.instellingen().aftekst(), ChatFormatting.GRAY), 5);
				Mc.actionbar(s, Spel.melding(s));
			}
		});
	}

	/** Wie op zijn beurt wacht (of na een beurt viert), krijgt geen schade. */
	@Override
	public boolean magSchade(ServerPlayer slachtoffer, DamageSource bron) {
		return Spel.status(slachtoffer).arena > 0 && (fase == Fase.WAVES || fase == Fase.COUNTDOWN);
	}

	/** Uitloggen telt alleen als dood zolang de beurt loopt, niet tijdens de tien seconden erna. */
	@Override
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.WAVES || fase == Fase.COUNTDOWN) {
			super.onQuit(server, speler);
			return;
		}
		SpelerStatus st = Spel.status(speler);
		st.arena = 0;
		st.kooi = 0;
		speler.removeEffect(MobEffects.GLOWING);
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		// Uitgelogd tijdens zijn beurt: telt als dood, het veld telt hem niet meer.
		Spel.status(speler).arena = 0;
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		speler.setGameMode(GameType.ADVENTURE);
		if (st.dood) {
			st.kooi = 0;
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
		} else {
			Teams.zorgVoorTeam(server, speler);
			Spel.zetRol(server, speler, Rol.SPELER);
			Tribune.naarTribune(speler, Ronde.MOBARENA);
		}
	}

	// Na de beurt

	private void beurtKlaar(MinecraftServer server) {
		fase = Fase.VIEREN;
		ruimMobsOp(server);
		mobs.clear();
		Mc.titleAllen(server, Mc.tekst("BEURT " + (beurt + 1) + " KLAAR", ChatFormatting.GOLD, ChatFormatting.BOLD),
				standTekst(), 0, 200, 20);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		Bossbar.zet(BossbarTekst.mobarena(beurt + 1, schema.beurten(), 0), BossEvent.BossBarColor.RED, 0f);
		for (int s = Regels.BEURT_VIEREN; s >= 1; s--) {
			int over = s;
			Planner.naSeconden(Regels.BEURT_VIEREN - s, () -> {
				for (ServerPlayer p : Mc.deelnemers(server)) {
					SpelerStatus st = Spel.status(p);
					if (st.arena > 0 || st.kooi > 0) {
						Mc.actionbar(p, Mc.tekst("Naar de tribune over " + over, ChatFormatting.YELLOW));
					}
				}
			});
		}
		// Pas na tien seconden: alleen het veld en de kooi naar de tribune. De tribune blijft staan.
		Planner.naSeconden(Regels.BEURT_VIEREN, () -> {
			if (Spel.actief() != this) {
				return;
			}
			for (ServerPlayer p : Mc.deelnemers(server)) {
				SpelerStatus st = Spel.status(p);
				if (st.arena > 0) {
					st.arena = 0;
					p.removeEffect(MobEffects.GLOWING);
					Tribune.naarTribune(p, Ronde.MOBARENA);
				} else if (st.kooi > 0) {
					st.kooi = 0;
					Tribune.naarTribune(p, Ronde.MOBARENA);
				}
			}
			opstelling.clear();
			if (beurt + 1 >= schema.beurten()) {
				einde(server);
			} else {
				fase = Fase.WACHT;
				Bossbar.zet(BossbarTekst.mobarena(beurt + 1, schema.beurten(), 0) + " · klaar", BossEvent.BossBarColor.RED, 0f);
			}
		});
	}

	/** {@code Rood 47 · Blauw 30 · Groen 12 · Geel 8}, op volgorde. */
	private MutableComponent standTekst() {
		MutableComponent t = Component.empty();
		boolean eerste = true;
		for (Kleur k : punten.volgorde()) {
			if (!meedoend.contains(k)) {
				continue;
			}
			if (!eerste) {
				t.append(Mc.tekst(" · ", ChatFormatting.GRAY));
			}
			t.append(Mc.tekst(k.naam() + " " + punten.punten(k), Mc.kleur(k)));
			eerste = false;
		}
		return t;
	}

	private void einde(MinecraftServer server) {
		fase = Fase.EINDE;
		List<Kleur> winnaars = punten.winnaars(meedoend);
		MutableComponent titel;
		if (winnaars.size() == 1) {
			Kleur w = winnaars.get(0);
			titel = Mc.tekst(w.naam().toUpperCase(Locale.ROOT) + " WINT DE MOB ARENA", Mc.kleur(w), ChatFormatting.BOLD);
		} else {
			titel = Component.empty();
			for (int i = 0; i < winnaars.size(); i++) {
				if (i > 0) {
					titel.append(Mc.tekst(i == winnaars.size() - 1 ? " EN " : ", ", ChatFormatting.GOLD, ChatFormatting.BOLD));
				}
				titel.append(Mc.tekst(winnaars.get(i).naam().toUpperCase(Locale.ROOT), Mc.kleur(winnaars.get(i)), ChatFormatting.BOLD));
			}
			titel.append(Mc.tekst(" WINNEN SAMEN", ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
		Mc.titleAllen(server, titel, standTekst(), 10, 180, 20);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		// Vuurpijlen boven de tribune, in de kleur van de winnaar.
		ServerLevel wereld = Mc.wereld(server);
		for (String p : Spel.reeks("tribune_mob_")) {
			Punt pt = Spel.punt(p);
			for (Kleur w : winnaars) {
				Vuurwerk.kleur(wereld, pt.x(), pt.y() + 3, pt.z(), w.rgb());
			}
		}
		Spel.einde(server);
		Bossbar.basiskamp();
		toonSidebar(server);
		// Tien seconden vieren, dan levert iedereen alles in (ook een ongebruikt Warden-ei) en gaat
		// geheald naar de quiz.
		Planner.naSeconden(Regels.VIEREN, () -> {
			for (ServerPlayer s : Mc.deelnemers(server)) {
				s.getInventory().clearContent();
				s.inventoryMenu.broadcastChanges();
				Spel.zetRol(server, s, Rol.SPELER);
				s.setGameMode(GameType.ADVENTURE);
				Mc.heal(s);
				Spel.status(s).tribunepunt = null;
				Spel.status(s).kooi = 0;
				Tribune.naarVerzamelpunt(s);
			}
		});
	}

	/** {@code /mobarena wave volgende}: de huidige wave telt als klaar, ook de warden. */
	public String forceerWave(MinecraftServer server) {
		if (fase != Fase.WAVES || verloop == null || verloop.wave() == 0) {
			return "er loopt geen wave";
		}
		verloop.forceer();
		return null;
	}

	/** {@code /mobarena schema}: per beurt wie er per team in het veld staat, wie af is. Alleen voor wie het typt. */
	public String schemaTekst() {
		StringBuilder sb = new StringBuilder("Schema van de mob arena (" + schema.beurten() + " beurten, geheim):");
		for (int b = 0; b < schema.beurten(); b++) {
			sb.append("\n Beurt ").append(b + 1).append(b == beurt ? " (nu)" : b < beurt ? " (gespeeld)" : "").append(":");
			for (Kleur k : meedoend) {
				List<String> wie = new ArrayList<>();
				for (int n = 1; n <= MobSchema.PER_BEURT; n++) {
					UUID s = schema.gepland(b, n, k);
					if (s == null) {
						wie.add("extra beurt");
					} else {
						SpelerStatus st = Spel.status(s);
						wie.add(Spel.naamVan(s) + (st != null && st.dood ? " (af)" : ""));
					}
				}
				sb.append("\n   ").append(k.naam()).append(": ").append(String.join(", ", wie));
			}
		}
		return sb.toString();
	}

	private void toonSidebar(MinecraftServer server) {
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Kleur k : punten.volgorde()) {
			if (meedoend.contains(k)) {
				regels.add(new Sidebar.Regel(Mc.tekst(k.naam(), Mc.kleur(k), ChatFormatting.BOLD), punten.punten(k)));
			}
		}
		Sidebar.toonScores(server, "Mob Arena", regels);
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		return "mob arena: beurt " + (beurt + 1) + "/" + (schema == null ? "?" : schema.beurten()) + ", fase " + fase.name().toLowerCase(Locale.ROOT)
				+ (verloop != null ? ", wave " + verloop.wave() : "") + (wardenLeeft() ? ", de warden loopt rond" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		ruimMobsOp(server);
		mobs.clear();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			st.arena = 0;
			s.removeEffect(MobEffects.GLOWING);
			if (st.bevroren) {
				Opstelling.ontdooi(s);
			}
		}
		Sidebar.weg(server);
	}

	/** De wave telt als klaar: de mobs in het veld weg, ook de vexes. */
	private void ruimVeld(MinecraftServer server) {
		mobs.forEach(Entity::discard);
		mobs.clear();
		Regio veld = Spel.regio("veld");
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e.entityTags().contains(TAG) || (e instanceof Vex && veld != null && veld.bevat(e.getX(), e.getZ()))) {
				weg.add(e);
			}
		}
		weg.forEach(Entity::discard);
	}

	/** Alle mobs met de tag, ook van een vorige serverrun, plus de vexes van evokers in de mob arena. */
	public static void ruimMobsOp(MinecraftServer server) {
		Regio arena = Spel.regio("mobarena");
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			boolean vexInArena = e instanceof Vex && arena != null && arena.omhullende().bevat(e.getX(), e.getZ());
			if (e.entityTags().contains(TAG) || vexInArena) {
				weg.add(e);
			}
		}
		weg.forEach(Entity::discard);
	}
}
