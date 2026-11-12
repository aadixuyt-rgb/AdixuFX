package pl.adixfx;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Decodes an external OGG file to a short-lived PCM WAV for Java Sound Clip playback. */
final class OggAudio {
    private OggAudio() {}

    static Path decodeToTemporaryWav(Path oggFile) throws IOException {
        byte[] encoded = Files.readAllBytes(oggFile);
        if (encoded.length == 0) throw new IOException("Plik OGG jest pusty");

        ByteBuffer memory = MemoryUtil.memAlloc(encoded.length);
        long decoder = MemoryUtil.NULL;
        Path wav = Files.createTempFile("adixfx-playback-", ".wav");
        boolean success = false;

        try {
            memory.put(encoded).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                decoder = STBVorbis.stb_vorbis_open_memory(memory, error, null);
                if (decoder == MemoryUtil.NULL) {
                    throw new IOException("Nie można otworzyć OGG (STB Vorbis: " + error.get(0) + ")");
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channels = info.channels();
                int sampleRate = info.sample_rate();
                if (channels < 1 || channels > 2 || sampleRate < 8000) {
                    throw new IOException("Nieobsługiwany format audio OGG");
                }

                float[] samples = new float[8192 * channels];
                byte[] pcm = new byte[samples.length * 2];
                long dataBytes = 0;

                try (RandomAccessFile out = new RandomAccessFile(wav.toFile(), "rw")) {
                    out.setLength(0);
                    out.seek(44);
                    int frames;
                    while ((frames = STBVorbis.stb_vorbis_get_samples_float_interleaved(
                            decoder, channels, samples)) > 0) {
                        int sampleCount = frames * channels;
                        int byteCount = sampleCount * 2;
                        for (int i = 0; i < sampleCount; i++) {
                            float value = samples[i];
                            if (!Float.isFinite(value)) value = 0f;
                            value = Math.max(-1f, Math.min(1f, value));
                            int sample = Math.round(value * 32767f);
                            pcm[i * 2] = (byte) (sample & 0xFF);
                            pcm[i * 2 + 1] = (byte) ((sample >>> 8) & 0xFF);
                        }
                        out.write(pcm, 0, byteCount);
                        dataBytes += byteCount;
                    }
                    if (dataBytes == 0 || dataBytes > 0xFFFFFFFFL - 36L) {
                        throw new IOException("Nieprawidłowy rozmiar danych OGG");
                    }
                    out.seek(0);
                    writeWavHeader(out, channels, sampleRate, dataBytes);
                }
            }
            success = true;
            return wav;
        } finally {
            if (decoder != MemoryUtil.NULL) STBVorbis.stb_vorbis_close(decoder);
            MemoryUtil.memFree(memory);
            if (!success) Files.deleteIfExists(wav);
        }
    }

    private static void writeWavHeader(RandomAccessFile out, int channels, int sampleRate,
                                       long dataBytes) throws IOException {
        out.writeBytes("RIFF");
        writeIntLE(out, (int) (36L + dataBytes));
        out.writeBytes("WAVE");
        out.writeBytes("fmt ");
        writeIntLE(out, 16);
        writeShortLE(out, 1);
        writeShortLE(out, channels);
        writeIntLE(out, sampleRate);
        writeIntLE(out, sampleRate * channels * 2);
        writeShortLE(out, channels * 2);
        writeShortLE(out, 16);
        out.writeBytes("data");
        writeIntLE(out, (int) dataBytes);
    }

    private static void writeShortLE(RandomAccessFile out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private static void writeIntLE(RandomAccessFile out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }
}