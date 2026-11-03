package com.ghostblocker.mixin;

import com.ghostblocker.feature.InputLocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar lock: ignore scroll-wheel and number-key slot changes on the local player. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	@Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
	private void ghostblocker$hotbarLock(int slot, CallbackInfo ci) {
		if (InputLocks.hotbarLocked && !InputLocks.serverSettingSlot && player == Minecraft.getInstance().player) {
			ci.cancel();
		}
	}
}
