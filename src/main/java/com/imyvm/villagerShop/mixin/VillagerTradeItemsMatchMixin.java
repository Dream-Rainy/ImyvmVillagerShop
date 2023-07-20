package com.imyvm.villagerShop.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(TradeOffer.class)
public abstract class VillagerTradeItemsMatchMixin {
    @Shadow public abstract ItemStack getAdjustedFirstBuyItem();

    @Shadow @Final private ItemStack firstBuyItem;

    @Inject(method = "depleteBuyItems", at = @At("HEAD"), cancellable = true)
    private void depleteBuyItems(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (Objects.requireNonNull(this.getAdjustedFirstBuyItem().getNbt()).contains("imyvmCurrency")) {
            cir.setReturnValue(true);
        }
    }
}
