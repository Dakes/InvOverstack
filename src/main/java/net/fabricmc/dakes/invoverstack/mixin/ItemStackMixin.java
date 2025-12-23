package net.fabricmc.dakes.invoverstack.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.dakes.invoverstack.util.StackContext;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Provides context-aware stack size for ItemStack queries and NBT persistence.
 * Assumes player inventory context when inventory is unknown.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxCount()I", at = @At("HEAD"), cancellable = true)
    private void onGetMaxCount(CallbackInfoReturnable<Integer> cir) {
        try {
            ItemStack self = (ItemStack) (Object) this;
            int effectiveMax = StackContext.getEffectiveMaxStackSize(self);
            int vanillaMax = self.getItem().getMaxCount();

            if (InvOverstackMod.getConfig() != null && InvOverstackMod.getConfig().debugMode) {
                InvOverstackMod.LOGGER.info("[ItemStackMixin] getMaxCount() for {}: vanilla={}, effective={}",
                    self.getItem(), vanillaMax, effectiveMax);
            }

            if (effectiveMax != vanillaMax) {
                cir.setReturnValue(effectiveMax);
            }
        } catch (Exception e) {
            // Graceful degradation
        }
    }

    @Inject(method = "isStackable()Z", at = @At("HEAD"), cancellable = true)
    private void onIsStackable(CallbackInfoReturnable<Boolean> cir) {
        try {
            ItemStack self = (ItemStack) (Object) this;

            if (self.isDamageable() || StackContext.isBlacklisted(self)) {
                return;
            }

            int effectiveMax = StackContext.getEffectiveMaxStackSize(self);
            cir.setReturnValue(effectiveMax > 1);
        } catch (Exception e) {
            // Graceful degradation
        }
    }

    @Shadow
    @Final
    @Mutable
    private static MapCodec<ItemStack> MAP_CODEC;

    @Shadow
    @Final
    @Mutable
    private static Codec<ItemStack> CODEC;

    // Replace vanilla codec to allow NBT persistence beyond 99
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceCodec(CallbackInfo ci) {
        try {
            MAP_CODEC = MapCodec.recursive(
                    "ItemStack",
                    codec -> RecordCodecBuilder.mapCodec(
                            instance -> instance.group(
                                    Item.ENTRY_CODEC.fieldOf("id").forGetter(ItemStack::getRegistryEntry),
                                    Codecs.rangedInt(1, 32767).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                                    ComponentChanges.CODEC.optionalFieldOf("components", ComponentChanges.EMPTY).forGetter(ItemStack::getComponentChanges)
                            )
                            .apply(instance, ItemStack::new)
                    )
            );
            CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
        } catch (Exception e) {
            InvOverstackMod.LOGGER.error("Failed to replace ItemStack codecs", e);
        }
    }
}
