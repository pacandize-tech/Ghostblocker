package com.ghostblocker.feature;

import com.ghostblocker.config.GhostblockerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Locked slots refer to player inventory indices (0-8 hotbar, 9-35 main, 36-39 armor, 40 offhand),
 * so a lock follows the slot into every container screen.
 */
public final class SlotLock {
	public static boolean isLocked(int inventoryIndex) {
		GhostblockerConfig c = GhostblockerConfig.get();
		return c.slotLocking && c.lockedSlots.contains(inventoryIndex);
	}

	public static boolean isLocked(Slot slot) {
		return slot != null && slot.container instanceof Inventory && isLocked(slot.getContainerSlot());
	}

	public static void toggle(Slot slot) {
		if (slot == null || !(slot.container instanceof Inventory)) return;
		int index = slot.getContainerSlot();
		GhostblockerConfig c = GhostblockerConfig.get();
		boolean nowLocked = c.lockedSlots.add(index);
		if (!nowLocked) c.lockedSlots.remove(index);
		GhostblockerConfig.save();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendOverlayMessage(Component.literal(nowLocked ? "§cSlot locked" : "§aSlot unlocked"));
		}
	}

	/** Whether a click in a container should be blocked because it would move a locked item. */
	public static boolean blocksClick(Slot slot, int button, ContainerInput input) {
		if (isLocked(slot)) return true;
		// Number-key swaps move the item in hotbar slot `button` (40 = offhand) too.
		return input == ContainerInput.SWAP && isLocked(button);
	}

	private SlotLock() {
	}
}
