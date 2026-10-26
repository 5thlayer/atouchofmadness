// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The continuous restore of music: while the nearest jukebox playing within range of a player plays a
 * disc, they restore by that disc's freshness factor in their {@link DiscHistory} (ADR 0004). The
 * history is the player's own attachment, saved with them, kept through death, and the same at every
 * jukebox; only the nearest playing counts, so jukeboxes never stack. A disc is its jukebox song, and
 * distance is from the player's feet to the jukebox's centre. A play is timed by the game time, so a
 * jukebox whose chunk stopped ticking while the player was away, such as one they logged out by on a
 * server, plays on as a new play: a Stale one, never a Fresh one.
 */
public final class MusicSource {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfMadness.MOD_ID);

    private static final Codec<DiscHistory.Play> PLAY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("jukebox").forGetter(DiscHistory.Play::jukebox),
            Codec.STRING.fieldOf("disc").forGetter(DiscHistory.Play::disc),
            Codec.LONG.fieldOf("ticks").forGetter(DiscHistory.Play::ticks),
            Codec.LONG.fieldOf("heard_at").forGetter(DiscHistory.Play::heardAt),
            Codec.DOUBLE.fieldOf("factor").forGetter(DiscHistory.Play::factor)
    ).apply(instance, DiscHistory.Play::new));

    private static final Supplier<AttachmentType<DiscHistory>> HISTORY = ATTACHMENTS.register("disc_history",
            () -> AttachmentType.builder(() -> DiscHistory.EMPTY)
                    .serialize(PLAY_CODEC.listOf().xmap(DiscHistory::new, DiscHistory::plays).fieldOf("plays"))
                    .copyOnDeath()
                    .build());

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile MusicRules rules = MusicRules.DEFAULT;

    private MusicSource() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(ModConfigEvent.Loading.class, MusicSource::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, MusicSource::configChanged);
        PlayerSanity.addSource(MusicSource::music);
    }

    /**
     * The restore of the play the player hears now, if any. Unlike other sources it also records the
     * play in their history, which only the once-a-second evaluation calls it to do.
     */
    private static double music(ServerPlayer player) {
        var current = rules;
        var found = nearestPlaying(player, current.range());
        if (found.isEmpty()) {
            return 0;
        }
        var jukebox = found.get();
        var disc = JukeboxSong.fromStack(jukebox.getTheItem()).flatMap(Holder::unwrapKey);
        if (disc.isEmpty()) {
            return 0;
        }
        var heard = player.getData(HISTORY).hear(jukeboxId(jukebox), disc.get().identifier().toString(),
                jukebox.getSongPlayer().getTicksSinceSongStarted(), player.level().getGameTime());
        player.setData(HISTORY, heard.history());
        return -EnvironmentRules.drainPerTick(current.restore(heard.factor()));
    }

    /** A jukebox's id in a history: its dimension and position. */
    private static String jukeboxId(JukeboxBlockEntity jukebox) {
        var pos = jukebox.getBlockPos();
        return jukebox.getLevel().dimension().identifier() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    /**
     * The playing jukebox nearest the player within {@code range}, among those whose chunks tick: one in
     * a chunk that doesn't would play on forever, its song never moving.
     */
    private static Optional<JukeboxBlockEntity> nearestPlaying(ServerPlayer player, double range) {
        var level = player.level();
        var feet = player.position();
        JukeboxBlockEntity nearest = null;
        double nearestSqr = range * range;
        for (int x = SectionPos.posToSectionCoord(feet.x - range); x <= SectionPos.posToSectionCoord(feet.x + range); x++) {
            for (int z = SectionPos.posToSectionCoord(feet.z - range); z <= SectionPos.posToSectionCoord(feet.z + range); z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof JukeboxBlockEntity jukebox && jukebox.getSongPlayer().isPlaying()
                            && level.shouldTickBlocksAt(jukebox.getBlockPos())) {
                        double distanceSqr = Vec3.atCenterOf(jukebox.getBlockPos()).distanceToSqr(feet);
                        if (distanceSqr <= nearestSqr) {
                            nearest = jukebox;
                            nearestSqr = distanceSqr;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == MadnessConfig.SPEC) {
            rules = MadnessConfig.musicRules();
        }
    }
}
