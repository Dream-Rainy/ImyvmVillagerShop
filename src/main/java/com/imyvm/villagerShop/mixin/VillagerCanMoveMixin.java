package com.imyvm.villagerShop.mixin;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.imyvm.villagerShop.VillagerShopMain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import java.util.List;
@Mixin(VillagerEntity.class)
public class VillagerCanMoveMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        Box searchBox = villager.getBoundingBox().expand(10);
        List<PlayerEntity> nearbyPlayers = villager.world.getEntitiesByClass(PlayerEntity.class, searchBox, player -> true);

        if (villager.getScoreboardTags().contains("VillagerShop")&& !nearbyPlayers.isEmpty()) {
            // 阻止村民移动
            villager.setVelocity(0, villager.getVelocity().y, 0);

            // 允许村民继续看向玩家
            PlayerEntity targetPlayer = nearbyPlayers.get(0);
            Vec3d targetPlayerPosition = targetPlayer.getPos().add(0, targetPlayer.getEyeHeight(targetPlayer.getPose()), 0);
            villager.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPlayerPosition);
        }
        // 取消后续方法调用
        ci.cancel();
    }
}


