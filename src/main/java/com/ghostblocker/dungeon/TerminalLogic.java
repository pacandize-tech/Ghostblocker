package com.ghostblocker.dungeon;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure solver logic for the Floor 7 terminals, independent of Minecraft classes so it can be unit tested.
 * Given the terminal's title and its items, returns which slots to highlight.
 */
public final class TerminalLogic {
	public enum Hint { NONE, NEXT, LATER }

	/** One slot of the terminal menu. {@code itemId} is like "minecraft:red_stained_glass_pane". */
	public record Item(String itemId, int count, String name, boolean enchanted) {
	}

	private static final Pattern STARTS_WITH = Pattern.compile("What starts with: '(\\w)'\\?");
	private static final Pattern SELECT_COLOR = Pattern.compile("Select all the ([A-Z ]+) items!");
	private static final String RED_PANE = "minecraft:red_stained_glass_pane";

	private static final Map<String, String> SPECIAL_COLOR_ITEMS = Map.of(
			"minecraft:bone_meal", "white",
			"minecraft:ink_sac", "black",
			"minecraft:cocoa_beans", "brown",
			"minecraft:lapis_lazuli", "blue");

	public static boolean isTerminal(String title) {
		return title.equals("Click in order!") || title.equals("Correct all the panes!")
				|| STARTS_WITH.matcher(title).matches() || SELECT_COLOR.matcher(title).matches();
	}

	/** Hint for every slot index; slots not in the map get no hint. */
	public static Map<Integer, Hint> solve(String title, List<Item> items) {
		Map<Integer, Hint> hints = new HashMap<>();
		if (title.equals("Click in order!")) {
			solveOrder(items, hints);
		} else if (title.equals("Correct all the panes!")) {
			for (int i = 0; i < items.size(); i++) {
				if (RED_PANE.equals(items.get(i).itemId())) hints.put(i, Hint.NEXT);
			}
		} else {
			Matcher startsWith = STARTS_WITH.matcher(title);
			Matcher color = SELECT_COLOR.matcher(title);
			if (startsWith.matches()) {
				String letter = startsWith.group(1).toLowerCase(Locale.ROOT);
				for (int i = 0; i < items.size(); i++) {
					Item item = items.get(i);
					if (!item.enchanted() && !isPane(item) && item.name().toLowerCase(Locale.ROOT).startsWith(letter)) {
						hints.put(i, Hint.NEXT);
					}
				}
			} else if (color.matches()) {
				String wanted = normalizeColor(color.group(1));
				for (int i = 0; i < items.size(); i++) {
					Item item = items.get(i);
					if (!item.enchanted() && wanted.equals(colorOf(item.itemId()))) hints.put(i, Hint.NEXT);
				}
			}
		}
		return hints;
	}

	/** Red panes must be clicked in ascending stack size: highlight the next one and the two after it. */
	private static void solveOrder(List<Item> items, Map<Integer, Hint> hints) {
		Integer[] order = new Integer[items.size()];
		int n = 0;
		for (int i = 0; i < items.size(); i++) {
			if (RED_PANE.equals(items.get(i).itemId())) order[n++] = i;
		}
		Integer[] red = java.util.Arrays.copyOf(order, n);
		java.util.Arrays.sort(red, java.util.Comparator.comparingInt(i -> items.get(i).count()));
		for (int k = 0; k < red.length && k < 3; k++) {
			hints.put(red[k], k == 0 ? Hint.NEXT : Hint.LATER);
		}
	}

	private static boolean isPane(Item item) {
		return item.itemId().endsWith("_stained_glass_pane");
	}

	static String normalizeColor(String titleColor) {
		String c = titleColor.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		return c.equals("silver") ? "light_gray" : c;
	}

	/** The dye colour an item belongs to, e.g. "light_blue" for light_blue_wool, or null. */
	static String colorOf(String itemId) {
		String special = SPECIAL_COLOR_ITEMS.get(itemId);
		if (special != null) return special;
		String path = itemId.substring(itemId.indexOf(':') + 1);
		for (String color : COLORS) {
			if (path.startsWith(color + "_")) return color;
		}
		return null;
	}

	// Two-word colours are checked first so "light_gray_wool" isn't matched by a shorter colour.
	private static final List<String> COLORS = List.of(
			"light_blue", "light_gray", "white", "orange", "magenta", "yellow", "lime", "pink", "gray",
			"cyan", "purple", "blue", "brown", "green", "red", "black");

	private TerminalLogic() {
	}
}
