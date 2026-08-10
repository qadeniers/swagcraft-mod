package dev.gembox.swagcraft.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Unique private static final int REPAIR_INTERVAL_TICKS = 10 * 20;
    @Unique private static final int REPAIR_AMOUNT = 1;

    @Unique private static final EquipmentSlot[] REPAIRABLE_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    @Unique private static ItemStack IRON_INGOT_STACK;

    @Unique
    private static ItemStack getIronIngotStack() {
        if (IRON_INGOT_STACK == null) {
            IRON_INGOT_STACK = new ItemStack(Items.IRON_INGOT);
        }
        return IRON_INGOT_STACK;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void repairIronEquipment(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide() || (player.tickCount % REPAIR_INTERVAL_TICKS) != 0) return;

        for (EquipmentSlot slot : REPAIRABLE_SLOTS) {
            repairIfIron(player.getItemBySlot(slot));
        }
    }

    @Unique
    private static void repairIfIron(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamaged()) return;

        if (isIron(stack)) {
            int currentDamage = stack.getDamageValue();
            stack.setDamageValue(Math.max(0, currentDamage - REPAIR_AMOUNT));
        }
    }

    @Unique
    private static boolean isIron(ItemStack stack) {
        Repairable repairable = stack.get(DataComponents.REPAIRABLE);
        if (repairable == null) {
            return false;
        }


        for (Holder<Item> holder : repairable.items()) {
            if (holder.value() == Items.IRON_INGOT) {
                return true;
            }
        }

        return false;
    }
}
