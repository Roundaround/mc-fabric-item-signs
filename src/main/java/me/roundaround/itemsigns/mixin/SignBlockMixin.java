package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin {
  @WrapMethod(method = "useItemOn")
  private InteractionResult preOnUseWithItem(
      ItemStack stack,
      BlockState state,
      Level world,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit,
      Operation<InteractionResult> original
  ) {
    Supplier<InteractionResult> callOriginal = () -> original.call(stack, state, world, pos, player, hand, hit);

    if (!(world.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity)) {
      return callOriginal.get();
    }

    // Should only get here if player is standing OR both hands are empty.

    SignText signText = signBlockEntity.getText(signBlockEntity.isFacingFrontText(player));
    if (signText.hasMessage(player)) {
      // If the sign currently has text, fall back to vanilla behavior.
      return callOriginal.get();
    }

    boolean canModify = player.mayBuild();
    boolean waxed = signBlockEntity.isWaxed();

    if (canModify && !waxed && stack.getItem() instanceof SignApplicator) {
      // If the item is a "sign changing item", try running the vanilla behavior first. If it is unsuccessful in
      // modifying the block, then we step in and try to mount the item instead. That is to say, if the result is a
      // SUCCESS or CONSUME, just keep that result as-is.
      InteractionResult vanillaResult = callOriginal.get();
      if (!(vanillaResult instanceof InteractionResult.TryEmptyHandInteraction)) {
        return vanillaResult;
      }
    }

    if (signBlockEntity.itemsigns$hasItemFacingPlayer(player)) {
      // If there is an item on the sign already, try to remove it.

      if (world.isClientSide()) {
        return canModify || waxed ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      }

      if (canModify && !waxed) {
        signBlockEntity.itemsigns$dropItemFacingPlayer(world, player);
        return InteractionResult.SUCCESS;
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
      return InteractionResult.SUCCESS;
    }

    return InteractionResult.TRY_WITH_EMPTY_HAND;
  }
}
