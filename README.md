# AdixFX (Fabric 1.21.4, klient)

Osobny mod: **kolorowe hitboxy** + **odtwarzacz muzyki z losową playlistą** (bez żadnych efektów po zabiciu gracza). Wszystko włączane w GUI (domyślnie **prawy Shift**, zmienisz w Sterowaniu).

## GUI
**Hitboxy:** włącz/wyłącz • tryb: *jeden kolor (losowy, stały)* / *jeden kolor (zmienia się)* / *wiele kolorów naraz (zmieniają się)* • przycisk "Wylosuj nowy kolor" (tryb stały) • szybkość zmian • zasięg • filtry: gracze / wrogie / przyjazne / przedmioty / inne.

**Muzyka:** włącz/wyłącz • **głośność ogólna 0–200%** • przyciski **Skip ⏭** i **Stop ⏸ / Play ▶** • lista utworów: `[WŁ/WYŁ] [nazwa] [głośność utworu 0–200%] [✎]`.
- Klik w nazwę = ten utwór gra od razu, a po jego końcu startuje **losowy** (nie po kolei) spośród włączonych.
- **Skip** pomija obecny utwór i losuje następny spośród włączonych.
- **Stop ⏸** zatrzymuje utwór w miejscu, a przycisk zmienia się w **Play ▶** - po kliknięciu utwór wznawia się dokładnie od tego samego momentu. Gdy nic nie było odtwarzane, Play włącza ostatnio grany utwór (albo losowy).
- **✎** = zmiana nazwy utworu (przycisk "Domyślna" przywraca oryginalną).
- Głośność = ogólna × utworu. W Minecraftcie końcowa głośność nie może przekroczyć 100% mocy, więc **100% w GUI = połowa mocy, 200% = pełna** (żeby ustawienia powyżej 100% naprawdę coś zmieniały). Zmiana suwaka działa od razu, także w trakcie grania.
- Playlista: 24 utwory (ZELENUYU, HELLBLADE, HardTekk…, Starships, Love Me Not, Britney Spears, Rihanna i inne) - lista przewija się kółkiem myszy, każdy utwór ma własną nazwę, którą zmienisz przyciskiem ✎.

## Dodawanie kolejnych utworów
1. Wrzuć `.ogg` do `src/main/resources/assets/adixfx/sounds/`.
2. Dopisz go w `sounds.json` (wzór: `zelenuyu`).
3. Dopisz wpis w `Playlist.java`.

## Build
Wrzuć na GitHuba → Actions → artefakt **AdixFX-mod** (jar). Lokalnie: `gradle build`.
