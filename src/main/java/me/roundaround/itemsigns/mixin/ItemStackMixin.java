package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
  @WrapOperation(
      method = "useOnBlock", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/item/Item;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)" +
               "Lnet/minecraft/util/ActionResult;"
  )
  )
  private ActionResult wrapItemUseOnBlock(Item item, ItemUsageContext context, Operation<ActionResult> original) {
    Supplier<ActionResult> callOriginal = () -> original.call(item, context);

    World world = context.getWorld();
    BlockEntity blockEntity = world.getBlockEntity(context.getBlockPos());
    PlayerEntity player = context.getPlayer();

    if (!(blockEntity instanceof SignBlockEntity signBlockEntity) || player == null) {
      return callOriginal.get();
    }

    // If we ever get here, it means the player is holding an item and sneaking.

    if (signBlockEntity.getText(signBlockEntity.isPlayerFacingFront(player)).hasText(player)) {
      // If the sign currently has text, fall back to vanilla behavior.
      return callOriginal.get();
    }

    // Try to allow vanilla to handle the item use first. If vanilla returns back PASS (as in it did nothing), we
    // then can try to place/remove the sign's item.
    ActionResult vanillaResult = callOriginal.get();
    if (!(vanillaResult instanceof ActionResult.Pass)) {
      return vanillaResult;
    }

    boolean canModify = player.canModifyBlocks();
    boolean waxed = signBlockEntity.isWaxed();

    if (world.isClient) {
      return canModify || waxed ? ActionResult.SUCCESS : ActionResult.CONSUME;
    }

    if (!canModify || waxed) {
      return callOriginal.get();
    }

    if (signBlockEntity.itemsigns$hasItemFacingPlayer(player)) {
      signBlockEntity.itemsigns$dropItemFacingPlayer(world, player);
      return ActionResult.SUCCESS;
    }

    if (signBlockEntity.itemsigns$placeItemFacingPlayer(world, player, context.getStack())) {
      return ActionResult.SUCCESS;
    }

    return callOriginal.get();
  }
}
