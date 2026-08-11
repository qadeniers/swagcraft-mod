package dev.gembox.swagcraft.mixin;

import dev.gembox.swagcraft.util.CachedVillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private static final WeakHashMap<ServerLevel, Map<Long, CachedVillageData>> SHARED_CACHE = new WeakHashMap<>();

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
                long chunkKey = (((long) (center.getX() >> 4)) & 0xFFFFFFFFL) | ((((long) (center.getZ() >> 4)) & 0xFFFFFFFFL) << 32);

                Map<Long, CachedVillageData> dimCache = SHARED_CACHE.computeIfAbsent(serverLevel, k -> new HashMap<>());
                CachedVillageData data = dimCache.get(chunkKey);
                long currentTick = serverLevel.getGameTime();

                if (data == null || currentTick - data.lastUpdateTick > 200) {
                    data = calculateVillageData(serverLevel, center, villager);
                    data.lastUpdateTick = currentTick;
                    dimCache.put(chunkKey, data);
                }

                this.cachedLegacyDoorCount = data.doorCount;
                this.cachedLegacyVillagerCount = data.villagerCount;

                if (this.cachedLegacyDoorCount >= 21 && this.cachedLegacyVillagerCount >= 10) {
                    if (data.golemCount < this.cachedLegacyVillagerCount / 10) {
                        IronGolem golem = EntityTypes.IRON_GOLEM.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
                        if (golem != null) {
                            golem.snapTo(villager.getX(), villager.getY(), villager.getZ(), 0.0F, 0.0F);
                            serverLevel.addFreshEntity(golem);
                            data.golemCount++;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private CachedVillageData calculateVillageData(ServerLevel level, BlockPos center, Villager villager) {
        CachedVillageData data = new CachedVillageData();
        int radius = 16;

        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = center.getY() - 4;
        int maxY = center.getY() + 4;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) {
                    int startX = Math.max(minX, cx << 4);
                    int endX = Math.min(maxX, (cx << 4) + 15);
                    int startZ = Math.max(minZ, cz << 4);
                    int endZ = Math.min(maxZ, (cz << 4) + 15);

                    int safeMinY = Math.max(minY, chunk.getMinY());
                    int safeMaxY = Math.max(maxY, chunk.getMaxY() - 1);

                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            for (int y = safeMinY; y <= safeMinY; y++) {
                                if (chunk.getBlockState(mutable.set(x, y, z)).is(BlockTags.WOODEN_DOORS)) {
                                    data.doorCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        var box = villager.getBoundingBox();
        data.villagerCount = level.getEntitiesOfClass(Villager.class, box.inflate(radius, 4.0D, radius)).size();
        data.golemCount = level.getEntitiesOfClass(IronGolem.class, box.inflate(16.0D, 8.0D, 16.0D)).size();

        return data;
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
