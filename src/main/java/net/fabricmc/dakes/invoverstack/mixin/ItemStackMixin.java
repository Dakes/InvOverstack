package net.fabricmc.dakes.invoverstack.mixin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.dakes.invoverstack.InvOverstackMod;
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

import java.util.Optional;

/**
 * Replaces ItemStack codecs to allow serialization of stacks > 99 in player inventories.
 * Vanilla codec clamps count to 1-99, we allow up to Integer.MAX_VALUE.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    @Final
    @Mutable
    private static MapCodec<ItemStack> MAP_CODEC;

    @Shadow
    @Final
    @Mutable
    private static Codec<ItemStack> CODEC;

    @Shadow
    @Final
    @Mutable
    private static Codec<ItemStack> OPTIONAL_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceCodecs(CallbackInfo ci) {
        try {
            MAP_CODEC = MapCodec.recursive(
                    "ItemStack",
                    codec -> RecordCodecBuilder.mapCodec(
                            instance -> instance.group(
                                    Item.ENTRY_CODEC.fieldOf("id").forGetter(ItemStack::getRegistryEntry),
                                    Codecs.rangedInt(1, Integer.MAX_VALUE).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                                    ComponentChanges.CODEC.optionalFieldOf("components", ComponentChanges.EMPTY).forGetter(ItemStack::getComponentChanges)
                            )
                            .apply(instance, ItemStack::new)
                    )
            );

            CODEC = Codec.lazyInitialized(MAP_CODEC::codec);

            OPTIONAL_CODEC = Codecs.optional(CODEC).xmap(
                    optional -> optional.orElse(ItemStack.EMPTY),
                    stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack)
            );

            InvOverstackMod.LOGGER.info("[ItemStackMixin] Replaced codecs to allow counts > 99");
        } catch (Exception e) {
            InvOverstackMod.LOGGER.error("Failed to replace ItemStack codecs", e);
        }
    }
}
