package sigf.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;
import sigf.kit.Sigf;

/** Jetpack Joyride Takeover: a machine-gun jetpack, a lab that never ends, coins, zappers, missiles and screaming scientists. */
public final class SigfMod implements ModInitializer {
	public static Item JETPACK, COIN;
	public static SoundEvent COIN_SOUND, ZAP_SOUND, WARNING_SOUND, GUN_SOUND, SCREAM_SOUND;
	/** Demo only: the stream bot flies the hall by itself. */
	public static volatile boolean pilot;
	static double startX;
	static double bestDist;
	static double nextMissileX;
	static boolean started;

	@Override
	public void onInitialize() {
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Sigf.id("jetpack"));
		JETPACK = Sigf.item("jetpack", p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE,
			Equippable.builder(EquipmentSlot.CHEST).setAsset(asset).build())));
		COIN = Sigf.item("coin", p -> new Item(p));
		COIN_SOUND = Sigf.registerSound("coin");
		ZAP_SOUND = Sigf.registerSound("zap");
		WARNING_SOUND = Sigf.registerSound("warning");
		GUN_SOUND = Sigf.registerSound("gunburst");
		SCREAM_SOUND = Sigf.registerSound("scream");

		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> Mobs.onDeath(e));
		ServerPlayConnectionEvents.JOIN.register((h, s, server) -> server.execute(() -> join(h.getPlayer())));

		// --- the demo: the bot straps on the jetpack and flies the hall
		Sigf.demo(0.5, () -> {
			ServerPlayer p = Sigf.host();
			Sigf.teleport(p, new Vec3(Lab.x0 + 5.5, Lab.gy, Lab.oz + 0.5), Vec3.ZERO);
			p.connection.teleport(Lab.x0 + 5.5, Lab.gy, Lab.oz + 0.5, -90f, 12f);
			startX = p.getX();
			Sigf.title("JETPACK JOYRIDE", "TAKEOVER", 3);
		});
		Sigf.demo(4.5, () -> { pilot = true; Sigf.title("", "Collect coins! Dodge zappers!", 3); });
	}

	static void join(ServerPlayer p) {
		Lab.init(sigf.kit.SigfKit.center());
		Lab.ensure(Lab.ox);
		p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(JETPACK));
		nextMissileX = Lab.x0 + 70;
		started = true;
	}

	static void tick() {
		if (!started || Sigf.host() == null) return;
		long now = Sigf.server().overworld().getGameTime();
		double maxX = Lab.ox;
		for (ServerPlayer p : Sigf.players()) if (!p.isSpectator()) maxX = Math.max(maxX, p.getX());
		Lab.ensure(maxX);
		Jetpack.tick(now);
		Coins.tick(now);
		Zappers.tick(now);
		Missiles.tick(now);
		Mobs.tick(now);
		if (now % 40 == 0) Coins.sweep();
		ServerPlayer h = Sigf.host();
		if (!h.isSpectator()) {
			if (h.getX() > nextMissileX) {
				Missiles.incoming(h, 50);
				nextMissileX += 85;
			}
			if (startX == 0) startX = Lab.x0 + 5;
			bestDist = Math.max(bestDist, h.getX() - startX);
			if (now % 4 == 0) {
				for (ServerPlayer p : Sigf.players()) {
					p.sendSystemMessage(Component.literal("\u00a76COINS \u00a7e" + Coins.total + "   \u00a7bDISTANCE \u00a7f" + (int) Math.max(0, bestDist) + " m"), true);
				}
			}
		}
	}
}
