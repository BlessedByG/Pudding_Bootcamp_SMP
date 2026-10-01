package nl.pudding.bootcamp.game.ronde6;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Klassement;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Arena;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 6: de FFA in de Arena. Iedereen, ook de uitverkorene en wie in Clown vs All af was, met
 * dezelfde kit. Geen timer: de laatste die overblijft wint en speelt de finale tegen de winnaar
 * van King of the Hill. De kroning komt na de finale.
 */
public final class Ffa extends RondeLogica {
	private static final int PAARS = 0xAA00AA;

	private boolean voorbij;
	/** Wie als laatste afviel: de nummer twee, voor de finale. */
	private String laatsteAf;
	private final Klassement kills = new Klassement();
	private int bijStart;
	private int laatstGemeld = Integer.MAX_VALUE;
	private final Set<UUID> netDood = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.FFA;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (Mc.deelnemers(server).size() < 2) {
			return "er zijn minstens twee spelers nodig (iedereen behalve staff)";
		}
		String fout = Kits.controleer(server, "arena");
		return fout != null ? fout : Arena.controleerPlekken();
	}

	@Override
	public void start(MinecraftServer server) {
		Spelregels.locatorBar(server, false);
		Kroon.wisHits();
		// R6.1: iedereen behalve staff, ook de uitverkorene en wie in ronde 5 af was.
		List<ServerPlayer> vechters = Mc.deelnemers(server);
		for (ServerPlayer s : vechters) {
			SpelerStatus st = Spel.status(s);
			st.dood = false;
			st.tribunepunt = null;
			// Een oude kroon gaat eraf: de kit blijft er anders vanaf.
			Kroon.neemAf(s);
			// Geen team: iedereen kan iedereen raken.
			Spel.zetRol(server, s, Rol.FFA);
			s.setGameMode(GameType.ADVENTURE);
			s.removeAllEffects();
			Kroon.toonOpLocator(s, false);
			Mc.heal(s);
		}
		Arena.verdeel(vechters);
		// Dezelfde kit voor iedereen; eigen spullen gaan weg (clear: true).
		Kits.geefAan(server, "arena", vechters);
		bijStart = vechters.size();
		Arena.zetBorder(server);
		toonSidebar(server);
		// Iedereen bevroren tot de commander /ffa go doet.
		Opstelling.wacht(server, vechters);
	}

	/** {@code /ffa go}: de countdown van tien seconden, daarna is iedereen los. */
	public String go(MinecraftServer server) {
		if (!Opstelling.wachtOpGo()) {
			return "er staat niemand klaar";
		}
		Opstelling.go(server, Regels.OPSTELLING, "De FFA begint over", null);
		return null;
	}

	// Vechten

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		if (voorbij || Spel.status(speler).rol != Rol.FFA) {
			return;
		}
		netDood.add(speler.getUUID());
		try {
			ServerPlayer killer = Tribune.aanvaller(bron);
			if (killer != null && (killer == speler || Spel.status(killer).rol != Rol.FFA)) {
				killer = null;
			}
			if (killer != null) {
				Spel.status(killer).kills++;
				kills.voegToe(killer.getUUID(), 1);
				toonSidebar(server);
			}
			laatsteAf = Mc.naam(speler);
			Spel.status(speler).dood = true;
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
			int over = Spel.levend(server, Rol.FFA).size();
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.WHITE, killer == null ? null : Mc.naam(killer), ChatFormatting.WHITE, over);
			// LAATSTE DRIE en TWEE gaan voor de kill; bij de laatste kill komt de winnaar.
			if (!laatsten(server, over) && killer != null && over > 1) {
				killInBeeld(server, killer, speler, over);
			}
			toonKills(server);
			controleerWinnaar(server);
		} finally {
			netDood.clear();
		}
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		if (!voorbij) {
			laatsteAf = Mc.naam(speler);
			// Hij telt al als dood, dus hij zit niet meer in de telling.
			int over = Spel.levend(server, Rol.FFA).size();
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.WHITE, null, ChatFormatting.WHITE, over);
			laatsten(server, over);
		}
	}

	/**
	 * Een kill groot in beeld voor de tribune (en staff) en voor de killer zelf. Wie nog vecht, ziet
	 * alleen de chatregel; de dode ziet zijn doodtekst.
	 */
	private void killInBeeld(MinecraftServer server, ServerPlayer killer, ServerPlayer dode, int over) {
		List<ServerPlayer> voor = Mc.spelers(server).stream()
				.filter(s -> !netDood.contains(s.getUUID()))
				.filter(s -> s == killer || Spel.status(s).rol != Rol.FFA || Spel.status(s).dood)
				.toList();
		// Zachter dan in King of the Hill: in de FFA vallen veel meer kills.
		Arena.killInBeeld(voor, killer, dode, ChatFormatting.WHITE, over, 0.5f);
	}

	/** Onderin voor iedereen die nog vecht: {@code 3 kills · 11 over}. Niet zolang iedereen stil staat. */
	private void toonKills(MinecraftServer server) {
		if (Tribune.stil()) {
			return;
		}
		List<ServerPlayer> vechters = Spel.levend(server, Rol.FFA);
		for (ServerPlayer s : vechters) {
			int k = kills.score(s.getUUID());
			Mc.actionbar(s, Mc.tekst(k + (k == 1 ? " kill" : " kills") + " · " + vechters.size() + " over", ChatFormatting.GOLD));
		}
	}

	/** Bij drie over LAATSTE DRIE, bij twee LAATSTE TWEE, met de namen. Geeft terug of die kwam. */
	private boolean laatsten(MinecraftServer server, int over) {
		String titel = Regels.aftelTitle(over);
		if (titel == null || over >= laatstGemeld) {
			return false;
		}
		laatstGemeld = over;
		List<String> namen = Spel.levend(server, Rol.FFA).stream().map(Mc::naam).toList();
		String sub = over == 2 && namen.size() == 2 ? namen.get(0) + " tegen " + namen.get(1) : String.join(", ", namen);
		Mc.titleAllenBehalve(server, netDood, Component.literal(titel).withStyle(s -> s.withColor(PAARS).withBold(true)),
				Mc.tekst(sub, ChatFormatting.LIGHT_PURPLE), 5, 50, 15);
		Mc.geluidAllen(server, SoundEvents.WITHER_SPAWN, 0.3f, 1f);
		return true;
	}

	private void controleerWinnaar(MinecraftServer server) {
		if (voorbij) {
			return;
		}
		List<ServerPlayer> over = Spel.levend(server, Rol.FFA);
		if (over.size() == 1) {
			einde(server, over.get(0));
		} else if (over.isEmpty()) {
			// Iedereen tegelijk weg (de laatste twee logden uit): de ref beslist.
			Spel.stop(server);
			Mc.chatAllen(server, Mc.tekst("[bootcamp] De FFA heeft geen winnaar: er staat niemand meer. Start hem opnieuw met /ffa start.",
					ChatFormatting.RED));
		}
	}

	/** {@code /ffa krimp}: de border rond de vloer krimpt, iedereen ziet het. */
	public void krimp(MinecraftServer server, int grootte, int seconden) {
		Arena.krimp(server, grootte, seconden);
	}

	// Klok

	@Override
	public void seconde(MinecraftServer server) {
		int over = Spel.levend(server, Rol.FFA).size();
		if (Opstelling.wachtOpGo()) {
			Bossbar.zet(BossbarTekst.FFA_WACHT, BossEvent.BossBarColor.PURPLE, 1f);
			for (ServerPlayer s : Spel.levend(server, Rol.FFA)) {
				Mc.actionbar(s, Mc.tekst(Spel.instellingen().ffaWachttekst(), ChatFormatting.YELLOW));
			}
			return;
		}
		Bossbar.zet(BossbarTekst.ffa(over), BossEvent.BossBarColor.PURPLE, bijStart <= 0 ? 1f : (float) over / bijStart);
		toonKills(server);
		// Niet alleen bij een dood: ook met maar één vechter, of na /bc kijker.
		if (over <= 1) {
			controleerWinnaar(server);
		}
	}

	private void toonSidebar(MinecraftServer server) {
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Klassement.Regel r : kills.top(15)) {
			regels.add(new Sidebar.Regel(Mc.tekst(Spel.naamVan(r.speler()), ChatFormatting.WHITE), r.score()));
		}
		Sidebar.toonScores(server, "Kills", regels);
	}

	// Einde

	/** De laatste die overblijft wint de FFA en gaat door naar de finale; de kroning komt daarna. */
	private void einde(MinecraftServer server, ServerPlayer winnaar) {
		voorbij = true;
		ConfigStore.get().zetUitslagFfa(Mc.naam(winnaar), laatsteAf);
		ConfigStore.bewaar();
		Spel.einde(server);
		Bossbar.basiskamp();
		MutableComponent titel = Component.empty().append(Mc.kop(winnaar)).append(Component.literal(" "))
				.append(Mc.tekst(Mc.naam(winnaar).toUpperCase(Locale.ROOT) + " WINT DE FFA", ChatFormatting.GOLD, ChatFormatting.BOLD));
		Mc.titleAllenBehalve(server, netDood, titel, null);
		Vuurwerk.goud(Mc.wereld(server), winnaar.getX(), winnaar.getY() + 2, winnaar.getZ());
		Mc.heal(winnaar);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		Spel.status(speler).dood = true;
		Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		return Spel.levend(server, Rol.FFA).size() + " over" + (Opstelling.wachtOpGo() ? ", wacht op /ffa go" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		// De sidebar met de kills blijft na de winst staan tot de finale of /bc reset.
		if (!voorbij) {
			Sidebar.weg(server);
		}
	}
}
