package net.fabricmc.dakes.invoverstack.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModConfig {

    public int defaultMaxStackSize = 512;
    public int maxAllowedStackSize = 4096;
    public Map<String, Integer> perItemOverrides = new HashMap<>();
    public Set<String> enabledItemCategories = new HashSet<>();
    public Set<String> itemBlacklist = new HashSet<>();
    public boolean autoNormalize = false;

    public ModConfig() {
        initializeDefaultBlacklist();
    }

    private void initializeDefaultBlacklist() {
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

        itemBlacklist.add("minecraft:bundle");
        itemBlacklist.add("minecraft:potion");
        itemBlacklist.add("minecraft:splash_potion");
        itemBlacklist.add("minecraft:lingering_potion");
        itemBlacklist.add("minecraft:enchanted_book");
    }

    public int getStackSizeForItem(String itemId) {
        if (itemBlacklist.contains(itemId)) {
            return -1;
        }

        if (perItemOverrides.containsKey(itemId)) {
            return Math.min(perItemOverrides.get(itemId), maxAllowedStackSize);
        }

        return Math.min(defaultMaxStackSize, maxAllowedStackSize);
    }

    public void validate() {
        if (defaultMaxStackSize < 1) {
            defaultMaxStackSize = 64;
        }
        if (defaultMaxStackSize > maxAllowedStackSize) {
            defaultMaxStackSize = maxAllowedStackSize;
        }
        if (maxAllowedStackSize < 64) {
            maxAllowedStackSize = 64;
        }
        if (maxAllowedStackSize > 32767) {
            maxAllowedStackSize = 32767;
        }

        perItemOverrides.replaceAll((key, value) -> {
            if (value < 1) return 64;
            if (value > maxAllowedStackSize) return maxAllowedStackSize;
            return value;
        });
    }
}
