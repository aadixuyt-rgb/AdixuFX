package pl.adixfx;

import biniu.ogg.Packet;
import biniu.ogg.Page;
import biniu.ogg.StreamState;
import biniu.vorbis.Block;
import biniu.vorbis.Comment;
import biniu.vorbis.DspState;
import biniu.vorbis.Info;
import biniu.vorbis.VorbisEnc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Encodes interleaved 16-bit little-endian stereo PCM as a real Ogg Vorbis file. */
final class OggVorbisEncoder {
    private static final int CHANNELS = 2;
    private static final int FRAMES_PER_BUFFER = 1024;

    private OggVorbisEncoder() {}

    static void encode(byte[] pcm, int sampleRate, Path output) throws IOException {
        if (pcm == null || pcm.length == 0 || pcm.length % (CHANNELS * 2) != 0) {
            throw new IOException("Nieprawidłowe dane PCM do konwersji OGG");
        }

        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);

        Info info = new Info();
        Comment comment = new Comment();
        DspState dsp = new DspState();
        Block block = null;
        StreamState stream = new StreamState();

        boolean initialized = false;
        try (InputStream input = new BufferedInputStream(new ByteArrayInputStream(pcm));
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(output))) {
            info.init();
            VorbisEnc encoder = new VorbisEnc();
            int result = encoder.initVBR(info, CHANNELS, sampleRate, 0.35f);
            if (result != 0) {
                throw new IOException("Nie udało się uruchomić kodera Vorbis (kod " + result + ")");
            }

            comment.init();
            comment.addTag("ENCODER", "AdixFX");
            if (dsp.analysisInit(info) != 0) {
                throw new IOException("Nie udało się zainicjalizować analizy Vorbis");
            }
            block = new Block(dsp);
            if (block.blockInit(dsp) != 0) {
                throw new IOException("Nie udało się zainicjalizować bloku Vorbis");
            }
            stream.init(new Random().nextInt());
            initialized = true;

            Packet header = new Packet();
            Packet headerComment = new Packet();
            Packet headerCodebook = new Packet();
            if (dsp.analysisHeaderOut(comment, header, headerComment, headerCodebook) != 0) {
                throw new IOException("Nie udało się utworzyć nagłówka OGG");
            }
            stream.packetIn(header);
            stream.packetIn(headerComment);
            stream.packetIn(headerCodebook);

            Page page = new Page();
            while (stream.flush(page)) {
                writePage(out, page);
            }

            Packet packet = new Packet();
            byte[] readBuffer = new byte[FRAMES_PER_BUFFER * CHANNELS * 2];
            boolean inputEnded = false;
            boolean eos = false;

            while (!eos) {
                if (!inputEnded) {
                    int bytes = input.read(readBuffer);
                    if (bytes < 0) {
                        dsp.analysisWrote(0);
                        inputEnded = true;
                    } else if (bytes > 0) {
                        if (bytes % (CHANNELS * 2) != 0) {
                            throw new IOException("Niepełna ramka PCM podczas kodowania OGG");
                        }
                        int frames = bytes / (CHANNELS * 2);
                        float[][] pcmBuffer = dsp.analysisBuffer(FRAMES_PER_BUFFER);
                        int base = dsp.pcm_current;
                        for (int i = 0; i < frames; i++) {
                            int offset = i * 4;
                            short left = (short) ((readBuffer[offset + 1] << 8) | (readBuffer[offset] & 0xFF));
                            short right = (short) ((readBuffer[offset + 3] << 8) | (readBuffer[offset + 2] & 0xFF));
                            pcmBuffer[0][base + i] = left / 32768.0f;
                            pcmBuffer[1][base + i] = right / 32768.0f;
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
            }
            out.flush();
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Nie udało się zakodować pliku OGG", e);
        } finally {
            if (initialized) {
                stream.clear();
                if (block != null) block.clear();
                dsp.clear();
                comment.clear();
                info.clear();
            }
        }
    }

    private static void writePage(OutputStream out, Page page) throws IOException {
        out.write(page.header_base, page.header, page.header_len);
        out.write(page.body_base, page.body, page.body_len);
    }
}
