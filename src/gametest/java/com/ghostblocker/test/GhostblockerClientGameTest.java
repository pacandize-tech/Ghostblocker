package com.ghostblocker.test;

import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.dungeon.rooms.RoomData;
import com.ghostblocker.dungeon.rooms.RoomScanner;
import com.ghostblocker.dungeon.rooms.Rotation;
import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.util.SkyblockLocation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.ghostblocker.feature.InputLocks;
import com.ghostblocker.feature.StashPicker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Exercises every Ghostblocker mixin and overlay in a real 26.3 client. Screenshots go to build/run/clientGameTest/screenshots. */
public class GhostblockerClientGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("ghostblocker-test");

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @a hotbar.0 with minecraft:diamond_sword");
			server.runCommand("item replace entity @a hotbar.1 with minecraft:golden_apple 16");
			context.waitTicks(10);

			// Camera lock: mouse turns are ignored.
			context.runOnClient(mc -> {
				InputLocks.cameraLocked = true;
				float before = mc.player.getYRot();
				mc.player.turn(200, 0);
				check(mc.player.getYRot() == before, "camera lock should block turning");
				InputLocks.cameraLocked = false;
				mc.player.turn(200, 0);
				check(mc.player.getYRot() != before, "turning should work again when unlocked");
			});

			// Hotbar lock: slot changes from input are ignored.
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(0);
				InputLocks.hotbarLocked = true;
				mc.player.getInventory().setSelectedSlot(4);
				check(mc.player.getInventory().getSelectedSlot() == 0, "hotbar lock should keep slot 0");
				InputLocks.hotbarLocked = false;
			});

			// Slot lock: Q can't drop the sword from locked slot 0.
			context.runOnClient(mc -> {
				GhostblockerConfig.get().lockedSlots.clear();
				GhostblockerConfig.get().lockedSlots.add(0);
				GhostblockerConfig.get().lockedSlots.add(10);
				mc.gameMode.dropItem(mc.player, true);
			});
			context.waitTicks(10);
			boolean kept = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().getItem(0).is(Items.DIAMOND_SWORD));
			check(kept, "locked slot item must not be dropped");
			LOGGER.info("Locked slot kept its item after pressing drop: {}", kept);

			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitTicks(5);
			context.takeScreenshot("ghostblocker-slot-lock");

			// Terminal solver: "Click in order!" highlights the lowest red pane.
			context.runOnClient(mc -> {
				SimpleContainer box = new SimpleContainer(27);
				int[] counts = {7, 3, 12, 1, 9, 5, 14, 2, 11};
				for (int i = 0; i < counts.length; i++) {
					box.setItem(10 + i % 7 + (i / 7) * 9, new ItemStack(Items.STAINED_GLASS_PANE.red(), counts[i]));
				}
				box.setItem(0, new ItemStack(Items.STAINED_GLASS_PANE.lime(), 4));
				ChestMenu menu = ChestMenu.threeRows(100, mc.player.getInventory(), box);
				mc.gui.setScreen(new ContainerScreen(menu, mc.player.getInventory(), Component.literal("Click in order!")));
			});
			context.waitTicks(5);
			context.takeScreenshot("ghostblocker-terminal-order");

			// Stash picker: search box filters the stash menu.
			context.runOnClient(mc -> {
				SimpleContainer box = new SimpleContainer(27);
				box.setItem(0, new ItemStack(Items.DIAMOND, 64));
				box.setItem(1, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));
				box.setItem(2, new ItemStack(Items.DIAMOND_BLOCK, 12));
				box.setItem(3, new ItemStack(Items.IRON_INGOT, 40));
				box.setItem(4, new ItemStack(Items.ENDER_PEARL, 16));
				ChestMenu menu = ChestMenu.threeRows(101, mc.player.getInventory(), box);
				mc.gui.setScreen(new ContainerScreen(menu, mc.player.getInventory(), Component.literal("Item Stash")));
			});
			context.waitTicks(5);
			context.runOnClient(mc -> {
				check(StashPicker.searchBox() != null, "stash menu should get a search box");
				StashPicker.searchBox().setValue("diamond");
			});
			context.waitTicks(2);
			context.takeScreenshot("ghostblocker-stash-search");

			// Stash chat message gets a [Choose items] button instead of anything being auto-picked-up.
			context.runOnClient(mc -> {
				Component modified = StashPicker.onGameMessage(Component.literal("You have 12 items stashed away!"));
				LOGGER.info("Stash message becomes: {}", modified.getString());
				check(modified.getString().contains("[Choose items]"), "stash message should offer [Choose items]");
				mc.gui.setScreen(null);
			});

			GhostblockerConfig.get().lockedSlots.clear();

			// Secret routes: build a fake one-cell room, teach the scanner its fingerprint as "Beams-2",
			// and check that the real Beams-2 route is detected, drawn and advanced.
			server.runCommand("fill -8 68 -8 22 68 22 minecraft:glass");
			server.runCommand("fill 7 12 7 7 80 7 minecraft:stone_bricks");
			server.runCommand("fill 7 40 7 7 47 7 minecraft:mossy_cobblestone");
			server.runCommand("setblock -8 80 -8 minecraft:blue_terracotta");
			// Beams-2's first secret (room-relative 7,81,2) lands at world -1,81,-6 for this anchor.
			server.runCommand("setblock -1 81 -6 minecraft:chest");
			server.runCommand("tp @a 2 69 12 172 -30"); // looking up at the first secret
			context.waitTicks(20);
			context.runOnClient(mc -> {
				int core = RoomScanner.coreForTesting(7, 7);
				RoomScanner.registerForTesting(new RoomData("Beams-2", RoomData.Type.NORMAL, List.of(core), 0, 3, 0, "1x1"));
				check(!SkyblockLocation.inDungeon(), "should not think we're in the Catacombs yet");
			});
			// A Hypixel-style sidebar: the line's text lives in the team prefix/suffix, split around a filler emoji
			// that is the score holder's name.
			server.runCommand("scoreboard objectives add sb dummy \"SKYBLOCK\"");
			server.runCommand("scoreboard objectives setdisplay sidebar sb");
			server.runCommand("team add line5");
			server.runCommand("team modify line5 prefix \" ⏣ The Catac\"");
			server.runCommand("team modify line5 suffix \"ombs (E)\"");
			server.runCommand("team join line5 §7🎂");
			server.runCommand("scoreboard players set §7🎂 sb 5");
			context.waitTicks(20);
			context.runOnClient(mc -> {
				LOGGER.info("Sidebar lines: {}", SkyblockLocation.sidebarLines());
				check(SkyblockLocation.inDungeon(), "Hypixel-style sidebar should be read as the Catacombs");
			});
			context.waitTicks(20);
			context.runOnClient(mc -> {
				check(RouteTracker.room() != null, "room should be detected");
				check("Beams-2".equals(RouteTracker.room().name()), "should detect Beams-2, got " + RouteTracker.room().name());
				check(RouteTracker.room().rotation == Rotation.SOUTH, "rotation should be SOUTH, got " + RouteTracker.room().rotation);
				check(!RouteTracker.steps().isEmpty(), "Beams-2 should have a route");
				check("interact".equals(RouteTracker.currentStep().secretType()), "first Beams-2 secret should be an interact");
				LOGGER.info("Detected {} rotated {} with {} route steps", RouteTracker.room().name(), RouteTracker.room().rotation, RouteTracker.steps().size());
			});
			context.waitTicks(5);
			context.takeScreenshot("ghostblocker-secret-route");

			// Right-clicking the chest (through the real useItemOn path) completes step 1.
			context.runOnClient(mc -> {
				BlockPos chest = new BlockPos(-1, 81, -6);
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(chest), Direction.SOUTH, chest, false));
			});
			context.waitTicks(10);
			context.runOnClient(mc -> {
				check(RouteTracker.stepIndex() == 1, "opening the chest should advance to step 2, at " + RouteTracker.stepIndex());
				LOGGER.info("Chest click advanced the route to step {}/{}", RouteTracker.stepIndex() + 1, RouteTracker.steps().size());
				mc.gui.setScreen(null);
			});
			context.waitTicks(5);
			LOGGER.info("GHOSTBLOCKER TEST PASSED");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
