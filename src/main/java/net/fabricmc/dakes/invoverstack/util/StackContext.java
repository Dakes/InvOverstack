package net.fabricmc.dakes.invoverstack.util;

import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.dakes.invoverstack.config.ModConfig;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Utility class for detecting inventory context and determining appropriate stack sizes.
 * <p>
 * This class is the core logic that distinguishes between player inventories
 * (which get increased stack sizes) and container inventories (which remain vanilla).
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>Player inventories get configured stack sizes (default 512)</li>
 *   <li>All other inventories (chests, hoppers, etc.) remain vanilla (typically 64)</li>
 *   <li>Blacklisted items never get modified stack sizes</li>
 *   <li>Items with durability are handled based on configuration</li>
 * </ul>
 *
 * @see ModConfig
 */
public class StackContext {

    /**
     * Determines if the given inventory is a player inventory.
     * <p>
     * This is the fundamental check that drives the entire mod's behavior.
     * Only player inventories get the stack size boost.
     * </p>
     *
     * @param inventory The inventory to check
     * @return true if this is a player inventory, false otherwise
     */
    public static boolean isPlayerInventory(Inventory inventory) {
        return inventory instanceof net.minecraft.entity.player.PlayerInventory;
    }

    /**
     * Gets the effective maximum stack size for an item in a given context.
     * <p>
     * This method encapsulates all the logic for determining stack sizes:
     * <ol>
     *   <li>Check if item is blacklisted → return vanilla max</li>
     *   <li>Check if item has durability → return vanilla max (tools shouldn't stack)</li>
     *   <li>Check if inventory is player inventory → return configured max</li>
     *   <li>Otherwise → return vanilla max</li>
     * </ol>
     * </p>
     *
     * @param stack     The ItemStack to check
     * @param inventory The inventory context (can be null for general queries)
     * @return The maximum stack size for this item in this context
     */
    public static int getEffectiveMaxStackSize(ItemStack stack, Inventory inventory) {
        if (stack == null || stack.isEmpty()) {
            return 64; // Default vanilla stack size
        }

        Item item = stack.getItem();
        int vanillaMax = item.getMaxCount();

        // Get item identifier for config lookup
        Identifier itemId = Registries.ITEM.getId(item);
        String itemIdString = itemId.toString();

        // Get configuration
        ModConfig config = InvOverstackMod.getConfig();
        if (config == null) {
            // Config not loaded yet, return vanilla
            return vanillaMax;
        }

        // Check blacklist first - blacklisted items always use vanilla max
        if (config.itemBlacklist.contains(itemIdString)) {
            return vanillaMax;
        }

        // Check if item has durability (tools, armor, etc.)
        // These should not stack beyond vanilla limits
        if (stack.isDamageable()) {
            return vanillaMax;
        }

        // If we have an inventory context, check if it's a player inventory
        if (inventory != null) {
            if (isPlayerInventory(inventory)) {
                // Player inventory - use configured stack size
                int configuredSize = config.getStackSizeForItem(itemIdString);

                // If getStackSizeForItem returns -1, item is blacklisted
                if (configuredSize == -1) {
                    return vanillaMax;
                }

                return configuredSize;
            } else {
                // Container inventory - use vanilla max
                return vanillaMax;
            }
        }

        // No inventory context - assume player inventory for now
        // This handles cases like item pickup, crafting, etc.
        int configuredSize = config.getStackSizeForItem(itemIdString);

        if (configuredSize == -1) {
            return vanillaMax;
        }

        return configuredSize;
    }

    /**
     * Gets the effective maximum stack size for an item without inventory context.
     * <p>
     * This variant is used when we don't have an inventory reference but need
     * to determine stack size (e.g., for UI display, item pickup, etc.).
     * It assumes player inventory context.
     * </p>
     *
     * @param stack The ItemStack to check
     * @return The maximum stack size for this item (assuming player context)
     */
    public static int getEffectiveMaxStackSize(ItemStack stack) {
        return getEffectiveMaxStackSize(stack, null);
    }

    /**
     * Checks if an item is blacklisted from stack size modifications.
     *
     * @param stack The ItemStack to check
     * @return true if the item is blacklisted, false otherwise
     */
    public static boolean isBlacklisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ModConfig config = InvOverstackMod.getConfig();
        if (config == null) {
            return false;
        }

        Item item = stack.getItem();
        Identifier itemId = Registries.ITEM.getId(item);

        return config.itemBlacklist.contains(itemId.toString());
    }

    /**
     * Validates that a stack count is within acceptable bounds for its context.
     * <p>
     * This is used for safety checks when loading items from disk or receiving
     * from network packets.
     * </p>
     *
     * @param stack     The ItemStack to validate
     * @param inventory The inventory context
     * @return A safe count value (clamped to valid range)
     */
    public static int getSafeStackCount(ItemStack stack, Inventory inventory) {
        int currentCount = stack.getCount();
        int maxAllowed = getEffectiveMaxStackSize(stack, inventory);

        // Clamp to valid range [0, maxAllowed]
        return Math.max(0, Math.min(currentCount, maxAllowed));
    }
}
