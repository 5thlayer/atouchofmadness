// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github._5thlayer.atouchofmadness.gametest.ShadowTests.Kind;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreature;
import io.github._5thlayer.atouchofmadness.shadow.ShadowCreatures;
import io.github._5thlayer.atouchofmadness.shadow.Shapes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The Shapes (#11). A Shadow Creature takes the Shape of one of the monsters its biome spawns that is in
 * its kind's tag, {@code #atouchofmadness:lurker_shapes} or {@code gaunt_shapes}: a husk or a spider in
 * a desert, a piglin or an enderman in the Nether's wastes; a zombie or a phantom where none is, as in
 * mushroom fields. A pack can change either tag. The Shape changes none of its stats.
 *
 * <p>The pick is random among the biome's Shapes, so each test asserts on the Shapes a biome offers,
 * and that a creature given that biome takes one of them, never on which one it takes.
 */
final class ShapeTests {

    /** How many times a creature takes a Shape from a biome, each of which must be one the biome offers. */
    private static final int TAKES = 20;

    /** How many seeds a test tries for a pick it needs to differ from the fallback. */
    private static final int SEEDS = 100;

    private ShapeTests() {
    }

    static void register(ATouchOfMadnessGameTests.Registrar defaults) {
        defaults.test("shadow_shape_a_lurker_in_a_desert_takes_a_desert_monsters_shape", 1,
                helper -> takes(helper, ShadowTests.LURKER, Biomes.DESERT, EntityType.HUSK));
        defaults.test("shadow_shape_a_gaunt_in_a_desert_takes_a_desert_monsters_shape", 1,
                helper -> takes(helper, ShadowTests.GAUNT, Biomes.DESERT, EntityType.SPIDER));
        defaults.test("shadow_shape_a_lurker_in_the_nether_takes_a_nether_monsters_shape", 1,
                helper -> takes(helper, ShadowTests.LURKER, Biomes.NETHER_WASTES, EntityType.PIGLIN));
        defaults.test("shadow_shape_a_gaunt_in_the_nether_takes_a_nether_monsters_shape", 1,
                helper -> takes(helper, ShadowTests.GAUNT, Biomes.NETHER_WASTES, EntityType.ENDERMAN));
        defaults.test("shadow_shape_a_lurker_in_mushroom_fields_falls_back_to_a_zombie", 1,
                helper -> fallsBack(helper, ShadowTests.LURKER, Biomes.MUSHROOM_FIELDS));
        defaults.test("shadow_shape_a_gaunt_in_mushroom_fields_falls_back_to_a_phantom", 1,
                helper -> fallsBack(helper, ShadowTests.GAUNT, Biomes.MUSHROOM_FIELDS));
        defaults.padded(ShadowTests.SPAWN_PADDING).test("shadow_shape_a_spawned_lurker_takes_a_shape_of_the_biome_it_appears_in", 1,
                ShapeTests::spawned);
        for (var kind : List.of(ShadowTests.LURKER, ShadowTests.GAUNT)) {
            defaults.test("shadow_shape_a_" + kind.name() + "s_shape_changes_none_of_its_stats", 1, helper -> noStat(helper, kind));
        }
        // The pack gives the Lurker the spider alone, and the Gaunt no Shape, for every test in its batch.
        defaults.under("shadow_shape_pack", new TestEnvironmentDefinition.AllOf(new Sky(Sky.NOON, Weather.Type.CLEAR), new WithTestPack("shapes")))
                .test("shadow_shape_a_pack_adds_and_removes_shapes", 1, ShapeTests::pack);
    }

    /** The biome offers {@code expected} among its kind's Shapes, offers only its own monsters in the tag, and a creature takes one of them. */
    private static void takes(GameTestHelper helper, Kind kind, ResourceKey<Biome> key, EntityType<?> expected) {
        var biome = biome(helper, key);
        var tag = kind.shapes();
        var offered = offered(biome, tag);
        if (!offered.contains(expected)) {
            helper.fail(key.identifier() + " offers a " + kind.name() + " " + names(offered) + ", not " + EntityType.getKey(expected));
        }
        var spawns = biome.value().getMobSettings().getMobs(MobCategory.MONSTER);
        var monsters = spawns.unwrap().stream().map(spawn -> spawn.value().type()).collect(Collectors.toSet());
        for (var shape : offered) {
            if (!monsters.contains(shape) || !shape.builtInRegistryHolder().is(tag)) {
                helper.fail(key.identifier() + " offers " + EntityType.getKey(shape) + ", which is not one of its monsters in " + tag.location());
            }
        }
        var creature = creature(helper, kind);
        for (int i = 0; i < TAKES; i++) {
            creature.takeShape(biome);
            if (!offered.contains(creature.shape())) {
                helper.fail("a " + kind.name() + " in " + key.identifier() + " took " + EntityType.getKey(creature.shape()) + ", not one of " + names(offered));
            }
        }
        helper.succeed();
    }

    private static void fallsBack(GameTestHelper helper, Kind kind, ResourceKey<Biome> key) {
        var fallback = kind.fallback();
        var creature = creature(helper, kind);
        if (creature.shape() != fallback) {
            helper.fail("a " + kind.name() + " is drawn as " + EntityType.getKey(creature.shape()) + " before it takes a Shape, not its fallback");
        }
        creature.takeShape(biome(helper, key));
        if (creature.shape() != fallback) {
            helper.fail("a " + kind.name() + " in " + key.identifier() + " took " + EntityType.getKey(creature.shape()) + ", not "
                    + EntityType.getKey(fallback));
        }
        helper.succeed();
    }

    private static void spawned(GameTestHelper helper) {
        ShadowTests.ground(helper);
        var player = ShadowTests.join(helper, ShadowTests.MIDDLE, ShadowTests.UNEASY, GameType.SURVIVAL);
        ShadowTests.attempts(player, 1);
        var creatures = ShadowCreatures.of(player);
        if (creatures.size() != 1) {
            helper.fail("an Uneasy player has " + creatures.size() + " Shadow Creatures after an attempt, not 1");
        }
        var creature = creatures.getFirst();
        var biome = helper.getLevel().getBiome(creature.blockPosition());
        var offered = offered(biome, ShadowTests.LURKER.shapes());
        if (offered.isEmpty() ? creature.shape() != ShadowTests.LURKER.fallback() : !offered.contains(creature.shape())) {
            helper.fail("a Lurker that appeared in " + biome.getRegisteredName() + " took " + EntityType.getKey(creature.shape())
                    + ", not one of " + names(offered));
        }
        helper.succeed();
    }

    /**
     * A creature that took a Shape has the same health, damage, speed and size as one of its kind that took none.
     * A desert offers the fallback among its Shapes, so the pick is seeded, by the first of {@link #SEEDS} that
     * draws another Shape: the same seed, and so the same Shape, every run.
     */
    private static void noStat(GameTestHelper helper, Kind kind) {
        var plain = creature(helper, kind);
        var shaped = creature(helper, kind);
        var desert = biome(helper, Biomes.DESERT);
        for (int seed = 0; seed < SEEDS && shaped.shape() == plain.shape(); seed++) {
            shaped.getRandom().setSeed(seed);
            shaped.takeShape(desert);
        }
        if (shaped.shape() == plain.shape()) {
            helper.fail("a " + kind.name() + " in a desert kept its fallback Shape for every seed, so this test compares nothing");
        }
        for (var attribute : List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.FOLLOW_RANGE)) {
            if (shaped.getAttributeValue(attribute) != plain.getAttributeValue(attribute)) {
                helper.fail("a " + kind.name() + " shaped as " + EntityType.getKey(shaped.shape()) + " has " + attribute.getRegisteredName()
                        + " " + shaped.getAttributeValue(attribute) + ", not " + plain.getAttributeValue(attribute));
            }
        }
        if (!shaped.getDimensions(shaped.getPose()).equals(plain.getDimensions(plain.getPose())) || shaped.getHealth() != plain.getHealth()) {
            helper.fail("a " + kind.name() + " shaped as " + EntityType.getKey(shaped.shape()) + " has another size or health");
        }
        helper.succeed();
    }

    private static void pack(GameTestHelper helper) {
        var desert = biome(helper, Biomes.DESERT);
        var lurkers = offered(desert, Shapes.LURKER_SHAPES);
        if (!lurkers.equals(Set.of(EntityType.SPIDER))) {
            helper.fail("with the pack, a desert offers a Lurker " + names(lurkers) + ", not the spider alone");
        }
        var lurker = creature(helper, ShadowTests.LURKER);
        lurker.takeShape(desert);
        if (lurker.shape() != EntityType.SPIDER) {
            helper.fail("with the pack, a Lurker in a desert took " + EntityType.getKey(lurker.shape()) + ", not the spider it adds");
        }
        var gaunt = creature(helper, ShadowTests.GAUNT);
        gaunt.takeShape(desert);
        if (gaunt.shape() != ShadowTests.GAUNT.fallback()) {
            helper.fail("with the pack, a Gaunt in a desert took " + EntityType.getKey(gaunt.shape()) + ", not its fallback, as the pack leaves it none");
        }
        helper.succeed();
    }

    private static ShadowCreature creature(GameTestHelper helper, Kind kind) {
        return kind.type().get().create(helper.getLevel(), EntitySpawnReason.EVENT);
    }

    private static Holder<Biome> biome(GameTestHelper helper, ResourceKey<Biome> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    private static Set<EntityType<?>> offered(Holder<Biome> biome, TagKey<EntityType<?>> tag) {
        return Shapes.in(biome, tag).unwrap().stream().map(Weighted::value).collect(Collectors.toSet());
    }

    private static String names(Set<EntityType<?>> types) {
        return types.stream().map(type -> EntityType.getKey(type).toString()).sorted().toList().toString();
    }
}
