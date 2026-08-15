package dev.gembox.swagcraft.mixin;

import dev.gembox.swagcraft.Mod;
import dev.gembox.swagcraft.util.CachedVillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(Villager.class)
public abstract class VillageManagerMixin extends AbstractVillager {
    @Shadow
    @Final
    public static Map<Item, Integer> FOOD_POINTS;
    @Unique private static final WeakHashMap<ServerLevel, Map<Long, CachedVillageData>> SHARED_CACHE = new WeakHashMap<>();

    @Unique private int cachedLegacyDoorCount = 0;
    @Unique private int cachedLegacyVillagerCount = 0;
    @Unique private long lastCacheUpdateTick = 0;

    protected VillageManagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void updateVillageCache(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        if (!villager.level().isClientSide() && villager.level() instanceof ServerLevel serverLevel) {
            long currentTime = serverLevel.getGameTime();
            // stagger ticking by ID
            if (this.lastCacheUpdateTick == 0 || currentTime - this.lastCacheUpdateTick >= 200) {
                BlockPos center = villager.blockPosition();

                int radius = 16;
                int doorCount = 0;

                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            mutable.setWithOffset(center, x, y, z);
                            if (serverLevel.getBlockState(mutable).is(BlockTags.WOODEN_DOORS)) {
                                doorCount++;
                            }
                        }
                    }
                }

                this.cachedLegacyDoorCount = doorCount;

                var box = villager.getBoundingBox().inflate(radius, 4.0D, radius);

                List<Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, box);

                this.cachedLegacyVillagerCount = villagers.size();

                if (this.cachedLegacyDoorCount >= 21 && this.cachedLegacyVillagerCount >= 10) {
                    List<IronGolem> golems = serverLevel.getEntitiesOfClass(IronGolem.class, villager.getBoundingBox().inflate(16.0D, 8.0D, 16.0D));
                    if (golems.size() < this.cachedLegacyVillagerCount / 10) {
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

    @Inject(method = "countFoodPointsInInventory", at = @At("HEAD"), cancellable = true)
    private void fixFoodPoints(CallbackInfoReturnable<Integer> cir) {
        Villager villager = (Villager) (Object) this;
        SimpleContainer inventory = villager.getInventory();
        int points = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (!stack.isEmpty()) {
                Integer foodValue = FOOD_POINTS.get(stack.getItem());

                if (foodValue != null) {
                    points += stack.getCount() * foodValue;
                }
            }
        }

        cir.setReturnValue(points);
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
