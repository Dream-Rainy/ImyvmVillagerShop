package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public class VillagerSleepMixin {
    @Inject(method = "sleep", at = @At(value = "HEAD"), cancellable = true)
    private void canNotSleep(BlockPos pos, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (villager.getCommandTags().contains("VillagerShop")) {
            ci.cancel();
        }
    }
}
