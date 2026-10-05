package sigf.mod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Incoming missiles: a red "!" warning and a siren, then a real 3D rocket streaks down the hall. Shoot it down with the jetpack gun. */
final class Missiles {
	static final List<Missile> all = new ArrayList<>();

	static final class Missile {
		Vec3 pos; double speed; int hp = 6; int warn; int dir = 1; final ServerPlayer target; final double y, z;
		final List<Display.BlockDisplay> parts = new ArrayList<>();
		final List<Vec3> offs = new ArrayList<>();
		Display.TextDisplay bang;
		boolean launched;
		Missile(ServerPlayer t, double y, double z, Vec3 pos) { target = t; this.y = y; this.z = z; this.pos = pos; }
	}

	private Missiles() {}

	static void incoming(ServerPlayer p, double ahead) {
		double y = Math.max(Lab.gy + 2.5, Math.min(Lab.gy + 11, p.getY() + 1.0));
		int dir = Lab.rng.nextBoolean() ? 1 : -1;
		double z0 = Lab.oz + 0.5 - dir * (Lab.HALF + 0.2);
		Missile m = new Missile(p, y, z0, new Vec3(p.getX() + 4, y, z0));
		m.dir = dir;
		m.warn = 44;
		Sigf.log("missile incoming at y=" + y);
		all.add(m);
	}

	private static void part(Missile m, net.minecraft.world.level.block.state.BlockState s, double f, float w, float h, float len, boolean glow) {
		Vec3 off = new Vec3(0, 0, f * m.dir);
		Display.BlockDisplay d = Gfx.box(m.pos.add(off), s, w, h, len, glow);
		d.setPosRotInterpolationDuration(2);
		m.parts.add(d);
		m.offs.add(off);
	}

