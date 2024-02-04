package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class VehicleAddMixin {
    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void canAdd(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        if (passenger.getCommandTags().contains("VillagerShop")) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addPassenger", at = @At("HEAD"), cancellable = true)
    private void addPassenger(Entity passenger, CallbackInfo ci) {
        if (passenger.getCommandTags().contains("VillagerShop")) {
            ci.cancel();
        }
    }
}
