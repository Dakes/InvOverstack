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
 * Mixin for the ItemStack class to provide context-aware stack size information.
 * <p>
 * This mixin modifies how ItemStacks report their maximum stack size. While
 * {@link SlotMixin} handles slot-level operations, this mixin ensures that
 * ItemStack queries also return appropriate values.
 * </p>
 *
 * <h2>Design Considerations</h2>
 * <p>
 * <strong>Challenge:</strong> ItemStack.getMaxStackSize() has no inventory context.
 * We don't know if the stack is in a player inventory or a chest.
 * </p>
 * <p>
 * <strong>Solution:</strong> We assume player inventory context for ItemStack queries.
 * This is a reasonable default because:
 * <ul>
 *   <li>Most ItemStack queries happen in player inventory contexts</li>
 *   <li>Slot-level checks (via SlotMixin) override this for containers</li>
 *   <li>Server authoritative validation happens at the slot level</li>
 * </ul>
 * </p>
 *
 * <h2>Why Both Mixins?</h2>
 * <ul>
 *   <li><strong>SlotMixin:</strong> Authoritative slot-level checks with full context</li>
 *   <li><strong>ItemStackMixin:</strong> Default stack size for UI, tooltips, general queries</li>
 * </ul>
 *
 * @see SlotMixin
 * @see StackContext
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * Intercepts {@code ItemStack.getMaxStackSize()} to provide configured stack limits.
     * <p>
     * This method is called in many contexts:
     * - Tooltip display
     * - Inventory UI rendering
     * - Item pickup logic
     * - Crafting result validation
     * </p>
     *
     * <h3>Important Note</h3>
     * <p>
     * Since we don't have inventory context here, we assume player inventory.
     * This means:
     * - Players will see correct stack sizes in their inventory
     * - Containers will still be limited by SlotMixin's authoritative checks
     * </p>
     *
     * <h3>Method Signature (Mojang Mappings)</h3>
     * {@code public int getMaxStackSize()}
     *
     * @param cir Callback info for returning a value and potentially cancelling
     */
    @Inject(method = "getMaxCount()I", at = @At("HEAD"), cancellable = true)
    private void onGetMaxCount(CallbackInfoReturnable<Integer> cir) {
        try {
            ItemStack self = (ItemStack) (Object) this;

            // Get effective max without inventory context (assumes player inventory)
            int effectiveMax = StackContext.getEffectiveMaxStackSize(self);

            // Only override if our configured size is different from vanilla
            // This minimizes mod interference
            if (effectiveMax != self.getItem().getMaxCount()) {
                cir.setReturnValue(effectiveMax);
            }
        } catch (Exception e) {
            // Graceful degradation - let vanilla handle errors
        }
    }

    /**
     * Intercepts {@code ItemStack.isStackable()} to ensure correct stackability.
     * <p>
     * An item is considered stackable if its max stack size > 1.
     * Since we're changing max stack sizes, we need to ensure this method
     * returns correct values.
     * </p>
     *
     * <h3>Why This Matters</h3>
     * <p>
     * Various systems check {@code isStackable()}:
     * - Item merging logic
     * - Pickup behavior
     * - Inventory organization
     * </p>
     *
     * <h3>Method Signature (Mojang Mappings)</h3>
     * {@code public boolean isStackable()}
     *
     * @param cir Callback info for returning a boolean value
     */
    @Inject(method = "isStackable()Z", at = @At("HEAD"), cancellable = true)
    private void onIsStackable(CallbackInfoReturnable<Boolean> cir) {
        try {
            ItemStack self = (ItemStack) (Object) this;

            // Check if item is damageable (tools, armor, etc.)
            // These should never be stackable regardless of configuration
            if (self.isDamageable()) {
                // Let vanilla handle it (will return false)
                return;
            }

            // Check if item is blacklisted
            if (StackContext.isBlacklisted(self)) {
                // Let vanilla handle it
                return;
            }

            // Get our configured max stack size
            int effectiveMax = StackContext.getEffectiveMaxStackSize(self);

            // An item is stackable if max stack size > 1
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

    /**
     * Replaces the MAP_CODEC and CODEC to allow larger stack sizes in NBT.
     * <p>
     * The vanilla codec uses {@code rangedInt(1, 99)} which clamps to [1, 99].
     * We replace it with a codec that allows up to 32767.
     * </p>
     */
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
