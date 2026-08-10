package dev.gembox.swagcraft.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTradingMixin {
    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void onTrade(MerchantOffer offer, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        if (!villager.level().isClientSide() && villager.level() instanceof ServerLevel serverLevel) {
            MerchantOffers offers = villager.getOffers();

            boolean resetAny = false;
            for (MerchantOffer merchantOffer : offers) {
                if (merchantOffer.isOutOfStock()) {
                    merchantOffer.resetUses();

                    resetAny = true;
                }
            }

            if (resetAny) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.0D, villager.getZ(), 7, 0.5D, 0.5D, 0.5D, 0.02D);
            }
        }
    }

    @Inject(method = "updateSpecialPrices", at = @At("HEAD"), cancellable = true)
    private void disablePriceAdjustments(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        for (MerchantOffer offer : villager.getOffers()) {
            offer.setSpecialPriceDiff(0);
        }
        ci.cancel();
    }

    @Inject(method = "updateDemand", at = @At("HEAD"), cancellable = true)
    private void freezeDemand(CallbackInfo ci) {
        ci.cancel();
    }
}
