package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.ai.brain.task.LoseJobOnSiteLossTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoseJobOnSiteLossTask.class)
public class VillagerKeepJobMixin {
    @Inject(method = "method_47038", at = @At("HEAD"), cancellable = true)
    private static void keepJob(ServerWorld world, VillagerEntity entity, long time, CallbackInfoReturnable<Boolean> cir) {
        if (entity.getCommandTags().contains("VillagerShop")) {
            cir.setReturnValue(false);
        }
    }
}
