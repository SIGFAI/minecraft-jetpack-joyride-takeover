package sigf.mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Panicking lab scientists (villagers in white coats) and flying jetpack guards (zombies). */
final class Mobs {
	private Mobs() {}

	static void scientist(Vec3 pos) {
		Sigf.command(String.format(java.util.Locale.ROOT,
			"summon villager %.2f %.2f %.2f {CustomName:\"Scientist\",CustomNameVisible:1b,Tags:[\"sci\"],PersistenceRequired:1b,VillagerData:{profession:\"minecraft:librarian\",type:\"minecraft:plains\",level:2}}",
			pos.x, pos.y, pos.z));
	}

	static void guard(Vec3 pos) {
		Zombie z = Sigf.spawn(EntityTypes.ZOMBIE, pos);
		if (z == null) return;
		z.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SigfMod.JETPACK));
		z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		z.setCustomName(Component.literal("Jetpack Guard"));
		z.setCustomNameVisible(true);
		z.setPersistenceRequired();
		z.addTag("guard");
	}

	static void onDeath(LivingEntity e) {
		if (e.entityTags().contains("sci")) {
			Coins.burst(e.position(), 6);
			Sigf.sound(SigfMod.SCREAM_SOUND, e.position(), 0.8f, 1.3f);
		} else if (e.entityTags().contains("guard")) {
			Coins.burst(e.position(), 10);
		}
	}

	static void tick(long now) {
		ServerLevel lv = Sigf.level();
		for (Entity e : lv.getAllEntities()) {
			if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
			boolean sci = e.entityTags().contains("sci"), guard = e.entityTags().contains("guard");
			if (!sci && !guard) continue;
			ServerPlayer pl = null; double best = 1e9;
			for (ServerPlayer p : Sigf.players()) {
				if (p.isSpectator()) continue;
				double d = p.distanceToSqr(e);
				if (d < best) { best = d; pl = p; }
			}
			if (pl == null) continue;
			Vec3 away = e.position().subtract(pl.position());
			Vec3 flat = new Vec3(away.x, 0, away.z);
			if (sci && best < 14 * 14 && flat.lengthSqr() > 0.01) {
				Vec3 d = flat.normalize();
				// run away from the jetpack, but never into the hall walls
				double nz = e.getZ() + d.z * 0.5;
				if (Math.abs(nz - (Lab.oz + 0.5)) > Lab.HALF - 0.8) d = new Vec3(d.x, 0, -Math.signum(d.z));
				Vec3 v = e.getDeltaMovement();
				if (e.onGround()) {
					e.setDeltaMovement(d.x * 0.22, v.y, d.z * 0.22);
					e.setYRot((float) (Math.toDegrees(Math.atan2(-d.x, d.z))));
					e.syncVelocity = true;
				}
				if (now % 24 == 0 && lv.getRandom().nextInt(3) == 0) {
					Sigf.sound(SigfMod.SCREAM_SOUND, e.position(), 0.6f, 1.0f + lv.getRandom().nextFloat() * 0.5f);
					lv.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 2.1, e.getZ(), 6, 0.2, 0.1, 0.2, 0.1);
				}
			}
			if (guard && best < 40 * 40) {
				Vec3 to = pl.position().add(0, 0.5, 0).subtract(e.position());
				Vec3 v = e.getDeltaMovement();
				double ax = 0, az = 0, ay = 0;
				Vec3 h = new Vec3(to.x, 0, to.z);
				if (h.lengthSqr() > 4) { h = h.normalize(); ax = h.x * 0.035; az = h.z * 0.035; }
				boolean up = to.y > 0.5;
				if (up) ay = 0.11;
				double vx = Math.max(-0.3, Math.min(0.3, v.x + ax)), vz = Math.max(-0.3, Math.min(0.3, v.z + az));
				double vy = up ? Math.min(0.32, v.y + ay) : v.y;
				e.setDeltaMovement(vx, vy, vz);
				le.fallDistance = 0;
				if (up) {
					Vec3 b = e.position().add(0, 0.7, 0);
					lv.sendParticles(ParticleTypes.FLAME, b.x, b.y, b.z, 0, 0, -1, 0, 0.25);
					lv.sendParticles(ParticleTypes.FLAME, b.x, b.y, b.z, 0, 0, -1, 0, 0.35);
					if (now % 2 == 0) lv.sendParticles(ParticleTypes.SMOKE, b.x, b.y - 0.2, b.z, 1, 0.1, 0.1, 0.1, 0.02);
				}
				if (le instanceof Zombie z && z.getTarget() == null) z.setTarget(pl);
			}
		}
	}
}
