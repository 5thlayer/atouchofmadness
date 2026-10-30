// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import io.github._5thlayer.atouchofmadness.crown.CrownRules;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.moments.MomentRules;
import io.github._5thlayer.atouchofmadness.music.MusicRules;
import io.github._5thlayer.atouchofmadness.nearby.NearbyRules;
import io.github._5thlayer.atouchofmadness.sanity.Sanity;
import io.github._5thlayer.atouchofmadness.sanity.SanityRules;
import io.github._5thlayer.atouchofmadness.shadow.ShadowRules;
import io.github._5thlayer.atouchofmadness.umbra.UmbraRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The common config, {@code atouchofmadness-common.toml}: where Sanity starts and where its stages
 * begin, the drains of where a player stands, the drain and restore of who is near, the restore of
 * music, the restores of sleep and flowers and the drain of a Strike by the Dark, the Flower Crown, the
 * Shadow Creatures, the drains of Umbra Gear, and whether discovery hides what each does. Each feature
 * holds its rules {@link #live}, and they are read anew whenever the config loads or reloads.
 */
public final class MadnessConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("A player's Sanity, from 0 to 100.").push("sanity");
    }

    private static final ModConfigSpec.DoubleValue START = BUILDER
            .comment("The Sanity a new player starts with.")
            .defineInRange("start", SanityRules.DEFAULT.start().value(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue RESPAWN = BUILDER
            .comment("The Sanity a player respawns with after dying.")
            .defineInRange("respawn", SanityRules.DEFAULT.respawn().value(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue SANE_ABOVE = BUILDER
            .comment("A player is Sane above this Sanity, and Uneasy from it down to insaneBelow.")
            .defineInRange("saneAbove", SanityRules.DEFAULT.saneAbove(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue INSANE_BELOW = BUILDER
            .comment("A player is Insane below this Sanity. Set above saneAbove, it is read as saneAbove.")
            .defineInRange("insaneBelow", SanityRules.DEFAULT.insaneBelow(), Sanity.MIN, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The drains of where a player stands: each in Sanity a minute, and the Low-light threshold.").push("environment");
    }

    private static final ModConfigSpec.IntValue LOW_LIGHT_BELOW = BUILDER
            .comment("Light at a player's eyes below this is Low light, unless a full moon is in the sky they can see.")
            .defineInRange("lowLightBelow", EnvironmentRules.DEFAULT.lowLightBelow(), 0, 16);

    private static final ModConfigSpec.DoubleValue LOW_LIGHT = BUILDER
            .comment("The drain while a player is in Low light.")
            .defineInRange("lowLight", EnvironmentRules.DEFAULT.lowLight(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue FREEZING = BUILDER
            .comment("The drain while a player is freezing, in powder snow or from any other cold.")
            .defineInRange("freezing", EnvironmentRules.DEFAULT.freezing(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue WET = BUILDER
            .comment("The drain while a player is in water or rain.")
            .defineInRange("wet", EnvironmentRules.DEFAULT.wet(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue UNSETTLING = BUILDER
            .comment("The drain while a player is in a dimension type in #atouchofmadness:unsettling, the Nether by default.")
            .defineInRange("unsettling", EnvironmentRules.DEFAULT.unsettling(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The drain and restore of who is near a player: rates in Sanity a minute, radii in blocks.").push("nearby");
    }

    private static final ModConfigSpec.DoubleValue HOSTILE_RADIUS = BUILDER
            .comment("How near an entity in #atouchofmadness:unsettling must be to drain.")
            .defineInRange("hostileRadius", NearbyRules.DEFAULT.hostileRadius(), 0, 64);

    private static final ModConfigSpec.DoubleValue HOSTILE = BUILDER
            .comment("The drain of each such entity, times its atouchofmadness:unsettling_weight data map entry, 1 if it has none.")
            .defineInRange("hostile", NearbyRules.DEFAULT.hostile(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue HOSTILE_CAP = BUILDER
            .comment("The most all such entities together drain.")
            .defineInRange("hostileCap", NearbyRules.DEFAULT.hostileCap(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue COMPANY_RADIUS = BUILDER
            .comment("How near company must be to restore.")
            .defineInRange("companyRadius", NearbyRules.DEFAULT.companyRadius(), 0, 64);

    private static final ModConfigSpec.DoubleValue COMPANY = BUILDER
            .comment("The restore while any entity in #atouchofmadness:comforting, tamed if it can be, or any other Survival or Adventure player is near; once however many.")
            .defineInRange("company", NearbyRules.DEFAULT.company(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The restore of music: the rate in Sanity a minute, the range in blocks.").push("music");
    }

    private static final ModConfigSpec.DoubleValue MUSIC_RANGE = BUILDER
            .comment("How near a playing jukebox must be to restore. Only the nearest counts.")
            .defineInRange("range", MusicRules.DEFAULT.range(), 0, 256);

    private static final ModConfigSpec.DoubleValue FRESH = BUILDER
            .comment("The restore while that jukebox plays a Fresh disc, one not among the last two a player heard; each play of a Stale disc restores half its last play's.")
            .defineInRange("fresh", MusicRules.DEFAULT.fresh(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The instantaneous drains and restores, each in Sanity. A food's is its atouchofmadness:sanity_modifier data map entry.").push("moments");
    }

    private static final ModConfigSpec.DoubleValue SLEEP = BUILDER
            .comment("The restore to each player who wakes from sleeping through the night.")
            .defineInRange("sleep", MomentRules.DEFAULT.sleep(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue FLOWER = BUILDER
            .comment("The restore for each block in #minecraft:flowers a player breaks.")
            .defineInRange("flower", MomentRules.DEFAULT.flower(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue STRUCK_BY_THE_DARK = BUILDER
            .comment("The drain of each Strike by the Dark, on every difficulty. Only A Touch of Darkness strikes, so without it this takes nothing.")
            .defineInRange("struckByTheDark", MomentRules.DEFAULT.struckByTheDark(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("The Flower Crown: its restore in Sanity a minute, and how long it lasts.").push("crown");
    }

    private static final ModConfigSpec.DoubleValue CROWN_RESTORE = BUILDER
            .comment("The restore while a player wears a Flower Crown in the head slot.")
            .defineInRange("restore", CrownRules.DEFAULT.restore(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue CROWN_WEAR_DAYS = BUILDER
            .comment("How many in-game days of wear a Flower Crown lasts; it wears only while worn. A crown worn under another value keeps the share of its life it has used.")
            .defineInRange("wearDays", CrownRules.DEFAULT.wearDays(), 0.001, 1000);

    static {
        BUILDER.pop();
        BUILDER.comment("The Shadow Creatures, the Lurker and the Gaunt: when and where they come, what they do, and what they drain.").push("shadows");
    }

    private static final ModConfigSpec.ConfigValue<List<? extends Number>> CAPS = BUILDER
            .comment("A player Uneasy or Insane may have one Shadow Creature for each of these Sanity values theirs is at or below, Lurkers and Gaunts together.")
            .defineList("caps", ShadowRules.DEFAULT.caps(), () -> 0.0,
                    value -> value instanceof Number number && number.doubleValue() >= Sanity.MIN && number.doubleValue() <= Sanity.MAX);

    private static final ModConfigSpec.IntValue INTERVAL = BUILDER
            .comment("How many seconds apart a player under their cap gets a spawn attempt.")
            .defineInRange("intervalSeconds", ShadowRules.DEFAULT.intervalTicks() / 20, 1, 3600);

    private static final ModConfigSpec.DoubleValue MIN_DISTANCE = BUILDER
            .comment("The nearest a creature appears to its Owner, in blocks.")
            .defineInRange("minDistance", ShadowRules.DEFAULT.minDistance(), 0, 64);

    private static final ModConfigSpec.DoubleValue MAX_DISTANCE = BUILDER
            .comment("The farthest a creature appears from its Owner, in blocks. Set below minDistance, it is read as minDistance.")
            .defineInRange("maxDistance", ShadowRules.DEFAULT.maxDistance(), 0, 64);

    private static final ModConfigSpec.DoubleValue FADE_DISTANCE = BUILDER
            .comment("An Unreal creature fades once its Owner comes this near, in blocks.")
            .defineInRange("fadeDistance", ShadowRules.DEFAULT.fadeDistance(), 0, 64);

    private static final ModConfigSpec.DoubleValue GAUNT_BELOW = BUILDER
            .comment("A Gaunt may come only to a player whose Sanity is below this.")
            .defineInRange("gauntBelow", ShadowRules.DEFAULT.gauntBelow(), Sanity.MIN, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue GAUNT_CHANCE = BUILDER
            .comment("The chance that a creature coming below gauntBelow is a Gaunt rather than a Lurker.")
            .defineInRange("gauntChance", ShadowRules.DEFAULT.gauntChance(), 0, 1);

    private static final ModConfigSpec.DoubleValue REAL_FACTOR = BUILDER
            .comment("How many times its atouchofmadness:unsettling_weight a Real creature drains its Owner nearby, at the nearby section's hostile rate.")
            .defineInRange("realFactor", ShadowRules.DEFAULT.realFactor(), 0, 100);

    private static final ModConfigSpec.DoubleValue STRIKE = BUILDER
            .comment("The Sanity each hit from a Real creature takes at once.")
            .defineInRange("strike", ShadowRules.DEFAULT.strike(), 0, Sanity.MAX);

    private static final Stats LURKER = Stats.define("lurker", "The Lurker's stats; a change applies to Lurkers that come after it.",
            ShadowRules.DEFAULT.lurker());

    private static final Stats GAUNT = Stats.define("gaunt", "The Gaunt's stats; a change applies to Gaunts that come after it.",
            ShadowRules.DEFAULT.gaunt());

    /** A creature's health, attack damage, drift speed and kill restore, in a section of their own. */
    private record Stats(ModConfigSpec.DoubleValue health, ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue drift,
            ModConfigSpec.DoubleValue kill) {

        static Stats define(String name, String comment, ShadowRules.Stats defaults) {
            BUILDER.comment(comment).push(name);
            var stats = new Stats(
                    BUILDER.comment("Its health.").defineInRange("health", defaults.health(), 1, 1024),
                    BUILDER.comment("The damage of each of its hits, before vanilla scales it by difficulty.").defineInRange("damage", defaults.damage(), 0, 1024),
                    BUILDER.comment("How fast it drifts, through blocks and all, in blocks a second.").defineInRange("drift", defaults.drift(), 0, 40),
                    BUILDER.comment("The Sanity a player who kills it while Real restores at once.").defineInRange("kill", defaults.kill(), 0, Sanity.MAX));
            BUILDER.pop();
            return stats;
        }

        ShadowRules.Stats get() {
            return new ShadowRules.Stats(health.get(), damage.get(), drift.get(), kill.get());
        }
    }

    static {
        BUILDER.pop();
        BUILDER.comment("Umbra Gear's drains, each in Sanity a minute; nothing drains while only carried.").push("umbra");
    }

    private static final ModConfigSpec.DoubleValue UMBRA_SWORD = BUILDER
            .comment("The drain while a player holds an Umbra Sword, for each hand that holds one.")
            .defineInRange("sword", UmbraRules.DEFAULT.sword(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue UMBRA_PICKAXE = BUILDER
            .comment("The drain while a player holds an Umbra Pickaxe, for each hand that holds one.")
            .defineInRange("pickaxe", UmbraRules.DEFAULT.pickaxe(), 0, Sanity.MAX);

    private static final ModConfigSpec.DoubleValue UMBRA_ARMOUR = BUILDER
            .comment("The drain for each piece of Umbra Armour a player wears.")
            .defineInRange("armour", UmbraRules.DEFAULT.armour(), 0, Sanity.MAX);

    static {
        BUILDER.pop();
        BUILDER.comment("Discovery: what a player learns of Sanity by meeting it.").push("discovery");
    }

    private static final ModConfigSpec.BooleanValue DISCOVERY = BUILDER
            .comment("Whether the Pillar shows nothing of what a thing does to Sanity until the player has met it. Off, everything counts as known; the advancements are still earned.")
            .define("enabled", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final List<Live<?>> LIVE = new CopyOnWriteArrayList<>();
    private static final List<Runnable> RELOAD_LISTENERS = new CopyOnWriteArrayList<>();

    private MadnessConfig() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModConfigEvent.Loading.class, MadnessConfig::changed);
        modBus.addListener(ModConfigEvent.Reloading.class, MadnessConfig::changed);
    }

    /**
     * Rules read from the config by {@code read}: its defaults until the config loads, then read anew,
     * whole, each time it loads or reloads, possibly off the server thread.
     */
    public static <T> Supplier<T> live(Supplier<T> read) {
        var live = new Live<>(read);
        LIVE.add(live);
        return live;
    }

    /** Adds a listener that hears of every load and reload of the config, once each {@link #live} rules have been read anew. */
    public static void onReload(Runnable listener) {
        RELOAD_LISTENERS.add(listener);
    }

    private static void changed(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            LIVE.forEach(Live::read);
            RELOAD_LISTENERS.forEach(Runnable::run);
        }
    }

    private static final class Live<T> implements Supplier<T> {

        private final Supplier<T> read;
        private volatile T value;

        Live(Supplier<T> read) {
            this.read = read;
            value = read.get();
        }

        void read() {
            value = read.get();
        }

        @Override
        public T get() {
            return value;
        }
    }

    /** The defaults until the config loads. */
    public static SanityRules sanityRules() {
        if (!SPEC.isLoaded()) {
            return SanityRules.DEFAULT;
        }
        return new SanityRules(new Sanity(START.get()), new Sanity(RESPAWN.get()), SANE_ABOVE.get(), INSANE_BELOW.get());
    }

    /** The defaults until the config loads. */
    public static EnvironmentRules environmentRules() {
        if (!SPEC.isLoaded()) {
            return EnvironmentRules.DEFAULT;
        }
        return new EnvironmentRules(LOW_LIGHT_BELOW.get(), LOW_LIGHT.get(), FREEZING.get(), WET.get(), UNSETTLING.get());
    }

    /** The defaults until the config loads. */
    public static NearbyRules nearbyRules() {
        if (!SPEC.isLoaded()) {
            return NearbyRules.DEFAULT;
        }
        return new NearbyRules(HOSTILE_RADIUS.get(), HOSTILE.get(), HOSTILE_CAP.get(), COMPANY_RADIUS.get(), COMPANY.get());
    }

    /** The defaults until the config loads. */
    public static MusicRules musicRules() {
        if (!SPEC.isLoaded()) {
            return MusicRules.DEFAULT;
        }
        return new MusicRules(MUSIC_RANGE.get(), FRESH.get());
    }

    /** The defaults until the config loads. */
    public static MomentRules momentRules() {
        if (!SPEC.isLoaded()) {
            return MomentRules.DEFAULT;
        }
        return new MomentRules(SLEEP.get(), FLOWER.get(), STRUCK_BY_THE_DARK.get());
    }

    /** The defaults until the config loads. */
    public static ShadowRules shadowRules() {
        if (!SPEC.isLoaded()) {
            return ShadowRules.DEFAULT;
        }
        return new ShadowRules(CAPS.get().stream().map(Number::doubleValue).toList(), INTERVAL.get() * 20, MIN_DISTANCE.get(),
                MAX_DISTANCE.get(), FADE_DISTANCE.get(), GAUNT_BELOW.get(), GAUNT_CHANCE.get(), REAL_FACTOR.get(),
                STRIKE.get(), LURKER.get(), GAUNT.get());
    }

    /** On until the config loads. */
    public static boolean discoveryEnabled() {
        return !SPEC.isLoaded() || DISCOVERY.get();
    }

    /** The defaults until the config loads. */
    public static CrownRules crownRules() {
        if (!SPEC.isLoaded()) {
            return CrownRules.DEFAULT;
        }
        return new CrownRules(CROWN_RESTORE.get(), CROWN_WEAR_DAYS.get());
    }

    /** The defaults until the config loads. */
    public static UmbraRules umbraRules() {
        if (!SPEC.isLoaded()) {
            return UmbraRules.DEFAULT;
        }
        return new UmbraRules(UMBRA_SWORD.get(), UMBRA_PICKAXE.get(), UMBRA_ARMOUR.get());
    }
}
