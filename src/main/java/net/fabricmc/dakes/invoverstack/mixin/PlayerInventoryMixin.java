package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    // Cache max stack sizes per item to avoid millions of StackContext calls during rapid crafting
    @Unique
    private final Map<Item, Integer> invoverstack$maxStackCache = new ConcurrentHashMap<>();

    /**
     * Get cached max stack size for an item in this player inventory.
     * Caches result to avoid repeated calls to StackContext during crafting operations.
     */
    @Unique
    private int invoverstack$getCachedMaxStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 64;
        }

        Item item = stack.getItem();
        return invoverstack$maxStackCache.computeIfAbsent(item,
            k -> StackContext.getEffectiveMaxStackSize(stack, (PlayerInventory) (Object) this));
    }

    /**
     * @author InvOverstack
     * @reason Optimized slot search to avoid millions of component comparisons during rapid crafting
     */
    @Overwrite
    public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.isStackable()) {
            return -1;
        }

        PlayerInventory self = (PlayerInventory) (Object) this;
        net.minecraft.item.Item targetItem = stack.getItem();
        int maxCount = invoverstack$getCachedMaxStack(stack);

        // Single pass through main inventory + hotbar (36 slots total)
        for (int i = 0; i < 36; i++) {
            ItemStack slotStack = self.getStack(i);

            // Quick rejection filters before expensive comparison
            if (slotStack.isEmpty()) continue;
            if (slotStack.getCount() >= maxCount) continue;
            if (slotStack.getItem() != targetItem) continue;

            // Only do expensive component comparison if all quick checks passed
            if (ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * @author InvOverstack
     * @reason Use configured stack limits for auto-pickup
     */
    @Overwrite
    private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
        // Early bailouts - cheapest checks first to avoid expensive NBT comparison
        if (existingStack.isEmpty()) return false;
        if (!existingStack.isStackable()) return false;

        // Quick item type check before expensive component/NBT comparison
        if (existingStack.getItem() != stack.getItem()) return false;

        int maxCount = invoverstack$getCachedMaxStack(existingStack);
        if (existingStack.getCount() >= maxCount) return false;

        // Only do expensive NBT/component comparison if all cheap checks passed
        return ItemStack.areItemsAndComponentsEqual(existingStack, stack);
    }

    /**
     * @author InvOverstack
     * @reason Use configured stack limits when adding to slots
     */
    @Overwrite
    private int addStack(int slot, ItemStack stack) {
        PlayerInventory self = (PlayerInventory) (Object) this;

        int remainingCount = stack.getCount();
        ItemStack slotStack = self.getStack(slot);

        if (slotStack.isEmpty()) {
            // Empty slot - add as much as we can up to max stack size
            int maxCount = invoverstack$getCachedMaxStack(stack);
            int toAdd = Math.min(remainingCount, maxCount);
            self.setStack(slot, stack.copyWithCount(toAdd));
            return remainingCount - toAdd;
        }

        // Slot has items - try to merge
        int maxCount = invoverstack$getCachedMaxStack(slotStack);
        int roomInSlot = maxCount - slotStack.getCount();
        int toAdd = Math.min(remainingCount, roomInSlot);

        if (toAdd > 0) {
            slotStack.increment(toAdd);
            slotStack.setBobbingAnimationTime(5);
            return remainingCount - toAdd;
        }

        return remainingCount;
    }
}
