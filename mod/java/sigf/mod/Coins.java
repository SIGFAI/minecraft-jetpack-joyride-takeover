package sigf.mod;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sigf.kit.Sigf;

/** Spinning gold coins hanging in the air: fly through them to collect. */
final class Coins {
	static final List<Coin> all = new ArrayList<>();
	static int total, streak;
	static long lastPickup;

	static final class Coin {
		final Display.ItemDisplay d; final Vec3 pos; Vec3 vel = Vec3.ZERO; long born;
		Coin(Display.ItemDisplay d, Vec3 pos) { this.d = d; this.pos = pos; }
	}

	private Coins() {}

	static Coin drop(Vec3 pos) {
		Display.ItemDisplay d = Gfx.item(pos, new ItemStack(SigfMod.COIN), 1.0f);
		d.setTransformationInterpolationDuration(10);
		Coin c = new Coin(d, pos);
		c.born = Sigf.server().overworld().getGameTime();
		all.add(c);
		return c;
	}

	/** A coin that bursts out of a defeated scientist. */
	static void burst(Vec3 at, int n) {
		var r = Sigf.level().getRandom();
		for (int i = 0; i < n; i++) {
			Coin c = drop(at.add(0, 1, 0));
			c.vel = new Vec3((r.nextDouble() - 0.5) * 0.5, 0.35 + r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.5);
		}
	}

	static void tick(long now) {
		boolean spin = now % 10 == 0;
		for (var it = all.iterator(); it.hasNext(); ) {
			Coin c = it.next();
			if (!c.d.isAlive()) { it.remove(); continue; }
			Vec3 p = c.d.position();
			if (c.vel.lengthSqr() > 1e-4) {
				p = p.add(c.vel);
				c.vel = c.vel.add(0, -0.04, 0).scale(0.94);
				if (c.vel.y < -0.3) c.vel = Vec3.ZERO;
				c.d.setPos(p.x, p.y, p.z);
				if (c.vel.y <= 0 && now - c.born > 14) c.vel = Vec3.ZERO;
			}
			if (spin) {
				float a = (float) ((now / 10) * Math.PI / 2);
				c.d.setTransformation(new Transformation(null, new Quaternionf().rotationY(a), new Vector3f(1.1f, 1.1f, 1.1f), null));
				c.d.setTransformationInterpolationDelay(0);
			}
			boolean taken = false;
			for (ServerPlayer pl : Sigf.players()) {
				if (pl.isSpectator()) continue;
				if (pl.position().add(0, 1, 0).distanceToSqr(p) < 2.6 * 2.6 && now - c.born > 6) { taken = true; break; }
			}
			if (taken) {
				c.d.discard();
				it.remove();
				total++;
				streak = now - lastPickup < 25 ? streak + 1 : 1;
				lastPickup = now;
				Sigf.sound(SigfMod.COIN_SOUND, p, 0.5f, 0.9f + Math.min(streak, 12) * 0.04f);
				Sigf.level().sendParticles(ParticleTypes.FIREWORK, p.x, p.y, p.z, 4, 0.2, 0.2, 0.2, 0.12);
			}
		}
	}

	/** Drop coins that are far behind every player. */
	static void sweep() {
		double minX = Double.MAX_VALUE;
		for (ServerPlayer p : Sigf.players()) minX = Math.min(minX, p.getX());
		if (minX == Double.MAX_VALUE) return;
		final double lim = minX - 70;
		all.removeIf(c -> { if (c.pos.x < lim) { c.d.discard(); return true; } return false; });
	}
}
