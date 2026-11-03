package com.ghostblocker.mixin;

import com.ghostblocker.feature.InputLocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera lock: mouse movement no longer turns the local player. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void ghostblocker$cameraLock(double yRot, double xRot, CallbackInfo ci) {
		if (InputLocks.cameraLocked && (Object) this == Minecraft.getInstance().player) {
			ci.cancel();
		}
	}
}
