package net.fabricmc.dakes.invoverstack.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getStack();

    /**
     * Prevent item merging when combined count would exceed vanilla max.
     * Without this, items over the vanilla stack limit are voided during merge.
     */
    @Inject(method = "tryMerge(Lnet/minecraft/entity/ItemEntity;)V", at = @At("HEAD"), cancellable = true)
    private void preventVoidingMerge(ItemEntity other, CallbackInfo ci) {
        ItemStack ourStack = this.getStack();
        ItemStack theirStack = other.getStack();

        // Only check if items can stack together
        if (ItemStack.areItemsAndComponentsEqual(ourStack, theirStack)) {
            int combinedCount = ourStack.getCount() + theirStack.getCount();
            int vanillaMax = ourStack.getMaxCount(); // Vanilla limit (64 for most items)

            // If merging would exceed vanilla max, prevent merge to avoid voiding items
            if (combinedCount > vanillaMax) {
                ci.cancel();
            }
        }
    }
}
