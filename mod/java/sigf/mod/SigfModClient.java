package sigf.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Client side: the jetpack thrust (the client moves the player, so it feels smooth), the demo pilot and the camera. */
public final class SigfModClient implements ClientModInitializer {
	static boolean cameraSet;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.START_CLIENT_TICK.register(SigfModClient::tick);
	}

	private static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) { cameraSet = false; return; }
		if (p.isSpectator()) return;
		if (!cameraSet && Sigf.isDemo()) {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			cameraSet = true;
		}
		if (Sigf.isDemo() && SigfMod.pilot) pilot(mc, p);
		if (!p.getItemBySlot(EquipmentSlot.CHEST).is(SigfMod.JETPACK)) return;
		boolean jump = mc.options.keyJump.isDown();
		if (jump && !p.onGround() && !p.isInWater()) {
			Vec3 v = p.getDeltaMovement();
			double vy = Math.min(0.42, v.y + 0.085 + (v.y < 0 ? 0.06 : 0));
			double vx = v.x, vz = v.z;
			if (mc.options.keyUp.isDown()) {
				Vec3 look = p.getLookAngle();
				double fx = look.x, fz = look.z, fl = Math.sqrt(fx * fx + fz * fz);
				if (fl > 0.01) {
					vx += fx / fl * 0.03; vz += fz / fl * 0.03;
					double sp = Math.sqrt(vx * vx + vz * vz);
					if (sp > 0.34) { vx *= 0.34 / sp; vz *= 0.34 / sp; }
				}
			}
			p.setDeltaMovement(vx, vy, vz);
		}
	}

	/** Demo pilot: runs east through the hall, holding jump whenever the bot is below the route's altitude. */
	private static void pilot(Minecraft mc, LocalPlayer p) {
		double target = Lab.pathY(p.getX() + 4);
		// steer toward the nearest coin ahead, if any, so the bot visibly collects them
		double best = 1e9; Display.ItemDisplay near = null;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof Display.ItemDisplay d && d.getX() > p.getX() + 1 && d.getX() < p.getX() + 14) {
				double dd = d.getX() - p.getX();
				if (dd < best) { best = dd; near = d; }
			}
		}
		if (near != null) target = near.getY() - 0.9;
		double dz = (Lab.oz + 0.5) - p.getZ();
		p.setYRot(-90f + (float) Math.max(-30, Math.min(30, dz * 9)));
		p.setXRot(12f);
		boolean climb = p.getY() < target - 0.2 || (p.onGround());
		if (p.getDeltaMovement().y > 0.2 && p.getY() > target - 1.0) climb = false;
		mc.options.keyUp.setDown(true);
		mc.options.keyJump.setDown(climb);
	}
}
