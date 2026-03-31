package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @WrapOperation(
      method = "useOn", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"
  )
  )
  private InteractionResult wrapItemUseOnBlock(Item item, UseOnContext context, Operation<InteractionResult> original) {
    Supplier<InteractionResult> callOriginal = () -> original.call(item, context);

    Level world = context.getLevel();
    BlockEntity blockEntity = world.getBlockEntity(context.getClickedPos());
    Player player = context.getPlayer();

    if (!(blockEntity instanceof SignBlockEntity signBlockEntity) || player == null) {
      return callOriginal.get();
    }

    // If we ever get here, it means the player is holding an item and sneaking.

    if (signBlockEntity.getText(signBlockEntity.isFacingFrontText(player)).hasMessage(player)) {
      // If the sign currently has text, fall back to vanilla behavior.
      return callOriginal.get();
    }

    // Try to allow vanilla to handle the item use first. If vanilla returns back PASS (as in it did nothing), we
    // then can try to place/remove the sign's item.
    InteractionResult vanillaResult = callOriginal.get();
    if (!(vanillaResult instanceof InteractionResult.Pass)) {
      return vanillaResult;
    }

    boolean canModify = player.mayBuild();
    boolean waxed = signBlockEntity.isWaxed();

    if (world.isClientSide()) {
      return canModify || waxed ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    if (!canModify || waxed) {
      return callOriginal.get();
    }

    if (signBlockEntity.itemsigns$hasItemFacingPlayer(player)) {
      signBlockEntity.itemsigns$dropItemFacingPlayer(world, player);
      return InteractionResult.SUCCESS;
    }

    if (signBlockEntity.itemsigns$placeItemFacingPlayer(world, player, context.getItemInHand())) {
      return InteractionResult.SUCCESS;
    }

    return callOriginal.get();
  }
}
