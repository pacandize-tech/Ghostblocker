package com.ghostblocker.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.ghostblocker.Ghostblocker;
import com.ghostblocker.feature.SlotLock;
import com.ghostblocker.feature.SlotOverlays;
import com.ghostblocker.feature.StashPicker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void ghostblocker$drawOverlays(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		SlotOverlays.draw(this, graphics, slot);
	}

	@Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
			at = @At("HEAD"), cancellable = true)
	private void ghostblocker$blockLockedSlots(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
		if (SlotLock.blocksClick(slot, button, input)) ci.cancel();
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void ghostblocker$keys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		// While typing in the stash search box, don't let E/Q/number keys act on the container.
		if (StashPicker.isTyping(this)) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				StashPicker.searchBox().setFocused(false);
				setFocused(null);
			} else {
				StashPicker.searchBox().keyPressed(event);
			}
			cir.setReturnValue(true);
			return;
		}
		if (Ghostblocker.LOCK_SLOT_KEY.matches(event)) {
			SlotLock.toggle(((AbstractContainerScreenAccessor) this).ghostblocker$getHoveredSlot());
			cir.setReturnValue(true);
		}
	}
}
