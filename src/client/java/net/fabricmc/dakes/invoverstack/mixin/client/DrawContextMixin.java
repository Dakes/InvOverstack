package net.fabricmc.dakes.invoverstack.mixin.client;

import net.fabricmc.dakes.invoverstack.util.NumberFormatter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

	/**
	 * Intercepts drawStackOverlay to format large stack counts.
	 * This prevents cramping for stacks >= 1000 by abbreviating to "1k", "10k", "0.1kk", etc.
	 */
	@Inject(
		method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void onDrawStackOverlay(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
		// Only modify if no custom text is provided and stack count >= 1000
		if (countText != null) {
			return; // Custom text already provided, don't modify
		}

		int count = stack.getCount();
		if (count <= 1) {
			return; // No count display for single items (vanilla behavior)
		}

		// Format large counts
		if (count >= 1000) {
			String formattedCount = NumberFormatter.formatStackCount(count);

			// Call the variant with custom text using our formatted count
			// We need to cancel this call and manually invoke the other variant
			DrawContext self = (DrawContext) (Object) this;
			self.drawStackOverlay(textRenderer, stack, x, y, formattedCount);

			ci.cancel(); // Prevent vanilla rendering
		}
	}
}
