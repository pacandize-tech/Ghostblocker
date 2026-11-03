package com.ghostblocker.dungeon;

import com.ghostblocker.config.GhostblockerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Highlights the right slots in Floor 7 terminals. Highlights only; you still do the clicking. */
public final class TerminalSolver {
	private static Screen cachedScreen;
	private static long cachedTick = -1;
	private static Map<Integer, TerminalLogic.Hint> cachedHints = Map.of();

	public static int highlight(Screen screen, Slot slot) {
		GhostblockerConfig c = GhostblockerConfig.get();
		if (!c.terminalSolver || !(screen instanceof AbstractContainerScreen<?> gui) || slot.container instanceof Inventory) return 0;
		String title = ChatFormatting.stripFormatting(screen.getTitle().getString());
		if (title == null || !TerminalLogic.isTerminal(title)) return 0;

		// Solve once per frame-ish: the menu contents change as you click.
		long tick = System.nanoTime() / 50_000_000L;
		if (screen != cachedScreen || tick != cachedTick) {
			cachedScreen = screen;
			cachedTick = tick;
			cachedHints = TerminalLogic.solve(title, items(gui));
		}
		TerminalLogic.Hint hint = cachedHints.getOrDefault(slot.index, TerminalLogic.Hint.NONE);
		return switch (hint) {
			case NEXT -> c.highlightPrimary;
			case LATER -> c.highlightSecondary;
			case NONE -> 0;
		};
	}

	private static List<TerminalLogic.Item> items(AbstractContainerScreen<?> gui) {
		List<TerminalLogic.Item> items = new ArrayList<>();
		for (Slot slot : gui.getMenu().slots) {
			if (slot.container instanceof Inventory) break;
			ItemStack stack = slot.getItem();
			String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
			items.add(new TerminalLogic.Item(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
					stack.getCount(), name == null ? "" : name, stack.hasFoil()));
		}
		return items;
	}

	private TerminalSolver() {
	}
}
