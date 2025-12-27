package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrafterBlockEntity.class)
public abstract class CrafterBlockEntityMixin {

    @Inject(method = "isValid(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsValid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            CrafterBlockEntity self = (CrafterBlockEntity) (Object) this;

            if (self.isSlotDisabled(slot)) {
                cir.setReturnValue(false);
                return;
            }

            ItemStack existingStack = self.getStack(slot);
            int effectiveMax = StackContext.getEffectiveMaxStackSize(existingStack, self);
            int currentCount = existingStack.getCount();

            if (currentCount >= effectiveMax) {
                cir.setReturnValue(false);
                return;
            }

            // Allow insertion if slot is empty
            if (existingStack.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }

            // Check if there's a better slot (vanilla logic from betterSlotExists)
            boolean betterSlotExists = false;
            for (int i = slot + 1; i < 9; i++) {
                if (!self.isSlotDisabled(i)) {
                    ItemStack otherStack = self.getStack(i);
                    if (otherStack.isEmpty() ||
                        (otherStack.getCount() < currentCount && ItemStack.areItemsAndComponentsEqual(otherStack, existingStack))) {
                        betterSlotExists = true;
                        break;
                    }
                }
            }

            cir.setReturnValue(!betterSlotExists);
        } catch (Exception e) {
            // Graceful degradation
        }
    }
}
