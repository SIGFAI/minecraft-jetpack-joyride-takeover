package sigf.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sigf.kit.Sigf;

/** Small helpers that build 3D shapes out of display entities (real blocks and items, lit, scaled, rotated). */
final class Gfx {
	private Gfx() {}

	/** A glowing box of sx*sy*sz blocks centred on pos. */
	static Display.BlockDisplay box(Vec3 pos, BlockState st, float sx, float sy, float sz, boolean glow) {
		Display.BlockDisplay d = Sigf.spawn(EntityTypes.BLOCK_DISPLAY, pos);
		d.setBlockState(st);
		d.setTransformation(new Transformation(new Vector3f(-sx / 2, -sy / 2, -sz / 2), null, new Vector3f(sx, sy, sz), null));
		if (glow) d.setBrightnessOverride(Brightness.FULL_BRIGHT);
		d.setViewRange(4f);
		return d;
	}

	/** A thin box stretched from a to b (a beam), anchored at a. */
	static Display.BlockDisplay beam(Vec3 a, Vec3 b, BlockState st, float width) {
		Vec3 dir = b.subtract(a);
		float len = (float) dir.length();
		Quaternionf q = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize());
		Vector3f off = new Vector3f(-width / 2, 0, -width / 2).rotate(q);
		Display.BlockDisplay d = Sigf.spawn(EntityTypes.BLOCK_DISPLAY, a);
		d.setBlockState(st);
		d.setTransformation(new Transformation(off, q, new Vector3f(width, len, width), null));
		d.setBrightnessOverride(Brightness.FULL_BRIGHT);
		d.setViewRange(4f);
		return d;
	}

	static Display.ItemDisplay item(Vec3 pos, ItemStack stack, float scale) {
		Display.ItemDisplay d = Sigf.spawn(EntityTypes.ITEM_DISPLAY, pos);
		d.setItemStack(stack);
		d.setTransformation(new Transformation(null, null, new Vector3f(scale, scale, scale), null));
		d.setBrightnessOverride(Brightness.FULL_BRIGHT);
		d.setViewRange(4f);
		return d;
	}

	/** Floating text on a wall; yaw is the direction the text faces. */
	static Display.TextDisplay text(Vec3 pos, String s, float scale, float yaw) {
		Display.TextDisplay d = Sigf.spawn(EntityTypes.TEXT_DISPLAY, pos);
		d.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
		d.setText(Component.literal(s));
		d.setTransformation(new Transformation(null, null, new Vector3f(scale, scale, scale), null));
		d.setBrightnessOverride(Brightness.FULL_BRIGHT);
		d.setBackgroundColor(0);
		d.setViewRange(6f);
		return d;
	}
}
