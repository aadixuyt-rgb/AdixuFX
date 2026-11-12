package pl.adixfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds a readable playlist title from common downloader-generated filenames. */
final class TrackNameCleaner {
    private static final Set<String> JUNK = Set.of(
            "ytsave", "yt5s", "yt1s", "y2mate", "savefrom", "youtube",
            "media", "download", "downloader", "www", "com", "net", "org",
            "mp3", "ogg", "wav", "m4a", "aac", "flac", "webm",
            "official", "audio", "video", "lyrics", "lyric", "visualizer",
            "visualiser", "musicvideo", "hd", "sd", "4k", "8k"
    );

    private TrackNameCleaner() {}

    static String clean(String filename) {
        if (filename == null || filename.isBlank()) return "Nowy utwór";

        String base = filename.trim();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        // Remove common downloader/domain prefixes even when they are written as one token.
        base = base.replaceAll("(?i)https?://", " ");
        base = base.replaceAll("(?i)(?:www\\.)?(?:ytsave|yt5s|yt1s|y2mate|savefrom|ssyoutube)(?:\\.(?:com|net|org))?", " ");

        // Remove bracketed labels such as [Official Audio], (Lyrics), or [128kbps].
        base = base.replaceAll("(?i)[\\[(（{][^\\])）}]*?(?:official|audio|video|lyrics?|visuali[sz]er|download|\\d{2,3}\\s*k(?:bps)?|\\d{3,4}p|4k|8k)[^\\])）}]*[\\])）}]", " ");

        base = base.replace('_', ' ').replace('-', ' ').replace('.', ' ');
        base = base.replaceAll("[\\[\\](){},;|]+", " ");
        base = base.replaceAll("\\s+", " ").trim();

        List<String> kept = new ArrayList<>();
        for (String token : base.split(" ")) {
            String cleaned = token.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
            if (cleaned.isBlank()) continue;
            String lower = cleaned.toLowerCase(Locale.ROOT);

            if (JUNK.contains(lower)) continue;
            if (lower.matches("\\d{2,3}k(?:bps)?") || lower.matches("\\d{3,4}p")) continue;
            if (lower.matches("\\d+")) continue;

            // Typical 11-character YouTube ID, e.g. dQw4w9WgXcQ.
            if (cleaned.matches("[A-Za-z0-9_-]{11}")
                    && cleaned.matches(".*\\d.*")
                    && cleaned.matches(".*[A-Z].*")
                    && cleaned.matches(".*[a-z].*")) continue;

            // Some downloaders append a long mixed-case video ID without separators.
            if (cleaned.length() >= 10 && cleaned.length() <= 15
                    && cleaned.matches(".*\\d.*")
                    && cleaned.matches(".*[A-Za-z].*")
                    && cleaned.matches(".*[A-Z].*")
                    && cleaned.matches(".*[a-z].*")) continue;

            kept.add(lower);
        }

        if (kept.isEmpty()) return "Nowy utwór";

        StringBuilder result = new StringBuilder();
        for (String token : kept) {
            if (result.length() > 0) result.append(' ');
            int first = token.codePointAt(0);
            result.appendCodePoint(Character.toUpperCase(first));
            result.append(token.substring(Character.charCount(first)));
        }
        return result.toString();
    }
}