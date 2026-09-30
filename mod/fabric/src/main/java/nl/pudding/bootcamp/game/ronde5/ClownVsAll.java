package nl.pudding.bootcamp.game.ronde5;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regeerperiodes;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Arena;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;
import nl.pudding.bootcamp.visuals.Zweefkroon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 5: Clown vs All, in beeld King of the Hill. Eén leven voor iedereen. Kill de kroonhouder en
 * je krijgt de kroon; elke kroonwissel is een reset van de jacht. De kroonhouder heeft Strength II;
 * zijn er nog drie of minder over, dan heeft iedereen Strength I. Geen timer: de laatste die
 * overblijft wint. De regels staan in docs/03.
 */
public final class ClownVsAll extends RondeLogica {
	private static final int GOUD = 0xFFD700;

	private final Regeerperiodes regeerperiodes = new Regeerperiodes();
	/** De kroonhouder van dit moment; blijft staan als hij uitlogt, tot de kroon doorgaat. */
	private UUID kroon;
	/** Seconden tot de kroon van een uitgelogde kroonhouder doorgaat; -1 als hij er gewoon is. */
	private int kroonWeg = -1;
	private int bijStart;
	private boolean voorbij;
	/** Wie deze tick doodging: die houdt zijn doodtekst in beeld in plaats van de title voor iedereen. */
	private final Set<UUID> netDood = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.CLOWN;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		ServerPlayer clown = Spel.clown(server);
		if (clown == null) {
			return "de uitverkorene is niet online of niet gezet (/clown uitverkoren <speler>)";
		}
		if (Mc.isStaff(clown)) {
			return "de uitverkorene staat in creative of spectator en doet dus niet mee";
		}
		if (Mc.deelnemers(server).size() < 2) {
			return "er zijn minstens twee spelers nodig";
		}
		String fout = Kits.controleer(server, "jager", "boss", "kroonpakket");
		return fout != null ? fout : Arena.controleerPlekken();
	}

	@Override
	public void start(MinecraftServer server) {
		ServerPlayer clown = Spel.clown(server);
		Spelregels.locatorBar(server, true);
		Kroon.wisHits();

		List<ServerPlayer> jagers = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			st.tribunepunt = null;
			s.setGameMode(GameType.ADVENTURE);
			s.removeAllEffects();
			Mc.heal(s);
			if (s != clown) {
				// Een kroon uit een eerdere poging (na /clown stop) mag niet blijven zitten.
				Kroon.neemAf(s);
				Spel.zetRol(server, s, Rol.JAGER);
				Kroon.toonOpLocator(s, false);
				jagers.add(s);
			}
		}
		Kits.geefAan(server, "jager", jagers);
		// Clown naar het podium: eerst de kroon, dan de bosskit (die blijft van de head-slot af), Glowing.
		Kroon.zetKroonOp(server, clown);
		Kits.geefAan(server, "boss", List.of(clown));
		wordKroonhouder(server, clown);
		Spel.naarPunt(clown, "troon");
		Arena.verdeel(jagers);
		bijStart = jagers.size() + 1;

		Arena.zetBorder(server);
		toonSidebar(server);
		// Iedereen bevroren, ook Clown, tot de commander /clown go doet.
		List<ServerPlayer> iedereen = new ArrayList<>(jagers);
		iedereen.add(clown);
		Opstelling.wacht(server, iedereen);
		sterkte(server);
	}

	/** {@code /clown go}: de countdown van tien seconden, daarna is iedereen los. */
	public String go(MinecraftServer server) {
		if (!Opstelling.wachtOpGo()) {
			return "er staat niemand klaar";
		}
		if (kroonWeg >= 0) {
			return "de kroonhouder is uitgelogd: wacht tot hij terug is, of geef de kroon met /clown kroon <speler>";
		}
		Opstelling.go(server, Regels.OPSTELLING, "De jacht begint over", null);
		return null;
	}

	private void wordKroonhouder(MinecraftServer server, ServerPlayer speler) {
		kroon = speler.getUUID();
		kroonWeg = -1;
		Kroon.geef(server, speler);
		regeerperiodes.nieuweKoning(Mc.naam(speler));
	}

	private List<ServerPlayer> levendeJagers(MinecraftServer server) {
		return Spel.levend(server, Rol.JAGER);
	}

	/** Hoeveel er nog meedoen: de levende jagers plus de kroonhouder (ook als die net is uitgelogd). */
	private int over(MinecraftServer server) {
		int n = levendeJagers(server).size();
		if (kroon != null) {
			ServerPlayer k = server.getPlayerList().getPlayer(kroon);
			if ((k != null && Spel.status(k).rol == Rol.KROON && !Spel.status(k).dood) || kroonWeg >= 0) {
				n++;
			}
		}
		return n;
	}

	// Dood

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		Rol rol = Spel.status(speler).rol;
		netDood.add(speler.getUUID());
		try {
			ServerPlayer killer = Tribune.aanvaller(bron);
			if (killer == speler) {
				killer = null;
			}
			if (rol == Rol.KROON) {
				kroonDoor(server, killer == null ? null : killer.getUUID(), true);
			} else if (rol == Rol.JAGER) {
				Spel.status(speler).dood = true;
				Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
				int over = over(server);
				Arena.afMelding(server, Mc.naam(speler), ChatFormatting.AQUA,
						killer == null ? null : Mc.naam(killer), ChatFormatting.GOLD, over);
				// Bij de laatste kill komt de winnaar in beeld, niet de kill.
				if (killer != null && Spel.status(killer).rol == Rol.KROON && !Regels.laatsteOver(over)) {
					kroonKill(server, killer, speler, over);
				}
				sterkte(server);
				controleerEinde(server);
			}
		} finally {
			netDood.clear();
		}
	}

	/**
	 * Een kill van de kroonhouder is een prestatie: groot in beeld voor iedereen behalve de dode, die
	 * zijn doodtekst ziet. Met de naam van wie de kroon nu heeft, dus niet altijd Clown.
	 */
	private void kroonKill(MinecraftServer server, ServerPlayer kroonhouder, ServerPlayer dode, int over) {
		List<ServerPlayer> voor = Mc.spelers(server).stream().filter(s -> !netDood.contains(s.getUUID())).toList();
		Arena.killInBeeld(voor, kroonhouder, dode, ChatFormatting.AQUA, over, 1f);
	}

	/**
	 * De kroonhouder is weg (dood, of te lang uitgelogd): de kroon gaat naar de killer, anders de
	 * laatste hit, anders een willekeurige levende jager. Is er geen jager meer, dan is de ronde
	 * voorbij.
	 */
	private void kroonDoor(MinecraftServer server, UUID killer, boolean doodtekst) {
		List<UUID> levend = levendeJagers(server).stream().map(ServerPlayer::getUUID).toList();
		Optional<UUID> opvolger = Regels.kroonOpvolger(killer, Kroon.laatsteHit(kroon), levend, Spel.RANDOM);
		if (opvolger.isEmpty()) {
			einde(server);
			return;
		}
		wissel(server, server.getPlayerList().getPlayer(opvolger.get()), killer, doodtekst);
	}

	/** Elke kroonwissel is een reset: nieuwe kroonhouder op het podium, jagers terug naar de rand, doden blijven dood. */
	private void wissel(MinecraftServer server, ServerPlayer nieuw, UUID killer, boolean doodtekst) {
		String oudeNaam = kroon == null ? null : Spel.naamVan(kroon);
		// De ex-kroonhouder (en wie door een bug ook een kroon draagt) gaat de tribune op.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s != nieuw && Spel.status(s).rol == Rol.KROON) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, doodtekst);
			}
		}
		if (kroon != null && server.getPlayerList().getPlayer(kroon) == null) {
			// Uitgelogd: hij is uit de ronde; komt hij terug, dan als kijker op de tribune.
			SpelerStatus weg = Spel.status(kroon);
			if (weg != null) {
				weg.dood = true;
				weg.rol = Rol.KIJKER;
			}
			Zweefkroon.uit(kroon);
		}
		// Elke wissel is een reset, ook van een gekrompen border.
		Arena.zetBorder(server);
		Kroon.wisHits();

		SpelerStatus st = Spel.status(nieuw);
		st.dood = false;
		st.tribunepunt = null;
		nieuw.setGameMode(GameType.ADVENTURE);
		wordKroonhouder(server, nieuw);
		Spel.naarPunt(nieuw, "troon");
		Mc.heal(nieuw);
		repareer(nieuw);
		Kits.geefAan(server, "kroonpakket", List.of(nieuw));

		List<ServerPlayer> jagers = levendeJagers(server);
		for (ServerPlayer j : jagers) {
			Mc.heal(j);
		}
		Arena.verdeel(jagers);

		if (oudeNaam != null) {
			String killerNaam = killer == null ? null : Spel.naamVan(killer);
			Arena.afMelding(server, oudeNaam, ChatFormatting.GOLD, killerNaam, ChatFormatting.AQUA, over(server));
		}
		Mc.titleAllenBehalve(server, netDood, Mc.tekst("NIEUWE KROON", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.kopEnNaam(nieuw, ChatFormatting.YELLOW, ChatFormatting.BOLD));
		// Donder zonder bliksem: echte bliksem zet dingen in de fik.
		Mc.geluidAllen(server, SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 1f);
		Mc.particles(Mc.wereld(server), ColorParticleOption.create(ParticleTypes.FLASH, GOUD), nieuw.getX(), nieuw.getY() + 1, nieuw.getZ(), 1, 0, 0);
		toonSidebar(server);
		sterkte(server);

		if (jagers.isEmpty()) {
			// De killer was de laatste jager: hij is als enige over.
			einde(server);
			return;
		}
		// Tien seconden waarin niemand van zijn plek kan, ook de kroonhouder niet. Loopt vanzelf.
		List<ServerPlayer> bevriezen = new ArrayList<>(jagers);
		bevriezen.add(nieuw);
		Opstelling.start(server, bevriezen, Regels.OPSTELLING, "De jacht gaat verder over", null);
	}

	/** Alles op volle durability. */
	private static void repareer(ServerPlayer speler) {
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isDamageableItem() && stack.isDamaged()) {
				stack.setDamageValue(0);
			}
		}
		speler.inventoryMenu.broadcastChanges();
	}

	/** {@code /clown kroon <speler>}: de noodknop van de ref. */
	public String forceer(MinecraftServer server, ServerPlayer nieuw) {
		if (Mc.isStaff(nieuw)) {
			return Mc.naam(nieuw) + " staat in creative of spectator en doet niet mee";
		}
		if (nieuw.getUUID().equals(kroon) && Kroon.draagtKroon(nieuw)) {
			return Mc.naam(nieuw) + " heeft de kroon al";
		}
		if (Spel.status(nieuw).rol != Rol.JAGER && Spel.status(nieuw).rol != Rol.KROON) {
			// Een kijker terughalen mag: hij doet weer mee, met de jagerskit.
			Spel.status(nieuw).dood = false;
			Spel.status(nieuw).tribunepunt = null;
			nieuw.setGameMode(GameType.ADVENTURE);
			Spel.zetRol(server, nieuw, Rol.JAGER);
			Kits.geefAan(server, "jager", List.of(nieuw));
		}
		if (Opstelling.wachtOpGo()) {
			wisselVoorDeStart(server, nieuw);
			return null;
		}
		wissel(server, nieuw, null, false);
		return null;
	}

	/**
	 * Voor {@code /clown go} (het rad landde op de verkeerde naam): de kroon gaat over zonder dat
	 * iemand af is. De oude kroonhouder wordt jager op de plek van de nieuwe, en iedereen blijft
	 * wachten op {@code /clown go}.
	 */
	private void wisselVoorDeStart(MinecraftServer server, ServerPlayer nieuw) {
		Punt plek = Punt.positie(nieuw.getX(), nieuw.getY(), nieuw.getZ(), nieuw.getYRot(), nieuw.getXRot());
		if (kroon != null && server.getPlayerList().getPlayer(kroon) == null) {
			// De oude kroonhouder is uitgelogd: komt hij terug, dan als jager.
			SpelerStatus weg = Spel.status(kroon);
			if (weg != null) {
				weg.rol = Rol.JAGER;
				weg.dood = false;
			}
			Zweefkroon.uit(kroon);
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s != nieuw && Spel.status(s).rol == Rol.KROON) {
				Kroon.neemAf(s);
				Spel.zetRol(server, s, Rol.JAGER);
				Kits.geefAan(server, "jager", List.of(s));
				Mc.teleport(s, plek);
				Opstelling.bevries(s);
			}
		}
		Kroon.wisHits();
		regeerperiodes.wis();
		Kroon.zetKroonOp(server, nieuw);
		Kits.geefAan(server, "boss", List.of(nieuw));
		wordKroonhouder(server, nieuw);
		Spel.naarPunt(nieuw, "troon");
		Mc.heal(nieuw);
		Opstelling.bevries(nieuw);
		toonSidebar(server);
		Mc.titleAllen(server, Mc.tekst("DE KROON", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.kopEnNaam(nieuw, ChatFormatting.YELLOW, ChatFormatting.BOLD));
	}

	// Uitloggen en terugkomen

	@Override
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.rol == Rol.KROON && speler.getUUID().equals(kroon)) {
			// De mod wacht dertig seconden, zichtbaar in de bossbar.
			kroonWeg = Regels.KROON_UITLOG_WACHT;
			return;
		}
		if (st.rol == Rol.JAGER && !st.dood) {
			st.dood = true;
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.AQUA, null, ChatFormatting.GOLD, over(server));
		}
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (kroonWeg >= 0 && speler.getUUID().equals(kroon)) {
			kroonWeg = -1;
			Kroon.geef(server, speler);
			return;
		}
		if (st.rol == Rol.JAGER && !st.dood && Opstelling.wachtOpGo()) {
			// De kroon ging voor de start naar een ander: hij doet mee als jager.
			Kroon.neemAf(speler);
			Spel.zetRol(server, speler, Rol.JAGER);
			Kits.geefAan(server, "jager", List.of(speler));
			Arena.verdeel(List.of(speler));
			Opstelling.bevries(speler);
			return;
		}
		st.dood = true;
		Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
	}

	// Klok

	@Override
	public void seconde(MinecraftServer server) {
		if (voorbij) {
			return;
		}
		String naam = kroon == null ? "?" : Spel.naamVan(kroon);
		int over = over(server);
		if (kroonWeg >= 0 && Opstelling.wachtOpGo()) {
			// Voor de start telt de wacht niet af: de commander wacht op hem of geeft de kroon met de hand.
			Bossbar.zet(BossbarTekst.CLOWN_WACHT + " · " + naam + " is weg", BossEvent.BossBarColor.RED, 1f);
		} else if (kroonWeg >= 0) {
			Bossbar.zet(BossbarTekst.kroonWeg(naam, kroonWeg), BossEvent.BossBarColor.RED, (float) kroonWeg / Regels.KROON_UITLOG_WACHT);
			if (kroonWeg == 0) {
				kroonWeg = -1;
				// Dezelfde kroonwissel als bij een val-dood: laatste hit, anders willekeurig.
				kroonDoor(server, null, false);
				return;
			}
			kroonWeg--;
		} else if (Opstelling.wachtOpGo()) {
			Bossbar.zet(BossbarTekst.CLOWN_WACHT, BossEvent.BossBarColor.YELLOW, 1f);
			for (ServerPlayer s : opDeVloer(server)) {
				Mc.actionbar(s, Mc.tekst(Spel.instellingen().clownWachttekst(), ChatFormatting.YELLOW));
			}
		} else {
			Bossbar.zet(BossbarTekst.clown(naam, over), BossEvent.BossBarColor.YELLOW, bijStart <= 0 ? 1f : (float) over / bijStart);
		}
		if (!Tribune.stil() && kroonWeg < 0) {
			regeerperiodes.seconde();
			toonSidebar(server);
			ServerPlayer k = kroon == null ? null : server.getPlayerList().getPlayer(kroon);
			if (k != null) {
				int jagers = levendeJagers(server).size();
				Mc.actionbar(k, Mc.tekst("Jij hebt de kroon · " + jagers + (jagers == 1 ? " jager" : " jagers"), ChatFormatting.GOLD));
			}
		}
		// Ook na een heal (die haalt effecten weg) of /bc kijker: elke seconde opnieuw gelijkgezet.
		sterkte(server);
		// Niet alleen bij een dood: ook als de ref de laatste jager met /bc kijker van de vloer haalt.
		controleerEinde(server);
	}

	/**
	 * De Strength van R5.10: de kroonhouder Strength II, de jagers niets; met nog drie of minder over
	 * iedereen Strength I. Wie af is heeft niets.
	 */
	private void sterkte(MinecraftServer server) {
		int over = over(server);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			boolean doetMee = !st.dood && (st.rol == Rol.KROON || st.rol == Rol.JAGER);
			zetSterkte(s, doetMee ? Regels.kroonSterkte(over, st.rol == Rol.KROON) : -1);
		}
	}

	private static void zetSterkte(ServerPlayer s, int niveau) {
		MobEffectInstance nu = s.getEffect(MobEffects.STRENGTH);
		if (niveau < 0) {
			if (nu != null) {
				s.removeEffect(MobEffects.STRENGTH);
			}
			return;
		}
		if (nu != null && nu.getAmplifier() == niveau && nu.isInfiniteDuration()) {
			return;
		}
		// Van II naar I kan niet met alleen een nieuw effect: vanilla houdt dan de sterkste.
		s.removeEffect(MobEffects.STRENGTH);
		Mc.effect(s, MobEffects.STRENGTH, -1, niveau);
	}

	private List<ServerPlayer> opDeVloer(MinecraftServer server) {
		List<ServerPlayer> uit = new ArrayList<>(levendeJagers(server));
		ServerPlayer k = kroon == null ? null : server.getPlayerList().getPlayer(kroon);
		if (k != null) {
			uit.add(k);
		}
		return uit;
	}

	private void toonSidebar(MinecraftServer server) {
		Sidebar.toonTekstRegels(server, "Regeerperiodes", regeerperiodes.periodes().stream().map(Regeerperiodes.Periode::tekst).toList());
	}

	private void controleerEinde(MinecraftServer server) {
		if (Spel.actief() == this && !voorbij && kroonWeg < 0 && Regels.laatsteOver(over(server))) {
			einde(server);
		}
	}

	/** {@code /clown krimp}: de border rond de vloer krimpt, iedereen ziet het. */
	public void krimp(MinecraftServer server, int grootte, int seconden) {
		Arena.krimp(server, grootte, seconden);
	}

	// Einde

	private void einde(MinecraftServer server) {
		if (voorbij) {
			return;
		}
		voorbij = true;
		// De laatste die overblijft: de kroonhouder, of de jager die hem als laatste versloeg.
		ServerPlayer winnaar = null;
		ServerPlayer k = kroon == null ? null : server.getPlayerList().getPlayer(kroon);
		if (k != null && Spel.status(k).rol == Rol.KROON && !Spel.status(k).dood) {
			winnaar = k;
		} else {
			List<ServerPlayer> jagers = levendeJagers(server);
			if (!jagers.isEmpty()) {
				winnaar = jagers.get(0);
			}
		}
		Regeerperiodes.Periode langste = regeerperiodes.langste();
		Spel.einde(server);
		Bossbar.basiskamp();
		toonSidebar(server);
		if (winnaar != null) {
			// Ook voor Clown dezelfde title: niks mag verraden dat hij moest winnen.
			MutableComponent titel = Component.empty().append(Mc.kop(winnaar)).append(Component.literal(" "))
					.append(Mc.tekst(Mc.naam(winnaar).toUpperCase(Locale.ROOT) + " WINT KING OF THE HILL", ChatFormatting.GOLD, ChatFormatting.BOLD));
			Mc.titleAllenBehalve(server, netDood, titel, null);
			Vuurwerk.goud(Mc.wereld(server), winnaar.getX(), winnaar.getY() + 2, winnaar.getZ());
			Mc.heal(winnaar);
		} else {
			Mc.titleAllen(server, Mc.tekst("KING OF THE HILL IS VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD), null);
		}
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		if (langste != null) {
			Mc.chatAllen(server, Mc.tekst("Langste regeerperiode: " + langste.tekst(), ChatFormatting.GOLD));
		}
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		return "kroon: " + (kroon == null ? "niemand" : Spel.naamVan(kroon)) + (kroonWeg >= 0 ? " (uitgelogd, " + kroonWeg + " s)" : "")
				+ ", " + over(server) + " over" + (Opstelling.wachtOpGo() ? ", wacht op /clown go" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		for (ServerPlayer s : Mc.deelnemers(server)) {
			s.removeEffect(MobEffects.STRENGTH);
		}
		Sidebar.weg(server);
	}
}
