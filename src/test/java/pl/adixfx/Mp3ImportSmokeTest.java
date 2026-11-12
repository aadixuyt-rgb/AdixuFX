package pl.adixfx;

import java.nio.file.Files;
import java.nio.file.Path;

/** End-to-end smoke test for importing an MP3 file and cleaning its playlist title. */
public final class Mp3ImportSmokeTest {
    private Mp3ImportSmokeTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected input MP3 path and output OGG path");
        }

        Path mp3 = Path.of(args[0]);
        Path ogg = Path.of(args[1]);
        if (!Files.isRegularFile(mp3) || Files.size(mp3) == 0) {
            throw new IllegalStateException("Missing MP3 smoke-test input: " + mp3);
        }

        TrackConverter.convert(mp3, ogg);
        byte[] bytes = Files.readAllBytes(ogg);
        if (bytes.length < 128 || bytes[0] != 'O' || bytes[1] != 'g'
                || bytes[2] != 'g' || bytes[3] != 'S') {
            throw new IllegalStateException("MP3 import did not create an OGG file");
        }

        expectName("YTSave_YouTube_Media_dQw4w9WgXcQ_My_Favorite_Song_(Official_Video)_128kbps.mp3",
                "My Favorite Song");
        expectName("Artist_-_Track_[Lyrics]_[320kbps].mp3", "Artist Track");
        expectName("ytsave.com - Starships (Official Audio).mp3", "Starships");
        expectName("Track.mp3", "Track");

        System.out.println("MP3 import and filename-cleanup smoke test passed: " + bytes.length + " OGG bytes");
    }

    private static void expectName(String input, String expected) {
        String actual = TrackNameCleaner.clean(input);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Filename cleanup failed for '" + input
                    + "': expected '" + expected + "', got '" + actual + "'");
        }
    }
}