package dev.gembox.swagcraft.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

public class SwagVillagerBreedGoal extends Goal {
    private final Villager villager;
    private Villager mate;
    private final Level level;
    private int mateTime;

    public SwagVillagerBreedGoal(Villager villager) {
        this.villager = villager;
        this.level = villager.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.villager.getAge() != 0) return false;

        if (this.villager.getRandom().nextInt(500) != 0) return false;

        if (!this.villager.canBreed()) return false;

        List<Villager> list = this.level.getEntitiesOfClass(Villager.class, this.villager.getBoundingBox().inflate(8.0D, 3.0D, 8.0D));
        Villager mate = null;

        for (Villager other : list) {
            if (other != this.villager && other.getAge() == 0 && other.canBreed()) {
                mate = other;
                break;
            }
        }

        if (mate == null) {
            return false;
        } else {
            this.mate = mate;
            return true;
        }
    }

    @Override
    public void start() {
        this.mateTime = 300;
        this.villager.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.mateTime >= 0 && this.mate != null && this.mate.isAlive() && this.villager.canBreed() && this.mate.canBreed();
    }

    @Override
    public void tick() {
        this.mateTime--;
        this.villager.getLookControl().setLookAt(this.mate, 10.0F, 30.0F);

        if (this.villager.distanceToSqr(this.mate) > 4.0D) {
            this.villager.getNavigation().moveTo(this.mate, 0.5D);
        } else {
            this.villager.getNavigation().stop();
            if (this.mateTime % 50 == 0) {
                this.level.broadcastEntityEvent(this.villager, (byte) 18); // hearts
            }
            if (this.mateTime <= 0) {
                this.breed();
            }
        }
    }

    private void breed() {
        if (!this.level.isClientSide() && this.level instanceof ServerLevel serverLevel) {
            Villager baby = this.villager.getBreedOffspring(serverLevel, this.mate);
            if (baby != null) {
                this.villager.setAge(6000);
                this.mate.setAge(6000);

                baby.setAge(-24000);
                baby.snapTo(this.villager.getX(), this.villager.getY(), this.villager.getZ(), 0.0F, 0.0F);
                serverLevel.addFreshEntityWithPassengers(baby);

                this.level.broadcastEntityEvent(this.villager, (byte) 18); // hearts
            }
        }
    }
}
