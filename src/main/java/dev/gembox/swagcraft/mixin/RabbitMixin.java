package dev.gembox.swagcraft.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Rabbit.class)
public abstract class RabbitMixin extends Animal {
    protected RabbitMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private static void addBunnyDamageAttribute(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(cir.getReturnValue().add(Attributes.ATTACK_DAMAGE, 10.0D).add(Attributes.MAX_HEALTH, 16.0D));
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void addProvokedHostilityGoals(CallbackInfo ci) {
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4D, true));
    }
}
