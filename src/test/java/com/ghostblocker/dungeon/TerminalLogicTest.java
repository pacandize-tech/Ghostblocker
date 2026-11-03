package com.ghostblocker.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ghostblocker.dungeon.TerminalLogic.Hint;
import com.ghostblocker.dungeon.TerminalLogic.Item;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TerminalLogicTest {
	private static Item pane(String color, int count) {
		return new Item("minecraft:" + color + "_stained_glass_pane", count, "", false);
	}

	private static Item item(String id, String name, boolean enchanted) {
		return new Item("minecraft:" + id, 1, name, enchanted);
	}

	@Test
	void clickInOrderHighlightsLowestRedPaneThenNextTwo() {
		List<Item> items = List.of(pane("black", 1), pane("red", 5), pane("lime", 1), pane("red", 3), pane("red", 4), pane("red", 9));
		Map<Integer, Hint> hints = TerminalLogic.solve("Click in order!", items);
		assertEquals(Hint.NEXT, hints.get(3));
		assertEquals(Hint.LATER, hints.get(4));
		assertEquals(Hint.LATER, hints.get(1));
		assertFalse(hints.containsKey(5), "only the next three are shown");
		assertFalse(hints.containsKey(2), "already-clicked lime panes are ignored");
	}

	@Test
	void correctPanesHighlightsOnlyRed() {
		List<Item> items = List.of(pane("lime", 1), pane("red", 1), pane("black", 1), pane("red", 1));
		Map<Integer, Hint> hints = TerminalLogic.solve("Correct all the panes!", items);
		assertEquals(Map.of(1, Hint.NEXT, 3, Hint.NEXT), hints);
	}

	@Test
	void startsWithIgnoresEnchantedAndPanes() {
		List<Item> items = List.of(
				item("apple", "Apple", false),
				item("arrow", "Arrow", true),
				item("bow", "Bow", false),
				pane("black", 1),
				item("anvil", "anvil", false));
		Map<Integer, Hint> hints = TerminalLogic.solve("What starts with: 'A'?", items);
		assertEquals(Map.of(0, Hint.NEXT, 4, Hint.NEXT), hints);
	}

	@Test
	void selectColorMatchesBlocksDyesAndSpecialItems() {
		List<Item> items = List.of(
				item("light_blue_wool", "Light Blue Wool", false),
				item("blue_concrete", "Blue Concrete", false),
				item("lapis_lazuli", "Lapis Lazuli", false),
				item("blue_dye", "Blue Dye", true),
				item("blue_stained_glass", "Blue Stained Glass", false));
		Map<Integer, Hint> hints = TerminalLogic.solve("Select all the BLUE items!", items);
		assertEquals(Map.of(1, Hint.NEXT, 2, Hint.NEXT, 4, Hint.NEXT), hints);
	}

	@Test
	void twoWordAndSilverColours() {
		assertEquals("light_blue", TerminalLogic.normalizeColor("LIGHT BLUE"));
		assertEquals("light_gray", TerminalLogic.normalizeColor("SILVER"));
		assertEquals("light_gray", TerminalLogic.colorOf("minecraft:light_gray_wool"));
		assertEquals("gray", TerminalLogic.colorOf("minecraft:gray_wool"));
		Map<Integer, Hint> hints = TerminalLogic.solve("Select all the SILVER items!",
				List.of(item("light_gray_wool", "", false), item("gray_wool", "", false)));
		assertEquals(Map.of(0, Hint.NEXT), hints);
	}

	@Test
	void recognisesTerminalTitles() {
		assertTrue(TerminalLogic.isTerminal("Click in order!"));
		assertTrue(TerminalLogic.isTerminal("What starts with: 'S'?"));
		assertTrue(TerminalLogic.isTerminal("Select all the LIGHT BLUE items!"));
		assertFalse(TerminalLogic.isTerminal("Item Stash"));
	}
}
