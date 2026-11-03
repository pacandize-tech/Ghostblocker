/*
 * Ghostblocker - Hypixel SkyBlock quality of life
 * World rendering approach adapted from Secret Routes Mod's RenderingBackend
 * (https://github.com/yourboykyle/SecretRoutes), Copyright 2025 yourboykyle & contributors. GPL-3.0-or-later.
 */
package com.ghostblocker.dungeon.routes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.util.SkyblockLocation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Draws the current route step (path line, etherwarp/stonk/superboom/pearl spots, the secret) and optionally every secret. */
public final class WaypointRenderer {
	private record Box(double x, double y, double z, double size, int argb) {
	}

	private record Line(Vec3 from, Vec3 to, int argb) {
	}

	private record Label(Vec3 pos, String text, int rgb) {
	}

	// Colours (ARGB)
	private static final int PATH = 0xFF30E060;
	private static final int ETHERWARP = 0x90A040FF;
	private static final int STONK = 0x90FF8020;
	private static final int SUPERBOOM = 0x90FF2020;
	private static final int INTERACT = 0x90FFE040;
	private static final int PEARL = 0x90FF60C0;
	private static final int BONZO = 0x9040A0FF;
	private static final int SECRET = 0x9020D0FF;
	private static final int OTHER_SECRET = 0x5080C0FF;
	private static final float LINE_WIDTH = 0.06f;

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(WaypointRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		GhostblockerConfig c = GhostblockerConfig.get();
		if (!c.secretRoutes || RouteTracker.room() == null || !SkyblockLocation.inDungeon()) return;

		List<Box> boxes = new ArrayList<>();
		List<Line> lines = new ArrayList<>();
		List<Label> labels = new ArrayList<>();
		collect(c, boxes, lines, labels);
		if (boxes.isEmpty() && lines.isEmpty() && labels.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		PoseStack pose = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();

		collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (p, buffer) -> {
			Matrix4f m = p.pose();
			for (Box b : boxes) cube(buffer, m, b, cam);
			for (Line l : lines) line(buffer, m, l, cam);
		});

		Font font = mc.font;
		for (Label l : labels) {
			pose.pushPose();
			pose.translate(l.pos.x - cam.x, l.pos.y - cam.y, l.pos.z - cam.z);
			pose.mulPose(new Matrix4f().rotate(camera.rotation()));
			float scale = 0.025f * (float) Math.max(1.0, Math.sqrt(l.pos.distanceTo(cam)) / 2.5);
			pose.scale(scale, -scale, scale);
			Component text = Component.literal(l.text);
			collector.submitText(pose, -font.width(text) / 2f, 0, text.getVisualOrderText(), true,
					Font.DisplayMode.SEE_THROUGH, 0xF000F0, 0xFF000000 | l.rgb, 0, 0);
			pose.popPose();
		}
	}

