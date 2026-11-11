package pl.adixfx.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.adixfx.Backtrack;

@Mixin(Entity.class)
public abstract class BacktrackEntityMixin {
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void adixfx$backtrackBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof PlayerEntity player) {
            cir.setReturnValue(Backtrack.historicalBox(player, cir.getReturnValue()));
        }
    }
}
