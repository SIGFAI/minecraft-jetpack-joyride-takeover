package sigf.mod;

import java.util.Random;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** What goes into each hall segment: coin trails, zappers, scientists and jetpack guards. */
final class Course {
	private Course() {}

	static Vec3 c(double x, double y, double z) { return new Vec3(x, y, z); }

	static void fill(int seg, int xs) {
		Random r = new Random(seg * 7919L + 13);
		int gy = Lab.gy, oz = Lab.oz;
		// --- coin trails that follow the pilot's route
		for (int i = 0; i < 9; i++) {
			double x = xs + 2 + i * 2.2;
			Coins.drop(c(x, Lab.pathY(x) + 1.0, oz + 0.5 + (seg % 2 == 0 ? 0 : Math.sin(i * 0.9) * 1.5)));
		}
		switch (seg % 3) {
			case 0 -> { // ring of coins
				double x = xs + 16, y = Lab.pathY(x) + 1;
				for (int i = 0; i < 12; i++) {
					double a = i * Math.PI / 6;
					Coins.drop(c(x, y + Math.sin(a) * 2.6, oz + 0.5 + Math.cos(a) * 2.6));
				}
			}
			case 1 -> { // block of coins
				for (int ix = 0; ix < 4; ix++) for (int iy = 0; iy < 3; iy++) {
					double x = xs + 15 + ix * 1.5;
					Coins.drop(c(x, Lab.pathY(x) + 0.2 + iy * 1.3, oz + 0.5));
				}
			}
			default -> { // coins high up along the ceiling and one low
				for (int i = 0; i < 6; i++) Coins.drop(c(xs + 14 + i * 1.8, gy + 11.5, oz + 0.5 + (i % 2 == 0 ? -2.5 : 2.5)));
			}
		}
		// --- zappers
		if (seg >= 1) {
			if (seg == 1) {
				double x = xs + 13;
				Zappers.place(c(x + 0.5, Lab.pathY(x) - 3.5 + 0.5, oz + 0.5), c(x + 0.5, Lab.pathY(x) + 3.5 + 0.5, oz + 0.5));
			} else {
				int x = xs + 6 + r.nextInt(4);
				double py = Lab.pathY(x);
				double y = py + 4.5 <= gy + 11 ? py + 4.5 : py - 4.5;
				int yy = (int) Math.round(y);
				int tilt = r.nextBoolean() ? 2 : -2;
				if (yy + tilt > gy + 12 || yy + tilt < gy + 2) tilt = 0;
				Zappers.place(c(x + 0.5, yy + 0.5, oz - 4.5), c(x + 0.5, yy + tilt + 0.5, oz + 5.5));
			}
			int x2 = xs + 17 + r.nextInt(4);
			double py2 = Lab.pathY(x2);
			int side = r.nextBoolean() ? 1 : -1;
			int z = oz + side * (3 + r.nextInt(3));
			int y0 = py2 >= gy + 6 ? gy + 1 : gy + 12;
			int y1 = py2 >= gy + 6 ? (int) (py2 - 3.5) : (int) (py2 + 4.5);
			if (Math.abs(y1 - y0) >= 3) Zappers.place(c(x2 + 0.5, y0 + 0.5, z + 0.5), c(x2 + 0.5, y1 + 0.5, z + 0.5));
		}
		// --- scientists
		int sci = seg == 0 ? 6 : 2 + r.nextInt(2);
		for (int i = 0; i < sci; i++) {
			Mobs.scientist(c(xs + 4 + r.nextInt(SegW()), gy, oz + 0.5 + (r.nextDouble() - 0.5) * 9));
		}
		// --- jetpack guards
		if (seg >= 2 && seg % 3 == 2) {
			for (int i = 0; i < 1 + r.nextInt(2); i++) Mobs.guard(c(xs + 10 + r.nextInt(8), gy, oz + 0.5 + (r.nextDouble() - 0.5) * 8));
		}
	}

	private static int SegW() { return Lab.SEG - 8; }
}
