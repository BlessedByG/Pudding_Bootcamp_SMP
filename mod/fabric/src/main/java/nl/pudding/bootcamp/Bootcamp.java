package nl.pudding.bootcamp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import nl.pudding.bootcamp.commands.BcCommand;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerReset;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.game.Teams;
import nl.pudding.bootcamp.game.ronde2.Horde;
import nl.pudding.bootcamp.rad.RadSpel;
import nl.pudding.bootcamp.setup.Wand;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Zweefkroon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bootcamp implements ModInitializer {
	public static final String MOD_ID = "bootcamp";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// De volgorde hier is de volgorde waarin /bc reset opruimt.
		Spel.init();
		RadSpel.init();
		Tribune.init();
		Kroon.init();
		Opstelling.init();
		Zweefkroon.init();
		Sidebar.init();
		Horde.init();
		Poorten.init();
		SpelerReset.init();
		Teams.init();
		Spelregels.init();
		Border.init();
		Bossbar.init();
		Wand.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> BcCommand.registreer(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Standaardbestanden.schrijfOntbrekende();
			ConfigStore.laad(server);
			Spelregels.zet(server);
			Teams.zorgDatZeBestaan(server);
			Zweefkroon.ruimAllesOp(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ConfigStore.bewaar());

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Wand.tick(server);
			Spel.tick(server);
			RadSpel.tick(server);
			Tribune.tick(server);
			Zweefkroon.tick(server);
		});

		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> Spel.onJoin(server, listener.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> Spel.onQuit(server, listener.getPlayer()));

		LOG.info("Pudding Bootcamp geladen");
	}
}
