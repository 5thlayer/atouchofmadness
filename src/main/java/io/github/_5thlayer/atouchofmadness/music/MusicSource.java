// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofmadness.music;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofmadness.ATouchOfMadness;
import io.github._5thlayer.atouchofmadness.MadnessConfig;
import io.github._5thlayer.atouchofmadness.discovery.Discovery;
import io.github._5thlayer.atouchofmadness.environment.EnvironmentRules;
import io.github._5thlayer.atouchofmadness.sanity.Cause;
import io.github._5thlayer.atouchofmadness.sanity.PlayerSanity;
import io.github._5thlayer.atouchofmadness.sanity.Rate;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The continuous restore of music: while the nearest jukebox playing within range of a player plays a
 * disc, they restore by that disc's freshness factor in their {@link DiscHistory} (ADR 0004). The
 * history is the player's own attachment, saved with them, kept through death, and the same at every
 * jukebox; only the nearest playing counts, so jukeboxes never stack. A disc is its jukebox song, and
 * distance is from the player's feet to the jukebox's centre. Each song a jukebox starts, from a player
 * or a hopper, is stamped on the jukebox with a new id, saved with it, so a song that pauses while its
 * chunk stops ticking, such as one a player logged out by on a server, goes on as the same play when it
 * comes back.
 */
public final class MusicSource {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfMadness.MOD_ID);

    private static final Codec<DiscHistory.Play> PLAY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("jukebox").forGetter(DiscHistory.Play::jukebox),
            Codec.LONG.fieldOf("song_id").forGetter(DiscHistory.Play::songId),
            Codec.STRING.fieldOf("disc").forGetter(DiscHistory.Play::disc),
            Codec.DOUBLE.fieldOf("factor").forGetter(DiscHistory.Play::factor)
    ).apply(instance, DiscHistory.Play::new));

    private static final Supplier<AttachmentType<DiscHistory>> HISTORY = ATTACHMENTS.register("disc_history",
            () -> AttachmentType.builder(() -> DiscHistory.EMPTY)
                    .serialize(PLAY_CODEC.listOf().xmap(DiscHistory::new, DiscHistory::plays).fieldOf("plays"))
                    .copyOnDeath()
                    .build());

    /**
     * The id of the song a jukebox plays, given when it started, and saved with the jukebox. Every read
     * checks it is there first, so its default is never read.
     */
    private static final Supplier<AttachmentType<Long>> SONG_ID = ATTACHMENTS.register("song_id",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("id")).build());

    /** The config's rules, swapped whole when it loads or reloads, possibly off the server thread. */
    private static volatile MusicRules rules = MusicRules.DEFAULT;

    private MusicSource() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(ModConfigEvent.Loading.class, MusicSource::configChanged);
        modBus.addListener(ModConfigEvent.Reloading.class, MusicSource::configChanged);
        PlayerSanity.addSource(MusicSource::music);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, BlockEvent.NeighborNotifyEvent.class,
                MusicSource::songChanged);
    }

    /**
     * Stamps a song on its jukebox as it starts, and clears the stamp as it stops: a jukebox notifies
     * its neighbours whenever it starts or stops a song, however its disc came or went, and even when
     * another mod cancels the notice. A song started while the game is frozen stays 0 ticks in, so a
     * notice there before the game runs on, which only a song changing gives, would stamp it again.
     */
    private static void songChanged(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        var songPlayer = jukebox.getSongPlayer();
        if (!songPlayer.isPlaying()) {
            if (jukebox.hasData(SONG_ID)) {
                jukebox.removeData(SONG_ID);
            }
        } else if (songPlayer.getTicksSinceSongStarted() == 0) {
            stamp(jukebox, level);
        }
    }

    /**
     * The restore of the play the player hears now, if any. Unlike other sources it also records the
     * play in their history, which only the once-a-second evaluation calls it to do.
     */
    private static Rate music(ServerPlayer player) {
        var current = rules;
        var found = nearestPlaying(player, current.range());
        if (found.isEmpty()) {
            return Rate.NONE;
        }
        var jukebox = found.get();
        var disc = disc(jukebox);
        if (disc.isEmpty()) {
            return Rate.NONE;
        }
        var heard = hear(player, jukebox, disc.get());
        player.setData(HISTORY, heard.history());
        double perTick = -EnvironmentRules.drainPerTick(current.restore(heard.factor()));
        // A Stale disc's play is music too.
        return heard.factor() < 1 ? Rate.of(perTick, Cause.MUSIC, Cause.STALE_DISC) : Rate.of(perTick, Cause.MUSIC);
    }

    /**
     * The freshness factor of the play a jukebox plays, for this player, as far as they may be shown
     * it: 1 for a Fresh disc, half its last play's for a Stale one, and nothing while the jukebox plays
     * no disc or until the player has discovered the Stale disc, before which even "Fresh" would give
     * away that freshness exists. It records nothing in their history, though it stamps a song with no
     * id as hearing it would, and is the same whether or not the player is in the jukebox's range.
     */
    public static OptionalDouble shownFreshness(ServerPlayer player, JukeboxBlockEntity jukebox) {
        if (!jukebox.getSongPlayer().isPlaying() || !Discovery.isKnown(player, Cause.STALE_DISC)) {
            return OptionalDouble.empty();
        }
        var disc = disc(jukebox);
        if (disc.isEmpty()) {
            return OptionalDouble.empty();
        }
        // The history the player would have once they heard it is thrown away: only the factor is wanted.
        return OptionalDouble.of(hear(player, jukebox, disc.get()).factor());
    }

    /**
     * The id of the song a jukebox plays, stamped now if it has none, as one that began playing without
     * starting, such as by a command, does.
     */
    private static long songId(JukeboxBlockEntity jukebox, ServerLevel level) {
        return jukebox.hasData(SONG_ID) ? jukebox.getData(SONG_ID) : stamp(jukebox, level);
    }

    /** Gives the song a jukebox plays a new id. */
    private static long stamp(JukeboxBlockEntity jukebox, ServerLevel level) {
        long id = level.getRandom().nextLong();
        jukebox.setData(SONG_ID, id);
        return id;
    }

    /** What hearing the jukebox's play of {@code disc} would make the player's history, which this doesn't set. */
    private static DiscHistory.Heard hear(ServerPlayer player, JukeboxBlockEntity jukebox, String disc) {
        return player.getData(HISTORY).hear(jukeboxId(jukebox), songId(jukebox, player.level()), disc);
    }

    /** The disc a jukebox holds, by its jukebox song's id, if it holds one. */
    private static Optional<String> disc(JukeboxBlockEntity jukebox) {
        return JukeboxSong.fromStack(jukebox.getTheItem()).flatMap(Holder::unwrapKey).map(key -> key.identifier().toString());
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