	private static void launch(Missile m) {
		m.launched = true;
		m.speed = 0.5;
		Sigf.log("missile launched at " + m.pos);
		// body; the nose points along the flight direction (across the hall)
		part(m, Blocks.CONCRETE.white().defaultBlockState(), 0, 0.8f, 0.8f, 2.6f, false);
		part(m, Blocks.CONCRETE.red().defaultBlockState(), 1.5, 0.64f, 0.64f, 0.7f, false);
		part(m, Blocks.CONCRETE.red().defaultBlockState(), 0.35, 0.9f, 0.9f, 0.3f, false);
		part(m, Blocks.CONCRETE.red().defaultBlockState(), -0.9, 0.14f, 2.0f, 0.9f, false);
		part(m, Blocks.CONCRETE.red().defaultBlockState(), -0.9, 2.0f, 0.14f, 0.9f, false);
		part(m, Blocks.CONCRETE.orange().defaultBlockState(), -1.45, 0.6f, 0.6f, 0.5f, true);
		part(m, Blocks.CONCRETE.yellow().defaultBlockState(), -1.85, 0.4f, 0.4f, 0.5f, true);
		Sigf.sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, m.pos, 3f, 0.6f);
		Sigf.sound(SoundEvents.GHAST_SHOOT, m.pos, 1.5f, 0.6f);
	}

	static void explode(Missile m, boolean shotDown) {
		ServerLevel lv = Sigf.level();
		Vec3 p = m.pos;
		Sigf.log("missile exploded at " + p + " shot=" + shotDown);
		lv.explode(null, p.x, p.y, p.z, shotDown ? 1.5f : 3.0f, Level.ExplosionInteraction.NONE);
		lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		lv.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 60, 1.2, 1.2, 1.2, 0.15);
		for (Entity e : Sigf.near(p, 6, en -> en instanceof LivingEntity)) {
			Vec3 d = e.position().add(0, 0.8, 0).subtract(p);
			Vec3 push = d.normalize().scale(1.3 - Math.min(1, d.length() / 8)).add(0, 0.5, 0);
			e.setDeltaMovement(e.getDeltaMovement().add(push));
			e.syncVelocity = true;
			if (e instanceof LivingEntity le && !(e instanceof ServerPlayer)) { le.setInvulnerableTime(0); le.hurtServer(lv, lv.damageSources().explosion(null, null), 8f); }
			if (e instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("\u00a7c\u00a7lBOOM!"), true);
		}
		if (shotDown) Coins.burst(p, 8);
		for (var d : m.parts) d.discard();
		if (m.bang != null) m.bang.discard();
		m.parts.clear();
	}

	/** One jetpack bullet passing near the missile counts as a hit. */
	static void shoot(Vec3 a, Vec3 b) {
		for (Missile m : all) {
			if (!m.launched || m.hp <= 0) continue;
			Vec3 ab = b.subtract(a);
			double t = Math.max(0, Math.min(1, m.pos.subtract(a).dot(ab) / ab.lengthSqr()));
			if (a.add(ab.scale(t)).distanceTo(m.pos) < 1.4) {
				m.hp--;
				Sigf.level().sendParticles(ParticleTypes.CRIT, m.pos.x, m.pos.y, m.pos.z, 8, 0.4, 0.3, 0.3, 0.3);
				Sigf.sound(SoundEvents.ANVIL_LAND, m.pos, 0.3f, 2.0f);
				if (m.hp <= 0) m.speed = 0;
			}
		}
	}

	static void tick(long now) {
		ServerLevel lv = Sigf.level();
		for (var it = all.iterator(); it.hasNext(); ) {
			Missile m = it.next();
			if (!m.target.isAlive() || m.target.isRemoved()) { for (var d : m.parts) d.discard(); if (m.bang != null) m.bang.discard(); it.remove(); continue; }
			if (!m.launched) {
				// warning: a flashing red "!" on the wall the rocket will come out of, with a siren
				Vec3 bp = new Vec3(m.target.getX() + 8, m.y + 0.6, m.z + m.dir * 0.8);
				if (m.bang == null) { m.bang = Gfx.text(bp, "!", 7f, 0f); m.bang.setText(Component.literal("!").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD)); m.bang.setBillboardConstraints(Display.BillboardConstraints.CENTER); }
				m.bang.setPos(bp.x, bp.y, bp.z);
				if (m.warn % 8 == 0) Sigf.sound(SigfMod.WARNING_SOUND, m.target.position(), 0.7f, 1.0f);
				if (--m.warn <= 0) { m.bang.discard(); m.bang = null; m.pos = new Vec3(m.target.getX() + 5, m.y, m.z); launch(m); }
				continue;
			}
			if (m.hp <= 0) { explode(m, true); it.remove(); continue; }
			// steer along the hall toward where the player is heading, fly across it
			double aimX = m.target.getX() + m.target.getDeltaMovement().x * 6;
			double nx = m.pos.x + Math.max(-0.35, Math.min(0.35, aimX - m.pos.x));
			m.pos = new Vec3(nx, m.pos.y, m.pos.z + m.dir * m.speed);
			for (int i = 0; i < m.parts.size(); i++) {
				Vec3 q = m.pos.add(m.offs.get(i));
				m.parts.get(i).setPos(q.x, q.y, q.z);
			}
			Vec3 tail = m.pos.add(0, 0, -m.dir * 2.1);
			lv.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 0, 0, 0, -m.dir, 0.4);
			lv.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z, 3, 0.1, 0.1, 0.1, 0.05);
			lv.sendParticles(ParticleTypes.LARGE_SMOKE, tail.x, tail.y, tail.z - m.dir, 2, 0.15, 0.15, 0.15, 0.01);
			if (now % 6 == 0) Sigf.sound(SoundEvents.FIRE_AMBIENT, m.pos, 1.2f, 0.6f);
			boolean hit = false;
			for (Entity e : Sigf.near(m.pos, 1.7, en -> en instanceof LivingEntity)) { hit = true; break; }
			if (hit || Math.abs(m.pos.z - (Lab.oz + 0.5)) > Lab.HALF + 0.9 && Math.signum(m.pos.z - (Lab.oz + 0.5)) == m.dir) { explode(m, false); it.remove(); }
		}
	}
}
