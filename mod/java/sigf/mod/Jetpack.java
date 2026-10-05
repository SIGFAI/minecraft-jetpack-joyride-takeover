package sigf.mod;

import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Server side of the machine-gun jetpack: flame jets, the downward gun, burning whatever is under the nozzles. */
final class Jetpack {
	private Jetpack() {}

	static boolean wearing(Player p) { return p.getItemBySlot(EquipmentSlot.CHEST).is(SigfMod.JETPACK); }

	static void tick(long now) {
		ServerLevel lv = Sigf.level();
		for (ServerPlayer p : Sigf.players()) {
			if (p.isSpectator() || !wearing(p)) continue;
			p.fallDistance = 0;
			boolean thrust = p.getLastClientInput().jump() && !p.onGround();
			if (!thrust) continue;
			Vec3 look = p.getLookAngle();
			Vec3 fwd = new Vec3(look.x, 0, look.z).normalize();
			Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
			Vec3 base = p.position().add(fwd.scale(-0.35)).add(0, 0.65, 0);
			for (int s = -1; s <= 1; s += 2) {
				Vec3 n = base.add(side.scale(0.2 * s));
				lv.sendParticles(ParticleTypes.FLAME, n.x, n.y, n.z, 0, 0, -1, 0, 0.45);
				lv.sendParticles(ParticleTypes.FLAME, n.x, n.y - 0.2, n.z, 0, 0, -1, 0, 0.3);
				lv.sendParticles(ParticleTypes.SMALL_FLAME, n.x, n.y, n.z, 0, 0, -1, 0, 0.2);
				if (now % 2 == 0) lv.sendParticles(ParticleTypes.SMOKE, n.x, n.y - 0.5, n.z, 1, 0.1, 0.1, 0.1, 0.02);
				if (now % 3 == 0) lv.sendParticles(ParticleTypes.LAVA, n.x, n.y - 0.6, n.z, 1, 0.15, 0.1, 0.15, 0);
			}
			if (now % 4 == 0) Sigf.sound(SoundEvents.FIRE_AMBIENT, p.position(), 1.0f, 0.6f);
			if (now % 14 == 0) Sigf.sound(SigfMod.GUN_SOUND, p.position(), 0.55f, 1.0f);
			if (now % 2 == 0) shoot(lv, p, fwd, side);
			if (now % 5 == 0) scorch(lv, p);
		}
	}

	/** One bullet, sprayed down and a little forward. */
	static void shoot(ServerLevel lv, ServerPlayer p, Vec3 fwd, Vec3 side) {
		var r = lv.getRandom();
		Vec3 from = p.position().add(fwd.scale(-0.2)).add(0, 0.6, 0);
		Vec3 dir = fwd.scale(0.45 + (r.nextDouble() - 0.5) * 0.25).add(side.scale((r.nextDouble() - 0.5) * 0.35)).add(0, -1, 0).normalize();
		Vec3 to = from.add(dir.scale(18));
		BlockHitResult bh = lv.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (bh.getType() == HitResult.Type.BLOCK) to = bh.getLocation();
		// entity in the line of fire?
		Entity hit = null; double hd = 1e9;
		List<Entity> list = lv.getEntities(p, new AABB(from, to).inflate(0.6), e -> e instanceof LivingEntity && e.isAlive() && !(e instanceof Player));
		for (Entity e : list) {
			var c = e.getBoundingBox().inflate(0.25).clip(from, to);
			if (c.isPresent() && from.distanceTo(c.get()) < hd) { hd = from.distanceTo(c.get()); hit = e; }
		}
		if (hit != null) to = from.add(dir.scale(hd));
		double len = from.distanceTo(to);
		for (double t = 1.5; t < len; t += 1.6) {
			Vec3 q = from.add(dir.scale(t));
			lv.sendParticles(new DustParticleOptions(0xFFD030, 0.7f), q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
		}
		lv.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 1, 0.05, 0.05, 0.05, 0.02);
		Missiles.shoot(from, to);
		if (hit instanceof LivingEntity le) {
			le.setInvulnerableTime(0);
			le.hurtServer(lv, lv.damageSources().playerAttack(p), 1.6f);
			le.setDeltaMovement(le.getDeltaMovement().add(dir.x * 0.08, 0.05, dir.z * 0.08));
			le.syncVelocity = true;
			Vec3 q = to;
			lv.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 4, 0.2, 0.2, 0.2, 0.3);
			lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 2, 0.1, 0.1, 0.1, 0.3);
		} else if (bh.getType() == HitResult.Type.BLOCK) {
			var st = lv.getBlockState(bh.getBlockPos());
			lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), to.x, to.y, to.z, 3, 0.15, 0.1, 0.15, 0.05);
			lv.sendParticles(ParticleTypes.SMOKE, to.x, to.y + 0.1, to.z, 1, 0.1, 0.0, 0.1, 0.01);
		}
	}

	/** The exhaust sets creatures under the pack on fire. */
	static void scorch(ServerLevel lv, ServerPlayer p) {
		AABB box = new AABB(p.getX() - 1.4, p.getY() - 5, p.getZ() - 1.4, p.getX() + 1.4, p.getY(), p.getZ() + 1.4);
		for (Entity e : lv.getEntities(p, box, en -> en instanceof LivingEntity && en.isAlive() && !(en instanceof Player))) {
			e.setRemainingFireTicks(80);
		}
	}
}