	private static void collect(GhostblockerConfig c, List<Box> boxes, List<Line> lines, List<Label> labels) {
		List<RouteStep> steps = RouteTracker.steps();
		int first = RouteTracker.stepIndex();
		int last = Math.min(steps.size(), first + Math.max(1, c.visibleRouteSteps));
		for (int i = first; i < last; i++) {
			RouteStep step = steps.get(i);
			boolean active = i == first;
			int fade = active ? 0xFF : 0x70;

			Vec3 prev = null;
			for (int[] p : step.path()) {
				Vec3 point = centre(p, 0.1);
				if (prev != null) lines.add(new Line(prev, point, withAlpha(PATH, fade)));
				prev = point;
			}
			marks(step.etherwarps(), ETHERWARP, "Etherwarp", active, boxes, labels);
			marks(step.mines(), STONK, "Stonk", active, boxes, labels);
			marks(step.superbooms(), SUPERBOOM, "Superboom", active, boxes, labels);
			marks(step.interacts(), INTERACT, "Interact", active, boxes, labels);
			marks(step.enderpearls(), PEARL, "Pearl", active, boxes, labels);
			marks(step.bonzoStaffs(), BONZO, "Bonzo", active, boxes, labels);

			if (step.secretPos() != null) {
				BlockPos s = RouteTracker.toWorld(step.secretPos()[0], step.secretPos()[1], step.secretPos()[2]);
				boxes.add(new Box(s.getX(), s.getY(), s.getZ(), 1.0, active ? SECRET : withAlpha(SECRET, 0x40)));
				if (active) {
					labels.add(new Label(new Vec3(s.getX() + 0.5, s.getY() + 1.4, s.getZ() + 0.5),
							secretLabel(step.secretType()) + " (" + (i + 1) + "/" + steps.size() + ")", 0x55FFFF));
				}
			}
		}

		if (c.showAllSecrets) {
			for (RouteDatabase.Secret secret : RouteDatabase.secrets(RouteTracker.room().name())) {
				if ("entrance".equals(secret.category()) || "superboom".equals(secret.category()) || "stonk".equals(secret.category())) continue;
				BlockPos pos = RouteTracker.toWorld(secret.x(), secret.y(), secret.z());
				if (RouteTracker.isFound(pos)) continue;
				boxes.add(new Box(pos.getX(), pos.getY(), pos.getZ(), 1.0, OTHER_SECRET));
				labels.add(new Label(new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5), secret.name(), 0xAAAAFF));
			}
		}
	}

	private static void marks(List<int[]> points, int color, String label, boolean active, List<Box> boxes, List<Label> labels) {
		for (int[] p : points) {
			BlockPos w = RouteTracker.toWorld(p[0], p[1], p[2]);
			boxes.add(new Box(w.getX(), w.getY(), w.getZ(), 1.0, active ? color : withAlpha(color, 0x30)));
			if (active) labels.add(new Label(new Vec3(w.getX() + 0.5, w.getY() + 1.2, w.getZ() + 0.5), label, color & 0xFFFFFF));
		}
	}

	private static Vec3 centre(int[] relative, double yOffset) {
		BlockPos w = RouteTracker.toWorld(relative[0], relative[1], relative[2]);
		return new Vec3(w.getX() + 0.5, w.getY() + yOffset, w.getZ() + 0.5);
	}

	private static String secretLabel(String type) {
		if (type == null) return "Secret";
		return switch (type) {
			case "interact" -> "Chest / Lever";
			case "item" -> "Item";
			case "bat" -> "Bat";
			case "exit", "exitroute" -> "Exit";
			default -> type;
		};
	}

	private static int withAlpha(int argb, int alpha) {
		return (alpha << 24) | (argb & 0xFFFFFF);
	}

	private static void cube(VertexConsumer b, Matrix4f m, Box box, Vec3 cam) {
		float pad = 0.002f;
		float x1 = (float) (box.x - cam.x) - pad, y1 = (float) (box.y - cam.y) - pad, z1 = (float) (box.z - cam.z) - pad;
		float x2 = x1 + (float) box.size + 2 * pad, y2 = y1 + (float) box.size + 2 * pad, z2 = z1 + (float) box.size + 2 * pad;
		float r = ((box.argb >> 16) & 0xFF) / 255f, g = ((box.argb >> 8) & 0xFF) / 255f, bl = (box.argb & 0xFF) / 255f, a = ((box.argb >>> 24) & 0xFF) / 255f;
		quad(b, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, bl, a); // bottom
		quad(b, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, bl, a); // top
		quad(b, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, bl, a); // north
		quad(b, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, bl, a); // south
		quad(b, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, bl, a); // west
		quad(b, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, bl, a); // east
	}

	/** A line drawn as a thin quad that always faces the camera. */
	private static void line(VertexConsumer b, Matrix4f m, Line l, Vec3 cam) {
		Vector3f s = new Vector3f((float) (l.from.x - cam.x), (float) (l.from.y - cam.y), (float) (l.from.z - cam.z));
		Vector3f e = new Vector3f((float) (l.to.x - cam.x), (float) (l.to.y - cam.y), (float) (l.to.z - cam.z));
		Vector3f dir = new Vector3f(e).sub(s);
		if (dir.lengthSquared() < 1e-6f) return;
		dir.normalize();
		Vector3f toCam = new Vector3f(s);
		if (toCam.lengthSquared() < 1e-6f) toCam.set(0, 1, 0);
		Vector3f width = dir.cross(toCam.normalize(), new Vector3f());
		if (width.lengthSquared() < 1e-6f) width.set(0, 1, 0);
		width.normalize().mul(LINE_WIDTH / 2);
		float r = ((l.argb >> 16) & 0xFF) / 255f, g = ((l.argb >> 8) & 0xFF) / 255f, bl = (l.argb & 0xFF) / 255f, a = ((l.argb >>> 24) & 0xFF) / 255f;
		quad(b, m, s.x - width.x, s.y - width.y, s.z - width.z, s.x + width.x, s.y + width.y, s.z + width.z,
				e.x + width.x, e.y + width.y, e.z + width.z, e.x - width.x, e.y - width.y, e.z - width.z, r, g, bl, a);
	}

	private static void quad(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float bl, float a) {
		b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
		b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
		b.addVertex(m, x3, y3, z3).setColor(r, g, bl, a);
		b.addVertex(m, x4, y4, z4).setColor(r, g, bl, a);
	}

	private WaypointRenderer() {
	}
}
