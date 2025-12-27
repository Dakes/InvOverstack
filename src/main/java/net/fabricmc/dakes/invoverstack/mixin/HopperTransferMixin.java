package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.dakes.invoverstack.util.DebugLogger;
import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla hopper transfer mixin - fallback when Lithium is not loaded.
 *
 * When Lithium IS present, HopperHelperMixin handles transfers instead.
 * This provides compatibility with vanilla servers or mods that don't use Lithium.
 * Priority 500 ensures HopperHelperMixin (1000) runs first if Lithium is available.
 */
@Mixin(value = HopperBlockEntity.class, priority = 500)
public abstract class HopperTransferMixin {

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private static void onTransferToSlot(@Nullable Inventory from, Inventory to, ItemStack stack, int slot,
                                          @Nullable Direction side, CallbackInfoReturnable<ItemStack> cir) {
        DebugLogger.debug("HopperTransferMixin.onTransferToSlot() - vanilla path (Lithium not active)");
        ItemStack existingStack = to.getStack(slot);

        // Check if we can insert - if not, return original stack unchanged
        if (!to.isValid(slot, stack)) {
            cir.setReturnValue(stack);
            return;
        }

        if (existingStack.isEmpty()) {
            // Empty slot - just set the stack
            to.setStack(slot, stack);
            to.markDirty();
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        // Check if stacks can merge - if not, return original stack unchanged
        if (!ItemStack.areItemsAndComponentsEqual(existingStack, stack)) {
            cir.setReturnValue(stack);
            return;
        }

        // Get the effective max for the target inventory
        int maxStack = StackContext.getEffectiveMaxStackSize(existingStack, to);
        int currentCount = existingStack.getCount();

        DebugLogger.debug("Hopper transfer: item=%s, inventory=%s, isPlayerInv=%b, maxStack=%d, current=%d",
                existingStack.getItem().toString(),
                to.getClass().getSimpleName(),
                StackContext.isPlayerInventory(to),
                maxStack,
                currentCount);

        if (currentCount >= maxStack) {
            // Slot is full - return original stack unchanged
            cir.setReturnValue(stack);
            return;
        }

        // Calculate how much we can transfer
        int space = maxStack - currentCount;
        int transferAmount = Math.min(stack.getCount(), space);

        DebugLogger.debug("Hopper transferring %d items (space=%d)", transferAmount, space);

        // Perform the transfer
        existingStack.increment(transferAmount);
        stack.decrement(transferAmount);
        to.markDirty();

        // Return the remainder (or empty if all transferred)
        if (stack.isEmpty()) {
            cir.setReturnValue(ItemStack.EMPTY);
        } else {
            cir.setReturnValue(stack);
        }
    }
}
