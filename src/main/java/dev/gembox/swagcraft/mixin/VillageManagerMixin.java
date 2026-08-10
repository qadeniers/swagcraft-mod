package dev.gembox.swagcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Villager.class)
public abstract class VillageManagerMixin extends AbstractVillager {
    @Shadow
    private int villagerXp;
    @Unique private int cachedLegacyDoorCount = 0;
    @Unique private int cachedLegacyVillagerCount = 0;

    protected VillageManagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void updateVillageCache(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        if (!villager.level().isClientSide() && villager.level() instanceof ServerLevel serverLevel) {
            // stagger ticking by ID
            if ((villager.tickCount + villager.getId()) % 200 == 0) {
                BlockPos center = villager.blockPosition();

                int radius = 16;
                int doorCount = 0;

                BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            mutableBlockPos.setWithOffset(center, x, y, z);
                            if (serverLevel.getBlockState(mutableBlockPos).is(BlockTags.WOODEN_DOORS)) doorCount++;
                        }
                    }
                }

                this.cachedLegacyDoorCount = doorCount;

                List<Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(radius, 4.0D, radius));

                this.cachedLegacyVillagerCount = villagers.size();

                if (this.cachedLegacyDoorCount >= 21 && this.cachedLegacyVillagerCount >= 10) {
                    List<IronGolem> existingGolems = serverLevel.getEntitiesOfClass(IronGolem.class, villager.getBoundingBox().inflate(16.0D, 8.0D, 16.0D));

                    if (existingGolems.size() < this.cachedLegacyVillagerCount / 10) {
                        IronGolem golem = EntityTypes.IRON_GOLEM.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                        if (golem != null) {
                            golem.snapTo(villager.getX(), villager.getY(), villager.getZ(), 0.0F, 0.0F);
                            serverLevel.addFreshEntity(golem);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void breedingCheck(CallbackInfoReturnable<Boolean> cir) {
        Villager villager = (Villager) (Object) this;

        boolean underPopulationCap = this.cachedLegacyVillagerCount < (int) (this.cachedLegacyDoorCount * 0.35D);
        boolean isAdult = villager.getAge() == 0;
        boolean hasFood = villager.hasExcessFood();

        cir.setReturnValue(underPopulationCap && isAdult && hasFood);
    }
}
