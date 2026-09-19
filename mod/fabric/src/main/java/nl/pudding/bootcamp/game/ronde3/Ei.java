package nl.pudding.bootcamp.game.ronde3;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.BossbarTekst;
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
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.List;

/**
 * Ronde 3: vind het Grote Ei, hak je naar binnen, pak loot en neem een diamond block mee. Dat
 * block is je ticket: in regio {@code eiplaat} wordt het ingenomen en sta je bij De Kring. Wie na
 * de timer geen ticket heeft gaat alsnog door, met een lege inventory en de basiskit.
 */
public final class Ei extends RondeLogica {
	private static final String POORT = "bos";
	private static final int CHECK_ELKE_TICKS = 10;
	/** Het Ei is ongeveer vijftien hoog en de beacon zit eronder: de vuurpijl start erboven. */
	private static final int VUURPIJL_BOVEN_BEACON = 22;

	private boolean beaconAan;
	private boolean vuurpijlAf;
	private BlockState stondOpBeaconplek;

	@Override
	public Ronde ronde() {
		return Ronde.EI;
	}

	@Override
	protected String startpunt() {
		return "ei_start";
	}

	@Override
	public String magStarten(MinecraftServer server) {
		// Zonder basis.json krijgt wie geen ticket haalt straks niks.
		String fout = Kits.controleer(server, "ei", "basis");
		if (fout == null) {
			fout = Spel.buitenRegio("eibos", "ei_start", "ei_beacon");
		}
		if (fout != null) {
			return fout;
		}
		Regio plaat = Spel.regio("eiplaat");
		if (!Spel.regio("eibos").bevat(plaat.centerX(), plaat.centerZ())) {
			return "regio eiplaat ligt buiten regio eibos: niemand kan er dan komen, want de border staat om eibos";
		}
		return null;
	}

	@Override
	public void start(MinecraftServer server) {
		Poorten.dichtAlsHijBestaat(server, POORT);
		// Een ticket van een eerdere poging telt niet.
		Spel.alleStatussen().forEach(st -> st.ticket = false);
		Spelregels.locatorBar(server, false);
		Spel.maakSpelers(server, true);
		List<ServerPlayer> spelers = Mc.deelnemers(server);
		for (ServerPlayer s : spelers) {
			Spel.naarPunt(s, startpunt());
		}
		Border.zet(server, Spel.regio("eibos"));
		// Alleen de pickaxe erbij; iedereen houdt wat hij al had.
		Kits.geefAan(server, "ei", spelers);
		Aftelling.start(5, "Het bos opent over", () -> {
			Poorten.openAlsHijBestaat(server, POORT);
			Spel.startTimer(ronde().duurSeconden());
		});
	}

