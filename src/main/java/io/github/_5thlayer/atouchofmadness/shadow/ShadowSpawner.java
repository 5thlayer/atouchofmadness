// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.shadow;

import java.util.Comparator;
import java.util.Optional;

import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Brings each player's Shadow Creatures. A player has a cap only while Haunted, which takes a dimension
 * type in {@link ShadowCreatures#SHADOWS}: then their Sanity sets it, and the Lurkers and Gaunts share
 * it. Every spawn interval, on a tick of the player's own, an attempt is made while they are under it;
 * once a second, the farthest of their creatures beyond it are removed. A creature appears between the
 * nearest and the farthest spawn distance, on a block it can stand on, out of the player's sight if any
 * such place is found; light doesn't matter. It takes its Shape from the biome of that place. The
 * attempts every interval stop while {@code doMobSpawning} is off, as other mobs' do.
 */
public final class ShadowSpawner {

    /** How many places an attempt tries before it gives up. */
    private static final int PLACES = 16;

    /** How far above and below the player's feet a place is looked for, in blocks. */
    private static final int SEARCH_HEIGHT = 8;

    /** How far from where the player looks a place is still in sight, in degrees. */
    private static final double SIGHT_DEGREES = 70;

    private ShadowSpawner() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, ShadowSpawner::tick);
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Each player on a tick of their own, chosen from their id, as their Sanity is (ADR 0005).
        int interval = ShadowCreatures.rules().intervalTicks();
        long tick = player.level().getServer().getTickCount() + Math.floorMod(player.getUUID().hashCode(), interval);
        if (tick % PlayerSanity.EVALUATION_TICKS == 0) {
            trim(player);
        }
        if (tick % interval == 0 && player.level().getGameRules().get(GameRules.SPAWN_MOBS)) {
            attempt(player);
        }
    }

    /** How many Shadow Creatures the player may have now. */
    public static int cap(ServerPlayer player) {
        return ShadowCreatures.rules().capFor(ShadowCreatures.factsOf(player));
    }

    /** Makes a spawn attempt for the player now, if they are under their cap, and returns what came of it. */
    public static Optional<ShadowCreature> attempt(ServerPlayer player) {
        if (ShadowCreatures.of(player).size() >= cap(player)) {
            return Optional.empty();
        }
        var rules = ShadowCreatures.rules();
        var random = player.getRandom();
        var type = rules.gauntsAt(PlayerSanity.get(player).value()) && random.nextDouble() < rules.gauntChance()
                ? ShadowCreatures.GAUNT.get() : ShadowCreatures.LURKER.get();
        return place(player, type, rules).map(at -> {
            ShadowCreature creature = type.create(player.level(), EntitySpawnReason.EVENT);
            creature.setOwner(player);
            creature.snapTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            creature.takeShape(player.level().getBiome(creature.blockPosition()));
            player.level().addFreshEntity(creature);
            return creature;
        });
    }

    /** Removes the farthest of the player's creatures beyond their cap. */
    public static void trim(ServerPlayer player) {
        var owned = ShadowCreatures.of(player);
        int excess = owned.size() - cap(player);
        owned.stream()
                .sorted(Comparator.comparingDouble(creature -> -creature.distanceToSqr(player)))
                .limit(Math.max(0, excess))
                .forEach(ShadowCreature::discard);
    }

    /** A place for a creature of this type around the player: the first found out of sight, or else the first found. */
    private static Optional<Vec3> place(ServerPlayer player, EntityType<?> type, ShadowRules rules) {
        var level = player.level();
        var random = player.getRandom();
        Vec3 inSight = null;
        for (int i = 0; i < PLACES; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = Mth.lerp(random.nextDouble(), rules.minDistance(), rules.maxDistance());
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            for (int dy = SEARCH_HEIGHT; dy >= -SEARCH_HEIGHT; dy--) {
                var pos = new BlockPos(x, player.getBlockY() + dy, z);
                if (standable(level, type, pos)) {
                    var at = Vec3.atBottomCenterOf(pos);
                    if (!inSight(player, at.add(0, type.getDimensions().eyeHeight(), 0))) {
                        return Optional.of(at);
                    }
                    if (inSight == null) {
                        inSight = at;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(inSight);
    }

    private static boolean standable(ServerLevel level, EntityType<?> type, BlockPos pos) {
        var below = pos.below();
        return level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** Whether the player could see this point: near enough where they look, with nothing in the way. */
    private static boolean inSight(ServerPlayer player, Vec3 point) {
        var eyes = player.getEyePosition();
        var toward = point.subtract(eyes).normalize();
        if (toward.dot(player.getLookAngle()) < Math.cos(Math.toRadians(SIGHT_DEGREES))) {
            return false;
        }
        var hit = player.level().clip(new ClipContext(eyes, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }
}
