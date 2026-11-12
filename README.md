# AdixFX (Fabric 1.21.4, klient)

Osobny mod: **kolorowe hitboxy** + **odtwarzacz muzyki z losową playlistą**. Wszystko włączane w GUI (domyślnie **prawy Shift**, zmienisz w Sterowaniu).

## GUI
**Hitboxy:** włącz/wyłącz • tryb: *jeden kolor (losowy, stały)* / *jeden kolor (zmienia się)* / *wiele kolorów naraz (zmieniają się)* • przycisk "Wylosuj nowy kolor" • szybkość zmian • zasięg • filtry.

**Muzyka:** włącz/wyłącz • głośność ogólna 0–200% • Skip ⏭ i Stop ⏸ / Play ▶ • lista utworów z osobną głośnością i możliwością zmiany nazwy.
- Klik w nazwę = ten utwór gra od razu, a po jego końcu startuje losowy włączony utwór.
- **Skip** pomija obecny utwór i losuje następny.
- **Stop / Play** wstrzymuje i wznawia ten sam utwór.
- Każdy plik OGG z folderu muzyki jest wykrywany automatycznie.

## Folder muzyki

Pliki audio są poza plikiem moda. AdixFX odczytuje je z:

`.minecraft/config/adixfx/music/`

Aby dodać domyślne 23 utwory, pobierz artefakt **AdixFX-Music-Pack.zip** z [tego starszego buildu](https://github.com/aadixuyt-rgb/AdixuFX/actions/runs/43093255313) i rozpakuj wszystkie pliki `.ogg` bezpośrednio do powyższego folderu. Nie wkładaj ich do JAR-a ani do podfolderu.

Nowe utwory dodawane przyciskiem **Dodaj utwór** (przeciągnięcie pliku MP3 lub wybór z dysku) są konwertowane do prawdziwego OGG Vorbis i zapisywane automatycznie w tym samym folderze. Nie są umieszczane w `.minecraft/config/adixfx/custom` ani w pliku moda.

## Build

Wrzuć na GitHuba → Actions → artefakt **AdixFX-mod** (jar). Lokalnie: `gradle build`.
