package com.ghostblocker.mixin;

import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.feature.InputLocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packet hooks. Handlers first run on the network thread and bail out at ensureRunningOnSameThread, then run
 * again on the main thread, so every hook here is placed just after that call to fire exactly once.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	private static final String ON_MAIN_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

	@Shadow
	private ClientLevel level;

	/** The server can always change our held slot, even with the hotbar lock on, so we never desync. */
	@Inject(method = "handleSetHeldSlot", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
	private void ghostblocker$allowServerSlot(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
		InputLocks.serverSettingSlot = true;
	}

	@Inject(method = "handleSetHeldSlot", at = @At("RETURN"))
	private void ghostblocker$endServerSlot(ClientboundSetHeldSlotPacket packet, CallbackInfo ci) {
		InputLocks.serverSettingSlot = false;
	}

	/** Item secrets: we picked up an item. Runs before the item entity is removed so its position is known. */
	@Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
	private void ghostblocker$itemPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (level == null || mc.player == null || packet.getPlayerId() != mc.player.getId()) return;
		Entity item = level.getEntity(packet.getItemId());
		if (item instanceof ItemEntity) RouteTracker.onItemPickup(item.getX(), item.getY(), item.getZ());
	}

	/** Bat secrets: a bat died nearby. */
	@Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = ON_MAIN_THREAD, shift = At.Shift.AFTER))
	private void ghostblocker$batDeath(ClientboundSoundPacket packet, CallbackInfo ci) {
		if (packet.getSound().value() == SoundEvents.BAT_DEATH) {
			RouteTracker.onBatDeath(packet.getX(), packet.getY(), packet.getZ());
		}
	}
}
