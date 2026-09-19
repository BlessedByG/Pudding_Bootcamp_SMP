package nl.pudding.bootcamp.game.ronde1;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Kompas;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;

/**
 * Ronde 1: vind de uitgang van het doolhof. Wie in regio {@code doolhof_uit} staat is eruit en
 * gaat naar {@code v2}; de eerste vijf krijgen het voorsprongkistje. Na de timer gaat iedereen
 * naar {@code v2}. Niemand ligt eruit.
 */
public final class Doolhof extends RondeLogica {
	private static final String POORT = "doolhof";
	private static final int CHECK_ELKE_TICKS = 5;

	private int aangekomen;
	private boolean hintGegeven;

	@Override
	public Ronde ronde() {
		return Ronde.DOOLHOF;
	}

	@Override
	protected String startpunt() {
		return "doolhof_start";
	}

	@Override
	public String magStarten(MinecraftServer server) {
		String fout = Spel.buitenRegio("doolhof", "doolhof_start");
		if (fout != null) {
			return fout;
		}
		Regio uit = Spel.regio("doolhof_uit");
		if (!Spel.regio("doolhof").bevat(uit.centerX(), uit.centerZ())) {
			return "regio doolhof_uit ligt buiten regio doolhof: niemand kan er dan komen, want de border staat om doolhof";
		}
		return null;
	}

	@Override
	public void start(MinecraftServer server) {
		Poorten.dichtAlsHijBestaat(server, POORT);
		Spelregels.locatorBar(server, false);
		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.naarPunt(s, startpunt());
		}
		Border.zet(server, Spel.regio("doolhof"));
		Aftelling.start(5, "Het doolhof opent over", () -> {
			Poorten.openAlsHijBestaat(server, POORT);
			Spel.startTimer(ronde().duurSeconden());
		});
	}

	@Override
	public void tick(MinecraftServer server) {
		if (!Spel.timerLoopt() || server.getTickCount() % CHECK_ELKE_TICKS != 0) {
			return;
		}
		Regio uit = Spel.regio("doolhof_uit");
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (!Spel.status(s).klaar && uit.bevat(s.getX(), s.getZ())) {
				eruit(server, s);
			}
		}
		if (Spel.levend(server, Rol.SPELER).isEmpty()) {
			einde(server);
		}
	}

	private void eruit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		st.klaar = true;
		aangekomen++;
		Tribune.maakKijkerOp(server, speler, "v2", Tribune.Spullen.HOUDEN, false);

		if (Regels.krijgtVoorsprong(aangekomen)) {
			// Het voorsprongkistje: 1 gapple en 1 ender pearl.
			Kits.geefOfDrop(speler, new ItemStack(Items.GOLDEN_APPLE));
			Kits.geefOfDrop(speler, new ItemStack(Items.ENDER_PEARL));
			Mc.title(speler, Mc.tekst("ERUIT! #" + aangekomen, ChatFormatting.GREEN, ChatFormatting.BOLD),
					Mc.tekst("Voorsprong: een gapple en een pearl", ChatFormatting.YELLOW));
		} else {
			Mc.title(speler, Mc.tekst("ERUIT! #" + aangekomen, ChatFormatting.GREEN, ChatFormatting.BOLD), null);
		}
		Mc.geluid(speler, SoundEvents.PLAYER_LEVELUP, 1f, 1f);
	}

	@Override
	public void seconde(MinecraftServer server) {
		if (!Spel.timerLoopt()) {
			return;
		}
		Bossbar.zet(BossbarTekst.doolhof(Spel.timer()), BossEvent.BossBarColor.GREEN, Spel.timerDeel());
		if (!hintGegeven && Spel.timer() <= Regels.DOOLHOF_HINT_BIJ) {
			hintGegeven = true;
			Regio doolhof = Spel.regio("doolhof");
			Regio uit = Spel.regio("doolhof_uit");
			String kant = Kompas.richting(uit.centerX() - doolhof.centerX(), uit.centerZ() - doolhof.centerZ());
			for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
				Mc.title(s, Mc.tekst("HINT", ChatFormatting.YELLOW, ChatFormatting.BOLD),
						Mc.tekst("De uitgang ligt aan de " + kant + "kant", ChatFormatting.WHITE));
			}
		}
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server);
	}

	/** In het doolhof ga je niet dood (de gevaarlijke gangen zijn vervelend, niet dodelijk); gebeurt het toch, dan terug naar de ingang. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		Spel.naarPunt(speler, startpunt());
	}

	private void einde(MinecraftServer server) {
		Spel.stop(server);
		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			SpelerStatus st = Spel.status(s);
			if (!st.klaar) {
				st.klaar = true;
				Spel.naarPunt(s, "v2");
			}
		}
		Mc.titleAllen(server, Mc.tekst("RONDE 1 VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(aangekomen + " spelers vonden de uitgang", ChatFormatting.WHITE));
	}

	@Override
	public void end(MinecraftServer server) {
		Poorten.dichtAlsHijBestaat(server, POORT);
	}
}
