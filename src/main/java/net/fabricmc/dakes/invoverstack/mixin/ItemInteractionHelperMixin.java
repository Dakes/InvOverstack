package net.fabricmc.dakes.invoverstack.mixin;

import net.fabricmc.dakes.invoverstack.config.ConfigManager;
import net.fabricmc.dakes.invoverstack.util.DebugLogger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

/**
 * Disables Easy Shulker Boxes functionality to prevent item voiding.
 * Can be controlled via config: disableEasyShulkerBoxes
 */
@Mixin(targets = "fuzs.iteminteractions.impl.world.item.container.ItemInteractionHelper", remap = false)
public class ItemInteractionHelperMixin {

    @Inject(
        method = "addStack(Ljava/util/function/Supplier;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Ljava/util/function/ToIntFunction;ILjava/util/function/ToIntBiFunction;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void disableEasyShulkerBoxes(
            Supplier<?> containerSupplier,
            PlayerEntity player,
            ItemStack newStack,
            ToIntFunction<ItemStack> acceptableItemCount,
            int prioritizedSlot,
            ToIntBiFunction<?, ItemStack> maxStackSize,
            CallbackInfoReturnable<Integer> cir) {

        if (ConfigManager.getConfig().disableEasyShulkerBoxes) {
            cir.setReturnValue(0);
        }
    }
}
