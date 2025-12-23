package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    /**
     * @author InvOverstack
     * @reason Use configured stack limits for auto-pickup
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
     * @reason Use configured stack limits when adding to slots
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
