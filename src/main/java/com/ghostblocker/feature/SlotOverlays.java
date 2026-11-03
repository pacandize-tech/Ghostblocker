package com.ghostblocker.feature;

import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.dungeon.TerminalSolver;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;

/** Draws every per-slot overlay: lock tint, stash search and terminal hints. */
public final class SlotOverlays {
	public static void draw(Screen screen, GuiGraphicsExtractor graphics, Slot slot) {
		int x = slot.x;
		int y = slot.y;

		int color = TerminalSolver.highlight(screen, slot);
		if (color == 0) color = StashPicker.highlight(screen, slot);
		if (color != 0) graphics.fill(x, y, x + 16, y + 16, color);

		if (SlotLock.isLocked(slot)) {
			graphics.fill(x, y, x + 16, y + 16, GhostblockerConfig.get().lockColor);
			graphics.outline(x, y, 16, 16, 0xFFFF3030);
		}
	}

	private SlotOverlays() {
	}
}
