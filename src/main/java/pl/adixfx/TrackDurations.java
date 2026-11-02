package pl.adixfx;

import java.util.Map;

/**
 * Długości (w sekundach) wbudowanych utworów, zmierzone raz z plików .ogg w assets/adixfx/sounds.
 * Silnik dźwięku Minecrafta nie udostępnia łatwo całkowitej długości granego dźwięku, więc do
 * paska postępu w GUI potrzebujemy tych wartości policzonych z góry.
 */
final class TrackDurations {
    private TrackDurations() {}

    private static final Map<String, Float> SECONDS = Map.ofEntries(
            Map.entry("zelenuyu", 109.14f),
            Map.entry("starships", 233.85f),
            Map.entry("hellblade_super", 96.11f),
            Map.entry("hellblade_ultra", 170.60f),
            Map.entry("my_destiny", 326.69f),
            Map.entry("musor_drop", 67.00f),
            Map.entry("zani_preparaty", 70.80f),
            Map.entry("dark_triad", 63.55f),
            Map.entry("misery", 166.40f),
            Map.entry("meant_to_be", 167.96f),
            Map.entry("sexyback", 150.56f),
            Map.entry("calabria", 246.82f),
            Map.entry("sometimes", 102.41f),
            Map.entry("so_lucky", 112.88f),
            Map.entry("love_me_not", 213.58f),
            Map.entry("lush_life", 201.08f),
            Map.entry("gimme_more", 251.73f),
            Map.entry("on_the_floor", 285.02f),
            Map.entry("danza_kuduro", 238.98f),
            Map.entry("oops_again", 210.23f),
            Map.entry("calabria_slowed", 277.02f),
            Map.entry("sm_rihanna", 243.60f),
            Map.entry("toca_toca", 169.06f)
    );

    /** @return długość w sekundach, albo 0 jeśli nieznana (np. nowy wbudowany utwór bez wpisu). */
    static float get(String trackId) {
        return SECONDS.getOrDefault(trackId, 0f);
    }
}
