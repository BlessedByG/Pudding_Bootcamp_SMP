package nl.pudding.bootcamp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import nl.pudding.bootcamp.commands.BcCommand;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.game.SpelerReset;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.game.Teams;
import nl.pudding.bootcamp.setup.Wand;
import nl.pudding.bootcamp.visuals.Bossbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bootcamp implements ModInitializer {
	public static final String MOD_ID = "bootcamp";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// De volgorde hier is de volgorde waarin /bc reset opruimt.
		SpelerReset.init();
		Teams.init();
		Spelregels.init();
		Bossbar.init();
		Wand.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> BcCommand.registreer(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Standaardbestanden.schrijfOntbrekende();
			ConfigStore.laad(server);
			Spelregels.zet(server);
			Teams.zorgDatZeBestaan(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ConfigStore.bewaar());

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Wand.tick(server);
			if (server.getTickCount() % 20 == 0) {
				Bossbar.iedereenErbij(server);
			}
		});

		LOG.info("Pudding Bootcamp geladen");
	}
}
