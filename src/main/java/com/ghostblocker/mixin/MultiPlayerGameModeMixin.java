package com.ghostblocker.mixin;

import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.feature.SlotLock;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	/** Pressing Q never drops an item from a locked hotbar slot. */
	@Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
	private void ghostblocker$noDropLocked(LocalPlayer player, boolean fullStack, CallbackInfo ci) {
		if (SlotLock.isLocked(player.getInventory().getSelectedSlot())) {
			player.sendOverlayMessage(Component.literal("§cThat slot is locked"));
			ci.cancel();
		}
	}

	/** Chest, lever and wither-essence secrets: we right-clicked a block. */
	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void ghostblocker$interact(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
		if (hand == InteractionHand.MAIN_HAND) RouteTracker.onInteract(hit.getBlockPos());
	}
}
