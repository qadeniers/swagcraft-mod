package dev.gembox.swagcraft.mixin;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PanicGoal.class)
public abstract class RabbitPanicGoalMixin {
    @Shadow @Final protected PathfinderMob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void preventRabbitPanicWhenProvoked(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Rabbit rabbit) {
            if (rabbit.getTarget() != null && rabbit.getTarget().isAlive()) {
                cir.setReturnValue(false);
            }
        }
    }
}
