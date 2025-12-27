package net.fabricmc.dakes.invoverstack.util;

import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.dakes.invoverstack.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Debug logging utility that logs in development environments OR when debugMode is enabled in config.
 * Set debugMode=true in config/invoverstack.json to enable debug logs in production.
 * Automatically suppresses duplicate consecutive log messages to reduce spam.
 */
public class DebugLogger {
    private static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

    // Track recent messages to avoid spam (max 100 unique messages)
    private static final Set<String> recentMessages = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * Check if debug logging is enabled (dev mode OR config flag).
     * @return true if debug logging should be enabled
     */
    private static boolean isDebugEnabled() {
        if (IS_DEV) {
            return true;
        }
        try {
            return ConfigManager.getConfig() != null && ConfigManager.getConfig().debugMode;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Log debug message if debug is enabled and message is not a duplicate.
     * @param message the message to log
     */
    public static void debug(String message) {
        if (isDebugEnabled()) {
            synchronized (recentMessages) {
                if (recentMessages.size() >= MAX_CACHE_SIZE) {
                    recentMessages.clear();
                }
                if (recentMessages.add(message)) {
                    InvOverstackMod.LOGGER.info("[DEBUG] " + message);
                }
            }
        }
    }

    /**
     * Log debug message with formatting if debug is enabled and message is not a duplicate.
     * @param format the format string
     * @param args the arguments
     */
    public static void debug(String format, Object... args) {
        if (isDebugEnabled()) {
            String message = String.format(format, args);
            synchronized (recentMessages) {
                if (recentMessages.size() >= MAX_CACHE_SIZE) {
                    recentMessages.clear();
                }
                if (recentMessages.add(message)) {
                    InvOverstackMod.LOGGER.info("[DEBUG] " + message);
                }
            }
        }
    }

    /**
     * Clear the duplicate message cache.
     * Useful when starting a new test scenario.
     */
    public static void clearCache() {
        recentMessages.clear();
    }

    /**
     * Check if we're in development mode.
     * @return true if in development environment
     */
    public static boolean isDevMode() {
        return IS_DEV;
    }
}
