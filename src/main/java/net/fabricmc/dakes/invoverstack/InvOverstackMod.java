package net.fabricmc.dakes.invoverstack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.dakes.invoverstack.command.InvOverstackCommand;
import net.fabricmc.dakes.invoverstack.config.ConfigManager;
import net.fabricmc.dakes.invoverstack.config.ModConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvOverstackMod implements ModInitializer {
	public static final String MOD_ID = "invoverstack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ModConfig config;

	@Override
	public void onInitialize() {
		LOGGER.info("InvOverstack mod initializing...");

		// Phase 2: Load configuration
		config = ConfigManager.loadConfig();
		LOGGER.info("Configuration loaded - Default stack size: {}, Max allowed: {}, Debug mode: {}",
				config.defaultMaxStackSize, config.maxAllowedStackSize, config.debugMode);

		if (config.debugMode) {
			LOGGER.warn("DEBUG MODE ENABLED - Verbose logging active!");
		}

		// Phase 2: Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			InvOverstackCommand.register(dispatcher);
		});
		LOGGER.info("Commands registered");

		// TODO Phase 3: Register mixins and utilities
		// TODO Phase 4: Set up transfer handlers

		LOGGER.info("InvOverstack mod initialized - Server-side stack size modifications enabled");
	}

	/**
	 * Gets the current mod configuration.
	 *
	 * @return The active ModConfig instance
	 */
	public static ModConfig getConfig() {
		return config;
	}
}
