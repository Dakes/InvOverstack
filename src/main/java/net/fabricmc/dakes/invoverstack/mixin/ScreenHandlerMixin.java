package net.fabricmc.dakes.invoverstack.mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes comparator output to use vanilla stack limits (64) for accurate signal strength.
 * This is the surgical fix that allows Easy Shulker Boxes to work while keeping comparators correct.
 */
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "calculateComparatorOutput(Lnet/minecraft/inventory/Inventory;)I", at = @At("HEAD"), cancellable = true)
    private static void onCalculateComparatorOutput(Inventory inventory, CallbackInfoReturnable<Integer> cir) {
        if (inventory == null) {
            cir.setReturnValue(0);
            return;
        }

        int i = 0;
        float f = 0.0F;

        for (int j = 0; j < inventory.size(); j++) {
            ItemStack itemStack = inventory.getStack(j);
            if (!itemStack.isEmpty()) {
                // Force vanilla max stack size (64) for comparator calculations
                // This ensures comparators show correct signal strength regardless of mod stack sizes
                int vanillaMax = Math.min(64, itemStack.getItem().getMaxCount());
                f += (float)itemStack.getCount() / (float)vanillaMax;
                i++;
            }
        }

        f /= (float)inventory.size();
        cir.setReturnValue(MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0));
    }
}
