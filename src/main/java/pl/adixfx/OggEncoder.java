package pl.adixfx;

import biniu.ogg.Packet;
import biniu.ogg.Page;
import biniu.ogg.StreamState;
import biniu.vorbis.Block;
import biniu.vorbis.Comment;
import biniu.vorbis.DspState;
import biniu.vorbis.Info;
import biniu.vorbis.VorbisEnc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Pure-Java OGG/Vorbis encoder for imported PCM WAV files. */
final class OggEncoder {
    private static final int FRAMES_PER_CHUNK = 1024;

    private OggEncoder() {}

    static void encode(Path wav, Path output) throws Exception {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(wav.toFile())) {
            AudioFormat format = input.getFormat();
            int channels = format.getChannels();
            int sampleRate = Math.round(format.getSampleRate());
            int frameSize = format.getFrameSize();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                    || format.getSampleSizeInBits() != 16
                    || channels < 1 || channels > 2
                    || frameSize < channels * 2 || sampleRate < 8000) {
                throw new IOException("Oczekiwano 16-bitowego PCM WAV mono/stereo");
            }

            Files.createDirectories(output.getParent());
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(output))) {
                encodePcm(input, out, channels, sampleRate, frameSize, format.isBigEndian());
            }
        }
    }

    private static void encodePcm(AudioInputStream input, OutputStream out, int channels,
                                  int sampleRate, int frameSize, boolean bigEndian) throws Exception {
        Info info = new Info();
        Comment comment = new Comment();
        DspState dsp = null;
        Block block = null;
        StreamState stream = new StreamState();
        Page page = new Page();
        Packet packet = new Packet();
        boolean infoInitialized = false;
        boolean commentInitialized = false;
        boolean streamInitialized = false;

        try {
            info.init();
            infoInitialized = true;
            VorbisEnc encoder = new VorbisEnc();
            int result = encoder.initVBR(info, channels, sampleRate, 0.1f);
            if (result != 0) throw new IOException("Nie można skonfigurować enkodera Vorbis: " + result);

            comment.init();
            commentInitialized = true;
            comment.addTag("ENCODER", "AdixFX");

            dsp = new DspState();
            dsp.analysisInit(info);
            block = new Block(dsp);
            block.blockInit(dsp);
            stream.init(new Random().nextInt());
            streamInitialized = true;

            Packet header = new Packet();
            Packet headerComment = new Packet();
            Packet headerCode = new Packet();
            dsp.analysisHeaderOut(comment, header, headerComment, headerCode);
            stream.packetIn(header);
            stream.packetIn(headerComment);
            stream.packetIn(headerCode);
            while (stream.flush(page)) writePage(out, page);

            byte[] readBuffer = new byte[FRAMES_PER_CHUNK * frameSize];
            boolean inputEnded = false;
            boolean eos = false;

            while (!eos) {
                if (!inputEnded) {
                    int bytes = input.read(readBuffer);
                    if (bytes < 0) {
                        inputEnded = true;
                        dsp.analysisWrote(0);
                    } else if (bytes > 0) {
                        int frames = bytes / frameSize;
                        if (frames == 0) continue;
                        float[][] pcm = dsp.analysisBuffer(frames);
                        int base = dsp.pcm_current;
                        for (int frame = 0; frame < frames; frame++) {
                            int frameOffset = frame * frameSize;
                            for (int channel = 0; channel < channels; channel++) {
                                int offset = frameOffset + channel * 2;
                                int first = readBuffer[offset] & 0xFF;
                                int second = readBuffer[offset + 1] & 0xFF;
                                short sample = bigEndian
                                        ? (short) ((first << 8) | second)
                                        : (short) ((second << 8) | first);
                                pcm[channel][base + frame] = sample / 32768.0f;
                            }
                        }
                        dsp.analysisWrote(frames);
                    }
                }

                while (block.analysisBlockOut()) {
                    block.analysis(null);
                    block.bitrateAddBlock();
                    while (dsp.bitrateFlushPacket(packet)) {
                        stream.packetIn(packet);
                        while (stream.pageOut(page)) {
                            writePage(out, page);
                            if (page.eos()) {
                                eos = true;
                                break;
                            }
                        }
                        if (eos) break;
                    }
                    if (eos) break;
                }
                if (inputEnded && !eos) {
                    throw new IOException("Enkoder OGG nie zamknął strumienia po końcu pliku WAV");
                }
            }
        } finally {
            if (streamInitialized) stream.clear();
            if (block != null) block.clear();
            if (dsp != null) dsp.clear();
            if (commentInitialized) comment.clear();
            if (infoInitialized) info.clear();
        }
    }

    private static void writePage(OutputStream out, Page page) throws IOException {
        out.write(page.header_base, page.header, page.header_len);
        out.write(page.body_base, page.body, page.body_len);
    }
}