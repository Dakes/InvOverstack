package net.fabricmc.dakes.invoverstack;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvOverstackMod implements ModInitializer {
	public static final String MOD_ID = "invoverstack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("InvOverstack mod initialized - Server-side stack size modifications enabled");

		// TODO Phase 2: Load configuration
		// TODO Phase 3: Register mixins and utilities
		// TODO Phase 4: Set up transfer handlers
	}
}
