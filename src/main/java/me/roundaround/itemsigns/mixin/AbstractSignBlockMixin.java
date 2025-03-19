package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignBlock.class)
public abstract class AbstractSignBlockMixin {
  @Shadow
  protected abstract boolean isOtherPlayerEditing(PlayerEntity player, SignBlockEntity blockEntity);

  @ModifyExpressionValue(
      method = "onUseWithItem", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/item/SignChangingItem;canUseOnSignText(Lnet/minecraft/block/entity/SignText;" +
               "Lnet/minecraft/entity/player/PlayerEntity;)Z"
  )
  )
  private boolean wrapCanUseOnSignText(boolean original, @Local(argsOnly = true) PlayerEntity player) {
    return original && !player.shouldCancelInteraction();
  }

  @ModifyReturnValue(method = "onUseWithItem", at = @At("RETURN"))
  private ActionResult afterVanillaUseItemAttempt(
      ActionResult original,
      @Local(argsOnly = true) ItemStack stack,
      @Local(argsOnly = true) BlockPos pos,
      @Local(argsOnly = true) World world,
      @Local(argsOnly = true) PlayerEntity player
  ) {
    if (!original.equals(ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION) || stack.isEmpty()) {
      return original;
    }

    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (!(blockEntity instanceof SignBlockEntity signBlockEntity)) {
      return original;
    }

    if (world instanceof ServerWorld serverWorld && signBlockEntity.itemsigns$placeItem(serverWorld, player, stack)) {
      return ActionResult.SUCCESS_SERVER;
    }

    return original;
  }

  @Inject(
      method = "onUse", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/AbstractSignBlock;isOtherPlayerEditing" +
               "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/block/entity/SignBlockEntity;)Z"
  ), cancellable = true
  )
  private void beforeCheckingTextEditable(
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      BlockHitResult hit,
      CallbackInfoReturnable<ActionResult> cir,
      @Local SignBlockEntity blockEntity
  ) {
    if (!this.isOtherPlayerEditing(player, blockEntity) && player.canModifyBlocks() &&
        blockEntity.itemsigns$hasItem(player)) {
      blockEntity.itemsigns$dropItem(world, player);
      cir.setReturnValue(ActionResult.SUCCESS_SERVER);
    }
  }
}
