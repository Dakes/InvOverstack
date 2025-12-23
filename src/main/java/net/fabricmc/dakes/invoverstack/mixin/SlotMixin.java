package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.InvOverstackMod;
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
 * Provides context-aware stack limits for slots.
 * Player inventory slots use configured limits, container slots use vanilla limits.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    @Final
    public Inventory inventory;

    @Inject(method = "getMaxItemCount()I", at = @At("HEAD"), cancellable = true)
    private void onGetMaxItemCount(CallbackInfoReturnable<Integer> cir) {
        try {
            Slot self = (Slot) (Object) this;
            ItemStack currentStack = self.getStack();
            int effectiveMax = StackContext.getEffectiveMaxStackSize(currentStack, this.inventory);

            if (InvOverstackMod.getConfig() != null && InvOverstackMod.getConfig().debugMode) {
                InvOverstackMod.LOGGER.info("[SlotMixin] getMaxItemCount() -> {}", effectiveMax);
            }

            cir.setReturnValue(effectiveMax);
        } catch (Exception e) {
            // Graceful degradation
        }
    }

    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I",
            at = @At("HEAD"), cancellable = true)
    private void onGetMaxItemCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        try {
            int effectiveMax = StackContext.getEffectiveMaxStackSize(stack, this.inventory);

            if (InvOverstackMod.getConfig() != null && InvOverstackMod.getConfig().debugMode) {
                InvOverstackMod.LOGGER.info("[SlotMixin] getMaxItemCount(stack) -> {}", effectiveMax);
            }

            cir.setReturnValue(effectiveMax);
        } catch (Exception e) {
            // Graceful degradation
        }
    }
}