	@Override
	public void tick(MinecraftServer server) {
		if (!Spel.timerLoopt() || server.getTickCount() % CHECK_ELKE_TICKS != 0) {
			return;
		}
		Regio plaat = Spel.regio("eiplaat");
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			if (!Spel.status(s).ticket && plaat.bevat(s.getX(), s.getZ()) && neemDiamondBlock(s)) {
				ticket(server, s);
			}
		}
		if (Spel.levend(server, Rol.SPELER).isEmpty()) {
			einde(server);
		}
	}

	/** Haalt precies één diamond block uit de inventory. Zonder block gebeurt er niks. */
	private static boolean neemDiamondBlock(ServerPlayer speler) {
		Inventory inv = speler.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(Items.DIAMOND_BLOCK)) {
				stack.shrink(1);
				speler.inventoryMenu.broadcastChanges();
				return true;
			}
		}
		return false;
	}

	private void ticket(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		st.ticket = true;
		st.klaar = true;
		Mc.particles(Mc.wereld(server), ParticleTypes.HAPPY_VILLAGER, speler.getX(), speler.getY() + 1, speler.getZ(), 30, 0.5, 0);
		// Kijker bij De Kring, met al zijn loot: dat is zijn gear voor ronde 4.
		Tribune.maakKijkerOp(server, speler, "kring", Tribune.Spullen.HOUDEN, false);
		Mc.geluid(speler, SoundEvents.PLAYER_LEVELUP, 1f, 1f);
		Mc.title(speler, Mc.tekst("TICKET BINNEN", ChatFormatting.AQUA, ChatFormatting.BOLD),
				Mc.tekst("Je loot gaat mee naar ronde 4", ChatFormatting.WHITE));
	}

	@Override
	public void seconde(MinecraftServer server) {
		if (!Spel.timerLoopt()) {
			return;
		}
		if (!beaconAan && Spel.timer() <= Regels.EI_BEACON_BIJ) {
			beaconAan(server);
		}
		if (!vuurpijlAf && Spel.timer() <= Regels.EI_VUURPIJL_BIJ) {
			vuurpijlAf = true;
			BlokPos b = Spel.punt("ei_beacon").blokPos();
			Vuurwerk.goud(Mc.wereld(server), b.x() + 0.5, b.y() + VUURPIJL_BOVEN_BEACON, b.z() + 0.5);
		}
		Bossbar.zet(BossbarTekst.ei(Spel.timer()), beaconAan ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN, Spel.timerDeel());
	}

	/** De hint: het ontbrekende blok in de beaconpiramide erin, en de lichtstraal gaat aan. */
	private void beaconAan(MinecraftServer server) {
		beaconAan = true;
		ServerLevel wereld = Mc.wereld(server);
		BlockPos pos = blockPos(Spel.punt("ei_beacon").blokPos());
		stondOpBeaconplek = wereld.getBlockState(pos);
		wereld.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		Mc.geluidAllen(server, SoundEvents.BEACON_ACTIVATE, 1f, 1f);
		for (ServerPlayer s : Spel.levend(server, Rol.SPELER)) {
			Mc.title(s, Mc.tekst("HINT", ChatFormatting.YELLOW, ChatFormatting.BOLD), Mc.tekst("Volg de lichtstraal", ChatFormatting.WHITE));
		}
	}

	private static BlockPos blockPos(BlokPos b) {
		return new BlockPos(b.x(), b.y(), b.z());
	}

	@Override
	public void timerOp(MinecraftServer server) {
		einde(server);
	}

	/** Doodgaan in het bos (val, verdrinken): terug naar de bosrand, met je spullen. */
	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		Spel.naarPunt(speler, startpunt());
	}

	private void einde(MinecraftServer server) {
		Spel.stop(server);

		// Geen ticket is geen loot: lege inventory plus de basiskit. Dat is de straf.
		List<ServerPlayer> zonderTicket = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (!Spel.status(s).ticket) {
				s.getInventory().clearContent();
				s.inventoryMenu.broadcastChanges();
				zonderTicket.add(s);
			}
		}
		if (!zonderTicket.isEmpty()) {
			// Ontbreekt basis.json, dan staat dat in de console en blijven ze leeg.
			Kits.geefAan(server, "basis", zonderTicket);
		}

		Spel.maakSpelers(server, false);
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (!Spel.status(s).klaar) {
				Spel.status(s).klaar = true;
				Spel.naarPunt(s, "kring");
			}
		}
		int metTicket = Mc.deelnemers(server).size() - zonderTicket.size();
		Mc.titleAllen(server, Mc.tekst("RONDE 3 VOORBIJ", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(metTicket + " spelers hebben een ticket", ChatFormatting.WHITE));
		for (ServerPlayer s : zonderTicket) {
			Mc.actionbar(s, Mc.tekst("Geen ticket: je loot is weg, je krijgt de basiskit", ChatFormatting.RED));
		}
	}

	@Override
	public void end(MinecraftServer server) {
		if (stondOpBeaconplek != null) {
			// De hint weer uit, zodat de ronde opnieuw gespeeld kan worden.
			Mc.wereld(server).setBlock(blockPos(Spel.punt("ei_beacon").blokPos()), stondOpBeaconplek, Block.UPDATE_ALL);
			stondOpBeaconplek = null;
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			s.setGameMode(GameType.ADVENTURE);
		}
		Poorten.dichtAlsHijBestaat(server, POORT);
	}
}
