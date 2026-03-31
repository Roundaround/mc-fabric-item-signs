package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.roundaround.itemsigns.event.LoadFromNbtEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
  @Shadow
  public abstract Level getLevel();

  @WrapOperation(
      method = "promotePendingBlockEntity", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadStatic(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
  )
  )
  private BlockEntity spliceItemsIntoNbt(
      BlockPos pos,
      BlockState state,
      CompoundTag nbt,
      HolderLookup.Provider registries,
      Operation<BlockEntity> original
  ) {
    BlockState adjustedState = LoadFromNbtEvents.BLOCK_ENTITY.invoker()
        .beforeBlockEntityLoaded(nbt, (ServerLevel) this.getLevel(), pos, state, registries);
    return original.call(pos, adjustedState, nbt, registries);
  }
}
