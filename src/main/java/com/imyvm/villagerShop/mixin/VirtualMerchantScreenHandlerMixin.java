package com.imyvm.villagerShop.mixin;

import com.imyvm.economy.EconomyMod;
import com.imyvm.villagerShop.apis.Translator;
import eu.pb4.sgui.api.gui.MerchantGui;
import eu.pb4.sgui.virtual.merchant.VirtualMerchant;
import eu.pb4.sgui.virtual.merchant.VirtualMerchantScreenHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.village.MerchantInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(VirtualMerchantScreenHandler.class)
public abstract class VirtualMerchantScreenHandlerMixin {
    @Shadow @Final private MerchantInventory merchantInventory;

    @Shadow(remap = false) public abstract MerchantGui getGui();

    @Shadow(remap = false) @Final private VirtualMerchant merchant;

    @Inject(method = "selectNewTrade", at = @At("HEAD"), cancellable = true, remap = false)
    private void selectNewTrade(int tradeIndex, CallbackInfo ci) {
        this.merchantInventory.setOfferIndex(tradeIndex);
        this.getGui().onSelectTrade(this.merchant.getOffers().get(tradeIndex));
        if (this.merchantInventory.getStack(0).getItem() == Items.PAPER) {
            this.merchantInventory.removeStack(0);
            if (this.merchantInventory.getStack(1).getItem() == Items.BARRIER) {
                this.merchantInventory.removeStack(1);
            }
        }

        if (this.merchant.getOffers().size() > tradeIndex && this.merchant.getOffers().get(tradeIndex).getAdjustedFirstBuyItem().getItem() == Items.PAPER) {
            var imyvmCurry = this.merchant.getOffers().get(tradeIndex).getAdjustedFirstBuyItem();
            this.merchantInventory.setStack(0, imyvmCurry);
            var barrierItem = Registries.ITEM.getOrEmpty(Identifier.tryParse("minecraft:barrier")).orElseThrow();
            var barrierItemStack = barrierItem.getDefaultStack();
            barrierItemStack.setCustomName(Translator.INSTANCE.tr("shop.buy.failed.lack"));
            barrierItemStack.addEnchantment(Enchantments.MENDING, 1);
            if (imyvmCurry.getNbt() != null) {
                var moneyShouldTakeString = imyvmCurry.getName().getString();
                var pattern = Pattern.compile("[0-9]+\\.?[0-9]*");
                var matcher = pattern.matcher(moneyShouldTakeString);
                if (matcher.find()) {
                    var moneyShouldTake = Double.parseDouble(matcher.group());
                    var player = this.getGui().getPlayer();
                    var playerBalance = EconomyMod.data.getOrCreate(player).getMoney();
                    if (playerBalance < moneyShouldTake * 100) {
                        this.merchantInventory.setStack(1, barrierItemStack);
                    }
                } else {
                    this.merchantInventory.setStack(1, barrierItemStack);
                }
            }
            ci.cancel();
        }
    }

    @Inject(method = "onClosed", at = @At(value = "INVOKE", target = "net/minecraft/entity/player/PlayerEntity.getWorld ()Lnet/minecraft/world/World;"), cancellable = true)
    private void onClosed(PlayerEntity playerEntity, CallbackInfo ci) {
        if (!playerEntity.getWorld().isClient && playerEntity instanceof ServerPlayerEntity) {
            if (this.merchantInventory.getStack(0).getItem() == Items.PAPER) {
                this.merchantInventory.removeStack(0);
                this.merchantInventory.removeStack(1);
                this.merchantInventory.removeStack(2);
                ci.cancel();
            }
        }
    }

}
