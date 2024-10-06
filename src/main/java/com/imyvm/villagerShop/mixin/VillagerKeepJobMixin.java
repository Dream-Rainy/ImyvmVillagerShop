package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.ai.brain.task.TakeJobSiteTask;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TakeJobSiteTask.class)
public class VillagerKeepJobMixin {
    @Inject(method = "canReachJobSite", at = @At("HEAD"), cancellable = true)
    private static void keepJob(PathAwareEntity entity, BlockPos pos, PointOfInterestType poiType, CallbackInfoReturnable<Boolean> cir) {
        if (entity.getCommandTags().contains("VillagerShop")) {
            cir.setReturnValue(false);
        }
    }
}
