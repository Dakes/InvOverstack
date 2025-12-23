package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Mixin for PlayerInventory to fix auto-pickup behavior.
 * <p>
 * This mixin overwrites methods in PlayerInventory to use our configured stack sizes.
 * </p>
 *
 * <h2>The Pickup Problem</h2>
 * <ul>
 *   <li>PlayerInventory.canStackAddMore() calls this.getMaxCount(stack)</li>
 *   <li>Inventory.getMaxCount() returns Math.min(getMaxCountPerStack(), stack.getMaxCount())</li>
 *   <li>getMaxCountPerStack() returns hardcoded 99</li>
 *   <li>So even with our mixin making stack.getMaxCount() return 1024, Math.min(99, 1024) = 99</li>
 * </ul>
 *
 * <h2>The Solution</h2>
 * <p>
 * Overwrite canStackAddMore to use our configured values directly.
 * </p>
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    /**
     * @author InvOverstack
     * @reason Fix stack size check to use configured limits instead of vanilla 99
     */
    @Overwrite
    private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
        PlayerInventory self = (PlayerInventory) (Object) this;
        int maxCount = StackContext.getEffectiveMaxStackSize(existingStack, self);

        return !existingStack.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(existingStack, stack)
                && existingStack.isStackable()
                && existingStack.getCount() < maxCount;
    }

    /**
     * @author InvOverstack
     * @reason Fix stack size limit in addStack to use configured limits
     */
    @Overwrite
    private int addStack(int slot, ItemStack stack) {
        PlayerInventory self = (PlayerInventory) (Object) this;

        int i = stack.getCount();
        ItemStack itemStack = self.getStack(slot);
        if (itemStack.isEmpty()) {
            itemStack = stack.copyWithCount(0);
            self.setStack(slot, itemStack);
        }

        int maxCount = StackContext.getEffectiveMaxStackSize(itemStack, self);
        int j = maxCount - itemStack.getCount();
        int k = Math.min(i, j);
        if (k == 0) {
            return i;
        } else {
            i -= k;
            itemStack.increment(k);
            itemStack.setBobbingAnimationTime(5);
            return i;
        }
    }
}
