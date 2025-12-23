package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for the Slot class to implement context-aware stack size limits.
 * <p>
 * This is the primary mixin that enables the core functionality of InvOverstack.
 * It intercepts calls to {@code getMaxStackSize()} methods and returns different
 * values based on whether the slot belongs to a player inventory or a container.
 * </p>
 *
 * <h2>Why This Works</h2>
 * <p>
 * Minecraft uses {@code Slot.getMaxStackSize()} to determine how many items can
 * be placed in a slot. By intercepting this method and checking the slot's
 * inventory context, we can allow larger stacks in player inventories while
 * keeping container inventories at vanilla limits.
 * </p>
 *
 * <h2>Injection Strategy</h2>
 * <ul>
 *   <li>Target: {@code getMaxStackSize()} and {@code getMaxStackSize(ItemStack)}</li>
 *   <li>Injection point: HEAD (before any other code runs)</li>
 *   <li>Cancellable: true (we replace the return value entirely)</li>
 * </ul>
 *
 * @see StackContext
 * @see Slot
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    /**
     * Shadow field to access the slot's underlying inventory.
     * This is marked @Final because it's set in the Slot constructor and never changes.
     */
    @Shadow
    @Final
    public Inventory inventory;

    /**
     * Intercepts {@code Slot.getMaxStackSize()} to provide context-aware stack limits.
     * <p>
     * This overload is called when checking slot limits without a specific ItemStack.
     * We use the current stack in the slot if available, otherwise return a safe default.
     * </p>
     *
     * <h3>Method Signature (Mojang Mappings)</h3>
     * {@code public int getMaxStackSize()}
     *
     * <h3>Injection Details</h3>
     * <ul>
     *   <li>at = @At("HEAD"): Inject at the very beginning</li>
     *   <li>cancellable = true: Allow us to cancel and return our own value</li>
     * </ul>
     *
     * @param cir Callback info for returning a value and potentially cancelling
     */
    @Inject(method = "getMaxItemCount()I", at = @At("HEAD"), cancellable = true)
    private void onGetMaxItemCount(CallbackInfoReturnable<Integer> cir) {
        try {
            // Get the current stack in this slot
            Slot self = (Slot) (Object) this;
            ItemStack currentStack = self.getStack();

            // Calculate the effective max stack size based on context
            int effectiveMax = StackContext.getEffectiveMaxStackSize(currentStack, this.inventory);

            // Cancel the original method and return our value
            cir.setReturnValue(effectiveMax);
        } catch (Exception e) {
            // If anything goes wrong, don't interfere - let vanilla logic handle it
            // This ensures the mod degrades gracefully on errors
        }
    }

    /**
     * Intercepts {@code Slot.getMaxStackSize(ItemStack)} for stack-specific limits.
     * <p>
     * This overload is called when checking if a specific ItemStack can fit in the slot.
     * It's used during drag-and-drop, shift-click, and other item transfer operations.
     * </p>
     *
     * <h3>Method Signature (Mojang Mappings)</h3>
     * {@code public int getMaxStackSize(ItemStack stack)}
     *
     * <h3>Why This Matters</h3>
     * <p>
     * This is called frequently during inventory operations:
     * - When hovering items over slots
     * - During shift-click transfers
     * - When the server validates item movement
     * </p>
     *
     * @param stack The ItemStack being checked
     * @param cir   Callback info for returning a value and potentially cancelling
     */
    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I",
            at = @At("HEAD"), cancellable = true)
    private void onGetMaxItemCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        try {
            // Calculate the effective max stack size for this specific item and context
            int effectiveMax = StackContext.getEffectiveMaxStackSize(stack, this.inventory);

            // Cancel the original method and return our value
            cir.setReturnValue(effectiveMax);
        } catch (Exception e) {
            // Graceful degradation on error
        }
    }
}
