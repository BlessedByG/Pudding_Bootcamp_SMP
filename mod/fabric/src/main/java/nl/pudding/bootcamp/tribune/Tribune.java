package nl.pudding.bootcamp.tribune;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Doodteksten;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Kijkers: wie dood of klaar is. Geen spectator mode, geen tp-items, geen vliegen. De mod laat
 * spelers nooit echt doodgaan: een dodelijke klap wordt geannuleerd en de ronde beslist wat er
 * gebeurt. Een kijker staat in adventure op de tribune, krijgt geen schade, komt de vloer niet op
 * en staat niet op de locator bar.
 */
public final class Tribune {
	/** Wat er met de inventory gebeurt van wie kijker wordt. */
	public enum Spullen {
		/** Klaar met de ronde (uit het doolhof): je houdt wat je hebt. */
		HOUDEN,
		/** Ronde 2: bewaard en bij v3 teruggegeven. */
		BEWAREN,
		/** In de Arena: leeg. */
		LEGEN
	}

	private static final int TERUGZET_ELKE_TICKS = 10;
	private static int volgendeTribune;

	private Tribune() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, bron, schade) -> {
			if (!(entity instanceof ServerPlayer speler) || Mc.isStaff(speler)) {
				return true;
			}
			return !vangDoodAf(speler, bron);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, bron, schade) -> {
			if (!(entity instanceof ServerPlayer slachtoffer) || Mc.isStaff(slachtoffer)) {
				return true;
			}
			return magSchade(slachtoffer, bron);
		});
		Reset.REGISTER.registreer("kijkers weg", server -> {
			volgendeTribune = 0;
			// De rollen en vlaggen zijn al gewist; hier alleen wat aan de speler zelf hangt.
			for (ServerPlayer s : Mc.deelnemers(server)) {
				Kroon.toonOpLocator(s, true);
			}
		});
	}

	// Dood en schade

	/** @return {@code true} als de dood is afgevangen */
	private static boolean vangDoodAf(ServerPlayer speler, DamageSource bron) {
		MinecraftServer server = speler.level().getServer();
		// Wie de dood annuleert moet zelf healen, anders gaat de speler de volgende tick alsnog dood.
		Mc.heal(speler);
		speler.removeAllEffects();
		RondeLogica ronde = Spel.actief();
		try {
			if (ronde != null && !Spel.status(speler).dood && Spel.rol(speler) != Rol.KIJKER) {
				ronde.onDeath(server, speler, bron);
			} else if (ronde != null) {
				// Een kijker die toch "doodgaat" (de void in): terug naar zijn plek.
				String plek = Spel.status(speler).tribunepunt;
				if (plek != null) {
					Spel.naarPunt(speler, plek);
				}
			} else {
				// Tussen twee rondes in of in het basiskamp: terug naar waar iedereen staat.
				String punt = verzamelpuntNa(Spel.ronde());
				if (Spel.punt(punt) != null) {
					Spel.naarPunt(speler, punt);
				}
			}
		} catch (RuntimeException e) {
			Bootcamp.LOG.error("De dood van {} afhandelen faalde", Mc.naam(speler), e);
		}
		return true;
	}

	private static boolean magSchade(ServerPlayer slachtoffer, DamageSource bron) {
		Rol rol = Spel.rol(slachtoffer);
		// Kijkers en wachtende finalisten krijgen geen schade, ook niet van de border.
		if (rol == Rol.KIJKER || rol == Rol.FINALIST) {
			return false;
		}
		// Zolang een opstelling loopt doet niemand elkaar iets.
		if (Opstelling.actief()) {
			return false;
		}
		if (bron.getEntity() instanceof ServerPlayer aanvaller && aanvaller != slachtoffer) {
			Rol aanvalRol = Spel.rol(aanvaller);
			if (aanvalRol == Rol.KIJKER || aanvalRol == Rol.FINALIST) {
				return false;
			}
			// De laatste hit: ook via een pijl, want getEntity() is de schutter.
			if (rol == Rol.KING && Spel.ronde() == Ronde.KING && !Spel.status(aanvaller).dood) {
				Kroon.onthoudHit(slachtoffer, aanvaller);
			}
		}
		return true;
	}

	/** Waar iedereen staat nadat deze ronde is afgelopen. */
	public static String verzamelpuntNa(Ronde ronde) {
		return switch (ronde) {
			case BASISKAMP -> "basiskamp";
			case DOOLHOF -> "v2";
			case HORDE -> "v3";
			case EI -> "kring";
			case KING, FFA, FINALE -> "tribune_1";
		};
	}

	// Kijker worden

	/**
	 * Maakt van deze speler een kijker en zet hem op de tribune van dat moment.
	 *
	 * @param doodtekst groot in beeld een willekeurige doodtekst, alleen voor hem
	 */
	public static void maakKijker(MinecraftServer server, ServerPlayer speler, Spullen spullen, boolean doodtekst) {
		String punt = volgendTribunepunt(Spel.ronde());
		maakKijkerOp(server, speler, punt, spullen, doodtekst);
		Spel.status(speler).tribunepunt = punt;
	}

	/**
	 * Kijker op een verzamelpunt in plaats van de tribune (uit het doolhof naar v2). Hij wordt
	 * daar niet vastgehouden: er is geen vloer waar hij af moet blijven.
	 */
	public static void maakKijkerOp(MinecraftServer server, ServerPlayer speler, String puntNaam, Spullen spullen, boolean doodtekst) {
		SpelerStatus st = Spel.status(speler);
		if (Kroon.draagtKroon(speler)) {
			Kroon.neemAf(speler);
		}
		switch (spullen) {
			case HOUDEN -> {
			}
			case BEWAREN -> bewaarInventory(speler, st);
			case LEGEN -> speler.getInventory().clearContent();
		}
		Spel.zetRol(server, speler, Rol.KIJKER);
		st.tribunepunt = null;
		speler.setGameMode(GameType.ADVENTURE);
		speler.removeEffect(MobEffects.GLOWING);
		Kroon.toonOpLocator(speler, false);
		Opstelling.ontdooi(speler);
		Mc.heal(speler);
		Spel.naarPunt(speler, puntNaam);
		if (doodtekst) {
			String tekst = Doodteksten.kies(ConfigStore.get().doodteksten(), Spel.RANDOM);
			Mc.title(speler, Mc.tekst(tekst, ChatFormatting.RED, ChatFormatting.BOLD), null, 5, 70, 20);
		}
	}

	/** Verdeelt kijkers om en om over de tribunepunten. */
	private static String volgendTribunepunt(Ronde ronde) {
		String prefix = ronde == Ronde.HORDE ? "tribune_horde_" : "tribune_";
		int aantal = ronde == Ronde.HORDE ? 2 : 4;
		List<String> bestaand = new ArrayList<>();
		for (int i = 1; i <= aantal; i++) {
			if (Spel.punt(prefix + i) != null) {
				bestaand.add(prefix + i);
			}
		}
		if (bestaand.isEmpty()) {
			return prefix + 1;
		}
		return bestaand.get(Math.floorMod(volgendeTribune++, bestaand.size()));
	}

	// Inventory bewaren (ronde 2)

	private static void bewaarInventory(ServerPlayer speler, SpelerStatus st) {
		Inventory inv = speler.getInventory();
		if (st.bewaard == null) {
			List<ItemStack> kopie = new ArrayList<>(inv.getContainerSize());
			for (int i = 0; i < inv.getContainerSize(); i++) {
				kopie.add(inv.getItem(i).copy());
			}
			st.bewaard = kopie;
		}
		inv.clearContent();
		speler.inventoryMenu.broadcastChanges();
	}

	/** Geeft terug wat {@link Spullen#BEWAREN} heeft weggezet. Wie niks bewaard had houdt wat hij heeft. */
	public static void geefBewaardTerug(ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.bewaard == null) {
			return;
		}
		Inventory inv = speler.getInventory();
		inv.clearContent();
		for (int i = 0; i < st.bewaard.size() && i < inv.getContainerSize(); i++) {
			inv.setItem(i, st.bewaard.get(i));
		}
		st.bewaard = null;
		speler.inventoryMenu.broadcastChanges();
	}

	// Op de tribune houden

	/** Elke servertick: een kijker die toch op de vloer komt gaat terug naar zijn tribunepunt. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % TERUGZET_ELKE_TICKS != 0) {
			return;
		}
		Regio verboden = switch (Spel.ronde()) {
			case HORDE -> Spel.regio("arena");
			case KING, FFA, FINALE -> Spel.regio("vloer");
			default -> null;
		};
		if (verboden == null) {
			return;
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			boolean hoortOpTribune = st.rol == Rol.KIJKER || st.rol == Rol.FINALIST;
			if (hoortOpTribune && st.tribunepunt != null && verboden.bevat(s.getX(), s.getZ())) {
				Spel.naarPunt(s, st.tribunepunt);
			}
		}
	}

	// /bc kijker

	/** Noodknop: iemand met de hand op de tribune zetten. */
	public static String handmatigAan(MinecraftServer server, ServerPlayer speler) {
		if (Mc.isStaff(speler)) {
			return Mc.naam(speler) + " staat in creative of spectator; de mod blijft van staff af";
		}
		Spullen spullen = Spel.ronde() == Ronde.HORDE ? Spullen.BEWAREN : Spel.ronde().inArena() ? Spullen.LEGEN : Spullen.HOUDEN;
		Spel.status(speler).dood = true;
		maakKijker(server, speler, spullen, false);
		return null;
	}

	/** Noodknop: iemand van de tribune halen en weer mee laten doen. */
	public static String handmatigUit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.rol != Rol.KIJKER) {
			return Mc.naam(speler) + " is geen kijker";
		}
		st.dood = false;
		st.tribunepunt = null;
		Ronde ronde = Spel.ronde();
		Rol rol = !Spel.loopt() ? Rol.SPELER : switch (ronde) {
			case KING -> Rol.HUNTER;
			case FFA -> Rol.FFA;
			default -> Rol.SPELER;
		};
		Spel.zetRol(server, speler, rol);
		speler.setGameMode(ronde.survival() && Spel.loopt() ? GameType.SURVIVAL : GameType.ADVENTURE);
		geefBewaardTerug(speler);
		String terug = switch (ronde) {
			case HORDE -> "arena_spawn";
			case KING, FFA -> "hunter_1";
			default -> null;
		};
		if (Spel.loopt() && terug != null) {
			Spel.naarPunt(speler, terug);
		}
		return null;
	}
}
