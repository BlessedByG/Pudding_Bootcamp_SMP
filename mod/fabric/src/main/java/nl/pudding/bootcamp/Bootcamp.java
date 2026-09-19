package nl.pudding.bootcamp;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bootcamp implements ModInitializer {
	public static final String MOD_ID = "bootcamp";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOG.info("Pudding Bootcamp geladen");
	}
}
