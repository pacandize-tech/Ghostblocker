package com.ghostblocker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ghostblocker.Ghostblocker;
import com.ghostblocker.dungeon.routes.RouteDatabase.RouteSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/** All user settings, saved to config/ghostblocker.json. */
public class GhostblockerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ghostblocker.json");

	private static GhostblockerConfig instance = new GhostblockerConfig();

	// Stash
	public boolean stashChooseButton = true;
	public boolean stashSearch = true;
	public boolean stashFullAlert = true;

	// Inventory
	public boolean slotLocking = true;
	public Set<Integer> lockedSlots = new HashSet<>();
	public int lockColor = 0x55FF3030;

	// Dungeons
	public boolean terminalSolver = true;
	public boolean weirdosSolver = true;
	public boolean starredMobGlow = true;
	public boolean secretRoutes = true;
	public boolean showAllSecrets = false;
	public RouteSet routeSet = RouteSet.FOW;
	/** How many upcoming route steps to draw (the first one bright, the rest faded). */
	public int visibleRouteSteps = 1;

	// Highlight colours (ARGB)
	public int highlightPrimary = 0x9020E020;
	public int highlightSecondary = 0x60E0E020;

	public static GhostblockerConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try {
				GhostblockerConfig loaded = GSON.fromJson(Files.readString(FILE), GhostblockerConfig.class);
				if (loaded != null) instance = loaded;
				if (instance.lockedSlots == null) instance.lockedSlots = new HashSet<>();
				if (instance.routeSet == null) instance.routeSet = RouteSet.FOW;
			} catch (IOException | RuntimeException e) {
				Ghostblocker.LOGGER.warn("Could not read {}, using defaults", FILE, e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			Ghostblocker.LOGGER.warn("Could not save {}", FILE, e);
		}
	}
}
