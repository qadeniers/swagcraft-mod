package dev.gembox.swagcraft.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Shadow private int tickTimer;

    @Shadow public void addExhaustion(final float amount) {}

    /**
     * @author bear
     * @reason archaic old hunger and regen rules (pre 1.8)
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(final ServerPlayer player, CallbackInfo ci) {
        ServerLevel level = player.level();
        Difficulty difficulty = level.getDifficulty();

        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        boolean naturalRegen = level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);

        // 0.5 hearts every 4 seconds when foodLevel >= 18
        if (naturalRegen && this.foodLevel >= 18 && player.isHurt()) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                player.heal(1.0F);
                this.addExhaustion(3.0F);
                this.tickTimer = 0;
            }
        }
        // starvation: 0.5 hearts damage every 4 seconds at 0 food
        else if (this.foodLevel <= 0) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                if (player.getHealth() >= 10.0F || difficulty == Difficulty.HARD || (player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL)) {
                    player.hurtServer(level, player.damageSources().starve(), 1.0F);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }

        ci.cancel();
    }
}
