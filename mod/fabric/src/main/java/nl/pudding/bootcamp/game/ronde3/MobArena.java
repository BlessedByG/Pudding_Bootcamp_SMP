package nl.pudding.bootcamp.game.ronde3;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.monster.Vex;
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
 * Ronde 3: de mob arena. Twee gespiegelde arena's spelen tegelijk; per beurt staat in elke arena van
 * elk team één speler op het vlak in zijn teamkleur. Vijf waves, punten per kill voor het team. Wie
 * sneuvelt gaat de kooi in, is af voor de rest van de ronde en is zijn spullen kwijt. Het schema is
 * geheim: je merkt pas dat je aan de beurt bent als je naar je vlak wordt geteleporteerd.
 */
public final class MobArena extends RondeLogica {
	public static final String TAG = "bootcamp_mob";
	private static final double VOLGBEREIK = 64.0;
	private static final double SPREIDING = 1.5;
	private static final int TARGET_ELKE_TICKS = 2;
	private static final int VELD_ELKE_TICKS = 10;

	private enum Fase {
		/** Wacht op {@code /mobarena volgende}. */
		WACHT,
		/** De countdown voor de waves. */
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
	private final List<Mob> mobs1 = new ArrayList<>();
	private final List<Mob> mobs2 = new ArrayList<>();
	private final int[] spawnTeller = new int[3];
	private final List<Kleur> meedoend = new ArrayList<>();
	/** Wie deze beurt in welke arena staat, met zijn kleur. */
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
		List<String> punten = new ArrayList<>(Spel.reeks("mob_1_"));
		punten.addAll(Spel.reeks("mob_2_"));
		punten.addAll(List.of("kooi_1", "kooi_2"));
		for (int a = 1; a <= 2; a++) {
			for (Kleur k : Kleur.values()) {
				punten.add("start_" + a + "_" + k.id());
			}
		}
		String fout = Spel.buitenRegio("mobarena", punten);
		if (fout != null) {
			return fout;
		}
		return tribuneInVeld();
	}

	/**
	 * Een tribuneplek die binnen een veld valt, zet wie er staat elke halve seconde terug: dan kan
	 * niemand op de tribune lopen. Meestal is het veld een blok te ruim geselecteerd, tot onder de
	 * tribune.
	 */
	private static String tribuneInVeld() {
		List<String> fout = new ArrayList<>();
		for (int a = 1; a <= 2; a++) {
			Regio veld = Spel.regio("veld_" + a);
			for (String t : Spel.reeks("tribune_mob_")) {
				Punt p = Spel.punt(t);
				if (veld != null && veld.bevatSpelerTot(p.x(), p.y(), p.z(), Spel.instellingen().veldHoogte())) {
					fout.add(t + " (in veld_" + a + ")");
				}
			}
		}
		return fout.isEmpty() ? null : "deze tribuneplekken liggen binnen een veld, waar wachtenden juist af moeten blijven: "
				+ String.join(", ", fout) + ". Selecteer het veld krapper: alleen de vloer, niet tot onder de tribune";
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
			st.arena = p.arena();
			st.tribunepunt = null;
			Spel.naarPunt(s, "start_" + p.arena() + "_" + p.kleur().id());
			Mc.heal(s);
			// Glowing in de teamkleur: de outline volgt de kleur van het team.
			Mc.effect(s, MobEffects.GLOWING, -1, 0);
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
		mobs1.removeIf(m -> !m.isAlive());
		mobs2.removeIf(m -> !m.isAlive());
		int[] mobs = {0, mobs1.size(), mobs2.size()};
		int[] spelers = {0, spelersIn(server, 1), spelersIn(server, 2)};
		for (MobVerloop.Gebeurtenis g : verloop.seconde(mobs, spelers)) {
			switch (g.soort()) {
				case START_WAVE -> startWave(server);
				case WAVE_GEFORCEERD -> {
					ruimArena(server, 1);
					ruimArena(server, 2);
				}
				case ARENA_KLAAR -> ruimArena(server, g.arena());
				case BEURT_KLAAR -> {
					beurtKlaar(server);
					return;
				}
			}
		}
		int over = mobs1.size() + mobs2.size();
		int totaal = verloop.wave() <= 0 ? 0 : waves.waves().get(verloop.wave() - 1).totaal() * actieveArenas();
		Bossbar.zet(BossbarTekst.mobarena(beurt + 1, schema.beurten(), verloop.wave()), BossEvent.BossBarColor.RED,
				totaal <= 0 ? 0f : (float) over / totaal);
	}

