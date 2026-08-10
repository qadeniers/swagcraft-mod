package dev.gembox.swagcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerBreedingMixin {
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void checkDoorsForBreeding(CallbackInfoReturnable<Boolean> cir) {
        Villager villager = (Villager) (Object) this;
        Level level = villager.level();
        BlockPos center = villager.blockPosition();

        int doorCOunt = 0;
        int radius = 16;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (level.getBlockState(pos).is(BlockTags.WOODEN_DOORS)) {
                doorCOunt++;
            }
        }

        boolean enoughDoors = doorCOunt >= 3;
        boolean isAdult = villager.getAge() == 0;
        boolean hasFood = villager.hasExcessFood();

        cir.setReturnValue(enoughDoors && isAdult && hasFood);
    }
}
