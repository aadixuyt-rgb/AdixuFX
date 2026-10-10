package pl.adixfx;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

/**
 * Odtwarzany utwór. Jest "tickable", dzięki czemu głośność (suwaki w GUI) zmienia się
 * na żywo, w trakcie grania - a nie dopiero od następnego utworu.
 */
public final class TrackSound extends AbstractSoundInstance implements TickableSoundInstance {
    private final Playlist.Track track;
    private boolean done;

    public TrackSound(Playlist.Track track) {
        super(SoundEvent.of(track.sound()), SoundCategory.MASTER, Random.create());
        this.track = track;
        this.pitch = 1.0f;
        this.repeat = false;
        this.relative = true;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.volume = Music.gain(track);
    }

    public Playlist.Track track() {
        return track;
    }

    public void finish() {
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void tick() {
        this.volume = Music.gain(track);
    }
}
