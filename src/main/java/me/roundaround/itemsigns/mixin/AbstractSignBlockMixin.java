package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignChangingItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;

@Mixin(AbstractSignBlock.class)
public abstract class AbstractSignBlockMixin extends BlockWithEntity {
  protected AbstractSignBlockMixin(Settings settings) {
    super(settings);
  }

  @WrapMethod(method = "onUseWithItem")
  private ActionResult preOnUseWithItem(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      BlockHitResult hit,
      Operation<ActionResult> original
  ) {
    Supplier<ActionResult> callOriginal = () -> original.call(stack, state, world, pos, player, hand, hit);

    if (!(world.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity)) {
      return callOriginal.get();
    }

    // Should only get here if player is standing OR both hands are empty.

    SignText signText = signBlockEntity.getText(signBlockEntity.isPlayerFacingFront(player));
    if (signText.hasText(player)) {
      // If the sign currently has text, fall back to vanilla behavior.
      return callOriginal.get();
    }

    boolean canModify = player.canModifyBlocks();
    boolean waxed = signBlockEntity.isWaxed();

    if (canModify && !waxed && stack.getItem() instanceof SignChangingItem) {
      // If the item is a "sign changing item", try running the vanilla behavior first. If it is unsuccessful in
      // modifying the block, then we step in and try to mount the item instead. That is to say, if the result is a
      // SUCCESS or CONSUME, just keep that result as-is.
      ActionResult vanillaResult = callOriginal.get();
      if (!(vanillaResult instanceof ActionResult.PassToDefaultBlockAction)) {
        return vanillaResult;
      }
    }

    if (signBlockEntity.itemsigns$hasItemFacingPlayer(player)) {
      // If there is an item on the sign already, try to remove it.

      if (world.isClient) {
        return canModify || waxed ? ActionResult.SUCCESS : ActionResult.CONSUME;
      }

      if (canModify && !waxed) {
        signBlockEntity.itemsigns$dropItemFacingPlayer(world, player);
        return ActionResult.SUCCESS;
      }

      // If we fail to remove the existing item, simply fall back to vanilla behavior.
      return callOriginal.get();
    }

    if (stack.isEmpty()) {
      // If there is no item and the player's hand is empty, fall back to vanilla behavior.
      return callOriginal.get();
    }

    // If we've gotten here, that means the sign is empty and the player is standing and holding an item.

    if (canModify && !waxed && signBlockEntity.itemsigns$placeItemFacingPlayer(world, player, stack)) {
      return ActionResult.SUCCESS;
    }

    return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
  }

  @Override
  protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof SignBlockEntity signBlockEntity) {
        ItemScatterer.spawn(world, pos, signBlockEntity.itemsigns$getItems());
        SignItemStorage.getInstance((ServerWorld) world).remove(pos);
      }

      super.onStateReplaced(state, world, pos, newState, moved);
    }
  }
}
