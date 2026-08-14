package dev.gembox.swagcraft.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Inject(method = "calculateIncreasedRepairCost", at = @At("RETURN"), cancellable = true)
    private static void capPriorWorkPenalty(int oldRepairCost, CallbackInfoReturnable<Integer> cir) {
        int maxPenalty = 15;

        if (cir.getReturnValue() > maxPenalty) {
            cir.setReturnValue(maxPenalty);
        }
    }
}
