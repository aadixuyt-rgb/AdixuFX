package pl.adixfx.mixin;

import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.adixfx.Music;

/**
 * Gdy w singleplayerze otwierasz ekran, który pauzuje grę (ekwipunek, Esc...), silnik dźwięku
 * Minecrafta sam wznawia WSZYSTKIE dźwięki po zamknięciu takiego ekranu - również ten, który
 * zatrzymaliśmy własnym przyciskiem Stop. Przez ułamek sekundy leciał więc fragment utworu
 * od złego miejsca (słychać to jako pisk), zanim nasz kod zdążył go z powrotem zapauzować.
 * Ten mixin łapie ten moment od razu, w tej samej chwili co wznowienie, więc nic nie zdąży zabrzmieć.
 */
@Mixin(SoundManager.class)
public class SoundManagerResumeMixin {
    @Inject(method = "resumeAll", at = @At("TAIL"))
    private void adixfx$resumeAll(CallbackInfo ci) {
        Music.reassertPauseIfNeeded();
    }
}
