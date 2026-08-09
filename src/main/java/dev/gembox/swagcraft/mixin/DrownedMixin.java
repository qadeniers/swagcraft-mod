package dev.gembox.swagcraft.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Drowned.class)
public abstract class DrownedMixin {
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
    private void removeTrident(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        Drowned drowned = (Drowned) (Object) this;

        if (drowned.getMainHandItem().is(Items.TRIDENT)) {
            drowned.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
}
