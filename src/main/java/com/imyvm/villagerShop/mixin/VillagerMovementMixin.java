package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class VillagerMovementMixin {

        @Redirect(method = "tickMovement", at = @At(value = "INVOKE",target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"))
        private void modifySetVelocity(LivingEntity entity, double x, double y, double z) {
                if (entity instanceof VillagerEntity && entity.getCommandTags().contains("VillagerShop")) {
                        entity.setVelocity(0.0, y, 0.0);
                } else {
                        entity.setVelocity(x, y, z);
                }
        }

        @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/Vec3d;)V"))
        private void modifyTravel(LivingEntity entity, Vec3d movement) {
                if (entity instanceof VillagerEntity && entity.getCommandTags().contains("VillagerShop")) {
                        entity.travel(new Vec3d(0.0, movement.y, 0.0));
                } else {
                        entity.travel(movement);
                }
        }
}



