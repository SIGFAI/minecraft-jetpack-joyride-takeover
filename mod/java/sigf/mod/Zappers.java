package sigf.mod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Electric zappers: two hazard-striped nodes and a crackling beam that shocks and flings whatever touches it. */
final class Zappers {
	static final List<Zapper> all = new ArrayList<>();

	static final class Zapper {
		final Vec3 a, b;
		Zapper(Vec3 a, Vec3 b) { this.a = a; this.b = b; }
	}

	private Zappers() {}

	static void place(Vec3 a, Vec3 b) {
		ServerLevel lv = Sigf.level();
		// solid hazard nodes: a small black-and-yellow cluster at each end
		for (Vec3 n : new Vec3[] { a, b }) {
			BlockPos c = BlockPos.containing(n);
			lv.setBlock(c, Blocks.GLOWSTONE.defaultBlockState(), 2);
			lv.setBlock(c.above(), Blocks.CONCRETE.black().defaultBlockState(), 2);
			lv.setBlock(c.below(), Blocks.CONCRETE.black().defaultBlockState(), 2);
			lv.setBlock(c.north(), Blocks.CONCRETE.yellow().defaultBlockState(), 2);
			lv.setBlock(c.south(), Blocks.CONCRETE.yellow().defaultBlockState(), 2);
		}
		Gfx.beam(a, b, Blocks.CONCRETE.lightBlue().defaultBlockState(), 0.32f);
		Gfx.beam(a, b, Blocks.CONCRETE.white().defaultBlockState(), 0.14f);
		all.add(new Zapper(a, b));
	}

	static double distToSeg(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / ab.lengthSqr()));
		return p.distanceTo(a.add(ab.scale(t)));
	}

	static void tick(long now) {
		ServerLevel lv = Sigf.level();
		var near = new ArrayList<ServerPlayer>();
		for (ServerPlayer p : Sigf.players()) near.add(p);
		for (Zapper z : all) {
			boolean close = false;
			for (ServerPlayer p : near) if (Math.abs(p.getX() - z.a.x) < 48) close = true;
			if (!close) continue;
			var r = lv.getRandom();
			// crackle along the beam
			for (int i = 0; i < 3; i++) {
				double t = r.nextDouble();
				Vec3 q = z.a.add(z.b.subtract(z.a).scale(t));
				lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0.25, 0.25, 0.25, 0.2);
			}
			if (now % 20 == 0) Sigf.sound(SigfMod.ZAP_SOUND, z.a.add(z.b).scale(0.5), 0.25f, 1.0f);
			if (now % 2 != 0) continue;
			AABB box = new AABB(z.a, z.b).inflate(1.2);
			for (Entity e : lv.getEntities((Entity) null, box, en -> en instanceof LivingEntity && en.isAlive())) {
				LivingEntity le = (LivingEntity) e;
				if (le instanceof ServerPlayer sp && sp.isSpectator()) continue;
				if (distToSeg(le.position().add(0, le.getBbHeight() / 2, 0), z.a, z.b) > 0.7 + le.getBbWidth() / 2) continue;
				if (now - lastZap(le) < 14) continue;
				zap(le, z, now);
			}
		}
	}

	private static long lastZap(LivingEntity e) {
		for (String t : e.entityTags()) if (t.startsWith("z")) { try { return Long.parseLong(t.substring(1)); } catch (Exception ignored) {} }
		return -100;
	}

	static void zap(LivingEntity le, Zapper z, long now) {
		ServerLevel lv = Sigf.level();
		le.entityTags().removeIf(t -> t.startsWith("z"));
		le.addTag("zapped");
		le.addTag("z" + now);
		Vec3 c = le.position().add(0, le.getBbHeight() / 2, 0);
		Vec3 push = c.subtract(z.a.add(z.b).scale(0.5));
		Vec3 dir = new Vec3(-0.6, 0.3, push.z * 0.4 + 0.1).normalize();
		le.setInvulnerableTime(0);
		le.hurtServer(lv, lv.damageSources().lightningBolt(), 3f);
		le.setDeltaMovement(dir.scale(0.9));
		le.syncVelocity = true;
		le.setRemainingFireTicks(0);
		lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 40, 0.5, 0.7, 0.5, 0.5);
		Sigf.sound(SigfMod.ZAP_SOUND, c, 1.0f, 1.1f);
		Sigf.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, c, 0.5f, 1.6f);
		if (le instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("\u00a7e\u00a7lBZZZZT!"), true);
	}
}
