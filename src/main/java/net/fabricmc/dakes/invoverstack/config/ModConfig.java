package net.fabricmc.dakes.invoverstack.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration data class for InvOverstack mod.
 * Contains all configurable settings for stack size modifications.
 */
public class ModConfig {

    /**
     * Default maximum stack size for items in player inventory.
     * Default: 512
     */
    public int defaultMaxStackSize = 512;

    /**
     * Maximum allowed stack size cap (to prevent overflow issues).
     * Default: 4096
     */
    public int maxAllowedStackSize = 4096;

    /**
     * Per-item stack size overrides.
     * Maps item identifier (e.g., "minecraft:dirt") to custom stack size.
     */
    public Map<String, Integer> perItemOverrides = new HashMap<>();

    /**
     * Enabled item categories that should have increased stack sizes.
     * Examples: "blocks", "foods", "tools"
     * Empty set means all items (except blacklisted) are enabled.
     */
    public Set<String> enabledItemCategories = new HashSet<>();

    /**
     * Item blacklist - items that should NEVER have modified stack sizes.
     * Uses item identifiers (e.g., "minecraft:shulker_box")
     */
    public Set<String> itemBlacklist = new HashSet<>();

    /**
     * Whether to automatically normalize oversized stacks when mod is disabled.
     * If true, splits stacks >64 when inventory is closed.
     */
    public boolean autoNormalize = false;

    /**
     * Creates a new ModConfig with default values and default blacklist.
     */
    public ModConfig() {
        initializeDefaultBlacklist();
    }

    /**
     * Initializes the default item blacklist with problematic items.
     */
    private void initializeDefaultBlacklist() {
        // Shulker boxes (nesting issues)
        itemBlacklist.add("minecraft:shulker_box");
        itemBlacklist.add("minecraft:white_shulker_box");
        itemBlacklist.add("minecraft:orange_shulker_box");
        itemBlacklist.add("minecraft:magenta_shulker_box");
        itemBlacklist.add("minecraft:light_blue_shulker_box");
        itemBlacklist.add("minecraft:yellow_shulker_box");
        itemBlacklist.add("minecraft:lime_shulker_box");
        itemBlacklist.add("minecraft:pink_shulker_box");
        itemBlacklist.add("minecraft:gray_shulker_box");
        itemBlacklist.add("minecraft:light_gray_shulker_box");
        itemBlacklist.add("minecraft:cyan_shulker_box");
        itemBlacklist.add("minecraft:purple_shulker_box");
        itemBlacklist.add("minecraft:blue_shulker_box");
        itemBlacklist.add("minecraft:brown_shulker_box");
        itemBlacklist.add("minecraft:green_shulker_box");
        itemBlacklist.add("minecraft:red_shulker_box");
        itemBlacklist.add("minecraft:black_shulker_box");

        // Bundles
        itemBlacklist.add("minecraft:bundle");

        // Note: Tools and armor with durability are handled separately
        // via ItemStack.isDamageableItem() check in the code

        // Potions and enchanted books (recipe consumption bugs)
        itemBlacklist.add("minecraft:potion");
        itemBlacklist.add("minecraft:splash_potion");
        itemBlacklist.add("minecraft:lingering_potion");
        itemBlacklist.add("minecraft:enchanted_book");
    }

    /**
     * Gets the effective stack size for a given item identifier.
     *
     * @param itemId The item identifier (e.g., "minecraft:dirt")
     * @return The configured stack size, or -1 if item is blacklisted
     */
    public int getStackSizeForItem(String itemId) {
        // Check blacklist first
        if (itemBlacklist.contains(itemId)) {
            return -1;
        }

        // Check per-item override
        if (perItemOverrides.containsKey(itemId)) {
            return Math.min(perItemOverrides.get(itemId), maxAllowedStackSize);
        }

        // Return default
        return Math.min(defaultMaxStackSize, maxAllowedStackSize);
    }

    /**
     * Validates the configuration values and fixes any invalid settings.
     */
    public void validate() {
        // Ensure stack sizes are within reasonable bounds
        if (defaultMaxStackSize < 1) {
            defaultMaxStackSize = 64;
        }
        if (defaultMaxStackSize > maxAllowedStackSize) {
            defaultMaxStackSize = maxAllowedStackSize;
        }
        if (maxAllowedStackSize < 64) {
            maxAllowedStackSize = 64;
        }
        if (maxAllowedStackSize > 32767) { // Short.MAX_VALUE / 2 for safety
            maxAllowedStackSize = 32767;
        }

        // Validate per-item overrides
        perItemOverrides.replaceAll((key, value) -> {
            if (value < 1) return 64;
            if (value > maxAllowedStackSize) return maxAllowedStackSize;
            return value;
        });
    }
}
