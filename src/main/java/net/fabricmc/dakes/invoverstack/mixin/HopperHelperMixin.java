package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.DebugLogger;
import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts Lithium's hopper transfer to apply InvOverstack stack limits.
 *
 * Lithium's tryMoveSingleItem() always transfers 1 item by default.
 * This mixin allows oversized transfers to player inventories (512 items)
 * while keeping containers at vanilla 64 limits.
 *
 * Priority 1000 ensures this runs BEFORE Lithium (950) to control the actual transfer logic.
 * If Lithium isn't loaded, this mixin won't be instantiated.
 */
@Mixin(
    targets = "net.caffeinemc.mods.lithium.common.hopper.HopperHelper",
    priority = 1000,
    remap = false
)
public class HopperHelperMixin {

    /**
     * Hooks into Lithium's single-item transfer and expands it for oversized stacks.
     * We intercept right before the grow(1) call and allow up to context-aware maximum.
     * Note: Lithium uses Mojang mappings, so we target grow() not increment()
     */
    @Inject(
        method = "tryMoveSingleItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;grow(I)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        require = 0
    )
    private static void expandTransferForOversizedStacks(
        Inventory to,
        @Nullable Object toSidedObj,
        ItemStack transferStack,
        ItemStack transferChecker,
        int targetSlot,
        @Nullable Object fromDirectionObj,
        CallbackInfoReturnable<Boolean> cir
    ) {
        DebugLogger.debug("[HopperHelperMixin] expandTransferForOversizedStacks FIRED!");

        try {
            // Get the stack in the target slot
            ItemStack toStack = to.getStack(targetSlot);

            // This should not happen but be defensive
            if (toStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(toStack, transferStack)) {
                DebugLogger.debug("[HopperHelperMixin] Stack empty or doesn't match - falling through");
                return; // Fall through to Lithium's grow(1)
            }

            // Get context-aware max stack size
            int maxStack = StackContext.getEffectiveMaxStackSize(toStack, to);
            int currentCount = toStack.getCount();
            int spaceRemaining = maxStack - currentCount;

            boolean isPlayerInv = StackContext.isPlayerInventory(to);

            DebugLogger.debug("[HopperHelperMixin] item=%s, inv=%s, isPlayer=%b, max=%d, current=%d, space=%d",
                toStack.getItem().toString(),
                to.getClass().getSimpleName(),
                isPlayerInv,
                maxStack,
                currentCount,
                spaceRemaining);

            // If we can fit more than 1 item and target is player inventory, do oversized transfer
            if (spaceRemaining > 1 && isPlayerInv) {
                int transferAmount = Math.min(transferStack.getCount(), spaceRemaining);

                DebugLogger.debug("[HopperHelperMixin] OVERSIZED transfer to player inv: %d items (vs Lithium's 1)",
                    transferAmount);

                // Perform the transfer
                toStack.increment(transferAmount);
                transferStack.decrement(transferAmount);
                to.markDirty();

                cir.setReturnValue(true);
                return;
            }

            // Container inventory or only 1 space left: fall through to Lithium's grow(1)
            DebugLogger.debug("[HopperHelperMixin] Using Lithium's 1-item transfer (isContainer=%b, space=%d)",
                !isPlayerInv, spaceRemaining);

            // Return early without cancelling to use Lithium's grow(1)
        } catch (Exception e) {
            DebugLogger.debug("HopperHelperMixin error (falling through): %s", e.getMessage());
            // Silently fall through to Lithium's logic on any error
        }
    }
}
