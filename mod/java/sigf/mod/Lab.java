package sigf.mod;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** The endless laboratory hall the jetpack flies through: built segment by segment ahead of the player. */
final class Lab {
	static final int SEG = 24, HALF = 6, HEIGHT = 13;
	static int ox, oz, gy, x0;
	static int built = 0;
	static boolean ready;
	static final Random rng = new Random(42);

	private Lab() {}

	static void init(Vec3 center) {
		ServerLevel lv = Sigf.level();
		ox = (int) Math.floor(center.x);
		oz = (int) Math.floor(center.z);
		gy = lv.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(ox, 0, oz)).getY();
		x0 = ox + 7;
		built = 0;
		ready = true;
	}

	/** Altitude (feet) the demo pilot and the coin trails follow at a given x. */
	static double pathY(double x) {
		return gy + 7.2 + 2.6 * Math.sin((x - x0) * 0.11);
	}

	static void ensure(double playerX) {
		if (!ready) return;
		int guard = 0;
		while (x0 + built * SEG < Math.max(playerX, ox) + 100 && guard++ < 2) {
			build(built++);
		}
	}

	private static void set(ServerLevel lv, int x, int y, int z, BlockState s) {
		lv.setBlock(new BlockPos(x, y, z), s, 2);
	}

	private static boolean soft(BlockState s) {
		return s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS);
	}

	static void build(int seg) {
		ServerLevel lv = Sigf.level();
		int xs = x0 + seg * SEG;
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState white = Blocks.CONCRETE.white().defaultBlockState();
		BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
		BlockState yellow = Blocks.CONCRETE.yellow().defaultBlockState();
		BlockState black = Blocks.CONCRETE.black().defaultBlockState();
		BlockState gray = Blocks.CONCRETE.lightGray().defaultBlockState();
		BlockState dark = Blocks.CONCRETE.gray().defaultBlockState();
		BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.lightBlue().defaultBlockState();
		BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
		for (int dx = 0; dx < SEG; dx++) {
			int x = xs + dx;
			lv.getChunk(new BlockPos(x, gy, oz));
			for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
				int z = oz + dz;
				boolean wall = Math.abs(dz) == HALF + 1;
				// floor with hazard edges and a checker
				BlockState floor = Math.abs(dz) == HALF ? (((x >> 1) & 1) == 0 ? yellow : black) : (((x + dz) & 1) == 0 ? gray : white);
				set(lv, x, gy - 1, z, wall ? dark : floor);
				for (int y = gy - 2; y > gy - 24; y--) {
					BlockPos p = new BlockPos(x, y, z);
					if (!soft(lv.getBlockState(p))) break;
					set(lv, x, y, z, y == gy - 2 ? dark : Blocks.STONE_BRICKS.defaultBlockState());
				}
				for (int y = gy; y <= gy + HEIGHT; y++) {
					BlockState s = air;
					if (wall) {
						s = white;
						int h = y - gy;
						if (h <= 1) s = (((x >> 1) & 1) == 0) ? yellow : black;
						else if (h >= 5 && h <= 9 && (dx % 8) >= 2 && (dx % 8) <= 5) s = glass;
						else if (h == 4 || h == 10) s = quartz;
						if (dx % 8 == 0) s = iron;
					}
					set(lv, x, y, z, s);
				}
				// ceiling: dark with light strips and skylights
				BlockState roof = dark;
				if (!wall && Math.abs(dz) % 4 == 2 && dx % 4 == 0) roof = lamp;
				else if (!wall && dz == 0 && dx % 8 >= 2 && dx % 8 <= 5) roof = glass;
				set(lv, x, gy + HEIGHT + 1, z, roof);
			}
		}
		// signs: distance on both walls, facing into the hall
		int mid = xs + SEG / 2;
		int meters = mid - x0;
		Gfx.text(new Vec3(mid + 0.5, gy + 7.5, oz - HALF - 0.4), meters + " M", 4f, 0f);
		Gfx.text(new Vec3(mid + 0.5, gy + 7.5, oz + HALF + 0.4), meters + " M", 4f, 180f);
		if (seg == 0) {
			Gfx.text(new Vec3(xs + 3.5, gy + 11, oz + 0.5), "JETPACK JOYRIDE LAB", 5f, 90f);
			Gfx.text(new Vec3(xs + 3.5, gy + 9, oz + 0.5), "Hold JUMP to blast off!", 3f, 90f);
		}
		Course.fill(seg, xs);
	}
}
