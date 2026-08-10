package dev.gembox.swagcraft.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Shadow public abstract void shoot(double x, double y, double z, float velocity, float inaccuracy);

    @Inject(method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("HEAD"), cancellable = true)
    private void bowBoost(Entity shooter, float pitch, float yaw, float roll, float speed, float divergence, CallbackInfo ci) {
        if (shooter instanceof Player player) {
            boolean wearingElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);

            if (!wearingElytra) return;

            ItemStack bow = player.getUseItem();
            if (!bow.is(Items.BOW)) {
                bow = player.getMainHandItem().is(Items.BOW) ? player.getMainHandItem() : player.getOffhandItem();
            }

            boolean hasPunch = isPunchBow(player, bow);
            boolean hasPower = isPowerBow(player, bow);

            if (hasPower || !hasPunch) return;

            float f = -(float)Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
            float g = -(float)Math.sin(Math.toRadians(pitch + roll));
            float h = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));

            this.shoot(f, g, h, speed, divergence);

            ci.cancel();
        }
    }

    @Unique
    private boolean isPunchBow(Player player, ItemStack stack) {
        if (!stack.is(Items.BOW)) return false;

        return player.level().registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(Enchantments.PUNCH))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }

    @Unique
    private boolean isPowerBow(Player player, ItemStack stack) {
        if (!stack.is(Items.BOW)) return false;

        return player.level().registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(reg -> reg.get(Enchantments.POWER))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
                .orElse(false);
    }
}
