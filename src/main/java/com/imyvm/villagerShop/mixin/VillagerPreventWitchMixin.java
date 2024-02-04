package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public class VillagerPreventWitchMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void preventWitchTransformation(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (villager.getCommandTags().contains("VillagerShop")) {
            ci.cancel();
        }
    }
}
