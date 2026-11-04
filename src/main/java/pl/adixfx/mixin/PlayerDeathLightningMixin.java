package pl.adixfx.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.adixfx.KillLightning;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class PlayerDeathLightningMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void adixfx$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        KillLightning.onEntityStatus(packet);
    }
}
