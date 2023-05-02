package com.imyvm.villagerShop.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class NoCollisionMixin {

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void isCollidable(CallbackInfoReturnable<Boolean> callback) {
        // 获取实体的NBT数据
        Entity Entity = (Entity) (Object) this;

        // 判断实体的NBT数据中是否包含特定键值
        if (Entity.getScoreboardTags().contains("VillagerShop")) {
            callback.setReturnValue(false);
        }
    }
}