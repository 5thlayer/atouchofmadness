// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Module's game tests, run by the {@code gameTestServer} Gradle run: the Minecraft-integration
 * seam, with a real player on a real server. Each test stands on the {@code gametest/platform}
 * structure, a stone floor that {@code scripts/build-gametest-structures.py} writes, and sets up
 * what it needs itself, so the setup is in the diff. It lives in the {@code gametest} source set, which
 * only the Module's own runs load, and hooks itself in, so the {@code @Mod} class names no game-test
 * code and nothing gates it (libworks ADR 0011).
 */
@EventBusSubscriber(modid = ATouchOfMadness.MOD_ID)
public final class ATouchOfMadnessGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, ATouchOfMadness.MOD_ID);

    private static final DeferredRegister<MapCodec<? extends TestEnvironmentDefinition<?>>> ENVIRONMENT_TYPES =
            DeferredRegister.create(Registries.TEST_ENVIRONMENT_DEFINITION_TYPE, ATouchOfMadness.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
        ENVIRONMENT_TYPES.register("sky", () -> Sky.CODEC);
    }

    private ATouchOfMadnessGameTests() {
    }

    @SubscribeEvent
    static void construct(FMLConstructModEvent event) {
        register(event.getContainer().getEventBus());
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        ENVIRONMENT_TYPES.register(modBus);
        SanityTests.listen(modBus);
        // Posted only when game tests are enabled, which a production game never is.
        modBus.addListener(ATouchOfMadnessGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        // Noon and clear, so no drain of where a player stands applies on the platform's open floor.
        var tests = Registrar.of(event, "default", new Sky(Sky.NOON, TestEnvironmentDefinition.Weather.Type.CLEAR));
        LoadTests.register(tests);
        GameTestPackTests.register(tests);
        CreativeTabTests.register(tests);
        SanityTests.register(tests);
        EnvironmentTests.register(tests);
        MomentsTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfMadness.MOD_ID, path);
    }

    /**
     * What a test class is handed: a name, a tick budget and a body per test, run in the default
     * environment or one of its own. Tests in different environments run in different batches. A test
     * is roofed with barriers, which let light through but not rain, unless it has sky access.
     */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, boolean skyAccess) {

        /** A Registrar whose tests run in this environment, registered under this name, with no sky access. */
        static Registrar of(RegisterGameTestsEvent event, String name, TestEnvironmentDefinition<?> definition) {
            return new Registrar(event, null, false).under(name, definition);
        }

        /** The tests registered through the result run in this environment, registered under this name. */
        Registrar under(String name, TestEnvironmentDefinition<?> definition) {
            // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
            return new Registrar(event, event.registerEnvironment(id(name), definition), skyAccess);
        }

        /** The tests registered through the result have no barrier roof, so rain reaches them. */
        Registrar withSkyAccess() {
            return new Registrar(event, environment, true);
        }

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(environment, PLATFORM, maxTicks, 0, true, Rotation.NONE,
                    false, 1, 1, skyAccess, 0)));
        }
    }
}
