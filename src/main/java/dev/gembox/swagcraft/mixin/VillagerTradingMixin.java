package dev.gembox.swagcraft.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Villager.class)
public abstract class VillagerTradingMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void catchUnemployedVillagers(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        if (!villager.level().isClientSide()) {
            VillagerData data = villager.getVillagerData();
            Holder<VillagerProfession> prof = data.profession();

            if (prof.is(VillagerProfession.NONE) || prof.is(VillagerProfession.NITWIT)) {
                Holder<VillagerProfession> newProf = getRandomProfession(villager.getRandom(), prof);
                if (!newProf.is(VillagerProfession.NONE) && !prof.is(VillagerProfession.NITWIT)) {
                    villager.setVillagerData(new VillagerData(data.type(), newProf, data.level()));
                }
            }
        }
    }

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

    @ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true, name = "data")
    private VillagerData assignPermanentProfession(VillagerData incoming) {
        Villager villager = (Villager) (Object) this;
        VillagerData currentData = villager.getVillagerData();

        boolean incomingIsNone = incoming.profession().is(VillagerProfession.NONE);
        boolean currentIsNone = currentData.profession().is(VillagerProfession.NONE);

        if (incomingIsNone && !currentIsNone) {
            return new VillagerData(incoming.type(), currentData.profession(), incoming.level());
        }

        if (incomingIsNone) {
            Holder<VillagerProfession> randomProf = getRandomProfession(villager.getRandom(), incoming.profession());
            return new VillagerData(incoming.type(), randomProf, incoming.level());
        }

        return incoming;
    }

    @Unique
    private Holder<VillagerProfession> getRandomProfession(RandomSource random, Holder<VillagerProfession> fallback) {
        List<Holder<VillagerProfession>> validProfessions = new ArrayList<>();

        for (VillagerProfession prof : BuiltInRegistries.VILLAGER_PROFESSION) {
            BuiltInRegistries.VILLAGER_PROFESSION.getResourceKey(prof)
                    .flatMap(BuiltInRegistries.VILLAGER_PROFESSION::get)
                    .ifPresent(holder -> {
                        if (!holder.is(VillagerProfession.NONE) && !holder.is(VillagerProfession.NITWIT)) {
                            validProfessions.add(holder);
                        }
                    });
        }

        if (validProfessions.isEmpty()) {
            return fallback;
        }

        return validProfessions.get(random.nextInt(validProfessions.size()));
    }
}
