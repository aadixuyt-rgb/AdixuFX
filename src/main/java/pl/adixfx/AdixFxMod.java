package pl.adixfx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class AdixFxMod implements ClientModInitializer {
    public static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        // domyślnie: prawy Shift (344)
        openMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.adixfx.open_menu", InputUtil.Type.KEYSYM, 344, "category.adixfx"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openMenuKey.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new AdixFxScreen());
            }
            Music.tick(mc);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(Hitboxes::render);
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> Lyrics.render(ctx, tickDelta.getTickProgress(false)));
    }
}