	private int actieveArenas() {
		int n = 0;
		for (int a = 1; a <= 2; a++) {
			if (!verloop.arenaKlaar(a)) {
				n++;
			}
		}
		return Math.max(1, n);
	}

	private static int spelersIn(MinecraftServer server, int arena) {
		int n = 0;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			if (st.arena == arena && !st.dood) {
				n++;
			}
		}
		return n;
	}

	private void startWave(MinecraftServer server) {
		WavesDef.Wave wave = waves.waves().get(verloop.wave() - 1);
		ServerLevel wereld = Mc.wereld(server);
		for (int a = 1; a <= 2; a++) {
			if (verloop.arenaKlaar(a)) {
				continue;
			}
			// Eerst een rookwolk op elk spawnpunt, dan de mobs.
			for (String p : Spel.reeks("mob_" + a + "_")) {
				Punt pt = Spel.punt(p);
				double x = pt.x() + (pt.blok() ? 0.5 : 0);
				double y = pt.y() + (pt.blok() ? 1 : 0);
				double z = pt.z() + (pt.blok() ? 0.5 : 0);
				Mc.particles(wereld, ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 40, 0.6, 0.02);
				wereld.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
			}
			for (WavesDef.Mob def : wave.mobs()) {
				for (int i = 0; i < def.aantal(); i++) {
					spawn(server, wereld, def, a);
				}
			}
		}
		Mc.titleAllen(server, Mc.tekst("WAVE " + verloop.wave(), ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst(wave.naam(), ChatFormatting.GRAY), 5, 40, 10);
		Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
	}

	private void spawn(MinecraftServer server, ServerLevel wereld, WavesDef.Mob def, int arena) {
		List<String> spawns = Spel.reeks("mob_" + arena + "_");
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
		// Om en om over de spawns van deze arena, met wat ruimte zodat ze niet in elkaar staan.
		Punt p = Spel.punt(spawns.get(Math.floorMod(spawnTeller[arena]++, spawns.size())));
		double x = p.x() + (p.blok() ? 0.5 : 0) + (Spel.RANDOM.nextDouble() * 2 - 1) * SPREIDING;
		double z = p.z() + (p.blok() ? 0.5 : 0) + (Spel.RANDOM.nextDouble() * 2 - 1) * SPREIDING;
		double y = p.y() + (p.blok() ? 1 : 0);
		mob.snapTo(x, y, z, Spel.RANDOM.nextFloat() * 360f, 0f);

		mob.finalizeSpawn(wereld, wereld.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), EntitySpawnReason.EVENT, null);
		mob.setBaby(false);
		mob.setPersistenceRequired();
		mob.addTag(TAG);
		mob.addTag("arena_" + arena);

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
		ServerPlayer doel = dichtstbijzijnde(server, mob, arena);
		if (doel != null) {
			mob.setTarget(doel);
		}
		wereld.addFreshEntity(mob);
		(arena == 1 ? mobs1 : mobs2).add(mob);
	}

	private static EntityType<?> type(String id) {
		Identifier sleutel = Identifier.tryParse(id);
		if (sleutel == null) {
			return null;
		}
		return BuiltInRegistries.ENTITY_TYPE.get(sleutel).map(Holder::value).orElse(null);
	}

	private static int arenaVan(Entity mob) {
		if (mob.entityTags().contains("arena_1")) {
			return 1;
		}
		return mob.entityTags().contains("arena_2") ? 2 : 0;
	}

	/** De dichtstbijzijnde speler die aan de beurt is in deze arena en nog leeft. */
	private static ServerPlayer dichtstbijzijnde(MinecraftServer server, Entity mob, int arena) {
		ServerPlayer beste = null;
		double afstand = VOLGBEREIK * VOLGBEREIK;
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			if (st.arena != arena || st.dood) {
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

	/** Mag deze mob dit doel hebben? Alleen spelers die aan de beurt zijn in zijn eigen arena. */
	private static boolean goedDoel(LivingEntity doel, int arena) {
		if (!(doel instanceof ServerPlayer s)) {
			return !(doel instanceof Player);
		}
		if (Mc.isStaff(s)) {
			return false;
		}
		SpelerStatus st = Spel.status(s);
		return st.arena == arena && !st.dood;
	}

	// Elke paar ticks: doelen en velden

	@Override
	public void tick(MinecraftServer server) {
		int t = server.getTickCount();
		if (t % TARGET_ELKE_TICKS == 0 && fase == Fase.WAVES) {
			houdKijkersBuiten(server);
		}
		if (t % VELD_ELKE_TICKS == 0 && (fase == Fase.WAVES || fase == Fase.COUNTDOWN)) {
			houdSpelersInHunVeld(server);
		}
	}

	/**
	 * Mobs laten kijkers met rust: een mob met een kijker, staff of een speler buiten zijn arena als
	 * doel verliest dat doel en kiest de dichtstbijzijnde speler die aan de beurt is. Ook de vexes
	 * van een evoker.
	 */
	private void houdKijkersBuiten(MinecraftServer server) {
		for (int a = 1; a <= 2; a++) {
			for (Mob m : a == 1 ? mobs1 : mobs2) {
				richt(server, m, a);
			}
		}
		Regio arena = Spel.regio("mobarena");
		if (arena != null) {
			Regio.Doos d = arena.omhullende();
			AABB doos = new AABB(d.min().x(), d.min().y() - 16, d.min().z(), d.max().x() + 1, d.max().y() + 32, d.max().z() + 1);
			for (Vex vex : Mc.wereld(server).getEntitiesOfClass(Vex.class, doos)) {
				richt(server, vex, arenaVanPlek(vex));
			}
		}
	}

	private static int arenaVanPlek(Entity e) {
		for (int a = 1; a <= 2; a++) {
			Regio veld = Spel.regio("veld_" + a);
			if (veld != null && veld.bevat(e.getX(), e.getZ())) {
				return a;
			}
		}
		return 0;
	}

	private static void richt(MinecraftServer server, Mob m, int arena) {
		if (!m.isAlive()) {
			return;
		}
		LivingEntity doel = m.getTarget();
		if (doel != null && !goedDoel(doel, arena)) {
			m.setTarget(null);
			doel = null;
		}
		if (doel == null && arena > 0) {
			ServerPlayer nieuw = dichtstbijzijnde(server, m, arena);
			if (nieuw != null) {
				m.setTarget(nieuw);
			}
		}
	}

	/** Een speler die aan de beurt is en zijn veld uit komt, gaat terug naar zijn vlak. */
	private void houdSpelersInHunVeld(MinecraftServer server) {
		for (MobSchema.Plek p : opstelling) {
			ServerPlayer s = p.speler() == null ? null : server.getPlayerList().getPlayer(p.speler());
			if (s == null) {
				continue;
			}
			SpelerStatus st = Spel.status(s);
			Regio veld = Spel.regio("veld_" + p.arena());
			if (st.arena == p.arena() && !st.dood && veld != null && !veld.bevat(s.getX(), s.getZ())) {
				Spel.naarPunt(s, "start_" + p.arena() + "_" + p.kleur().id());
			}
		}
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
		Mc.geluid(killer, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f);
		Spel.melding(killer, Component.empty()
				.append(Mc.tekst("+" + erbij, ChatFormatting.GREEN, ChatFormatting.BOLD))
				.append(Mc.tekst(" · " + team.naam() + " " + punten.punten(team), Mc.kleur(team))), 2);
		Mc.actionbar(killer, Spel.melding(killer));
		String kort = Instellingen.mobSleutel(type);
		if (kort.equals("evoker") || kort.equals("ravager")) {
			Mc.chatAllen(server, Component.empty().append(Mc.tekst(Mc.naam(killer), Mc.kleur(team)))
					.append(Mc.tekst(" killde de " + kort + " (+" + erbij + ")", ChatFormatting.GOLD)));
		}
		toonSidebar(server);
	}

	/** Gesneuveld: kijker in de kooi van zijn arena tot het einde van de beurt, af voor de rest. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		SpelerStatus st = Spel.status(speler);
		int arena = st.arena;
		st.dood = true;
		if (arena == 0) {
			// Kan eigenlijk niet: wie wacht krijgt geen schade. Dan gewoon terug naar de tribune.
			st.dood = false;
			Tribune.naarTribune(speler, Ronde.MOBARENA);
			return;
		}
		st.kooi = arena;
		// De kooi, inventory leeg: wie af is speelt niet meer, en na de ronde levert iedereen toch alles in.
		Tribune.maakKijkerOp(server, speler, "kooi_" + arena, Tribune.Spullen.LEGEN, true);
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
		// Uitgelogd tijdens zijn beurt: telt als dood, zijn arena telt hem niet meer.
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
		mobs1.clear();
		mobs2.clear();
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
		// Pas na tien seconden: alleen de arena's en de kooien naar de tribune. De tribune blijft staan.
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
		// Tien seconden vieren, dan levert iedereen alles in en gaat geheald naar de quiz.
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

	/** {@code /mobarena wave volgende}: de huidige wave telt als klaar in beide arena's. */
	public String forceerWave(MinecraftServer server) {
		if (fase != Fase.WAVES || verloop == null || verloop.wave() == 0) {
			return "er loopt geen wave";
		}
		verloop.forceer();
		return null;
	}

	/** {@code /mobarena schema}: per beurt wie in welke arena staat, wie af is. Alleen voor wie het typt. */
	public String schemaTekst() {
		StringBuilder sb = new StringBuilder("Schema van de mob arena (" + schema.beurten() + " beurten, geheim):");
		for (int b = 0; b < schema.beurten(); b++) {
			sb.append("\n Beurt ").append(b + 1).append(b == beurt ? " (nu)" : b < beurt ? " (gespeeld)" : "").append(":");
			for (int a = 1; a <= 2; a++) {
				sb.append("\n   arena ").append(a).append(": ");
				List<String> delen = new ArrayList<>();
				for (Kleur k : meedoend) {
					UUID s = schema.gepland(b, a, k);
					String wie;
					if (s == null) {
						wie = "extra beurt";
					} else {
						SpelerStatus st = Spel.status(s);
						wie = Spel.naamVan(s) + (st != null && st.dood ? " (af)" : "");
					}
					delen.add(k.naam() + " " + wie);
				}
				sb.append(String.join(", ", delen));
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
				+ (verloop != null ? ", wave " + verloop.wave() : "");
	}

	@Override
	public void end(MinecraftServer server) {
		ruimMobsOp(server);
		mobs1.clear();
		mobs2.clear();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			st.arena = 0;
			s.removeEffect(MobEffects.GLOWING);
		}
		Sidebar.weg(server);
	}

	private void ruimArena(MinecraftServer server, int arena) {
		List<Mob> lijst = arena == 1 ? mobs1 : mobs2;
		lijst.forEach(Entity::discard);
		lijst.clear();
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e.entityTags().contains("arena_" + arena) || (e instanceof Vex && arenaVanPlek(e) == arena)) {
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
