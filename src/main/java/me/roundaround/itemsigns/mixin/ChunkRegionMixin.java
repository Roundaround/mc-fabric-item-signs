package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.roundaround.itemsigns.event.LoadFromNbtEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin {
  @Shadow
  @Final
  private ServerWorld world;

  @WrapOperation(
      method = "getBlockEntity", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/block/entity/BlockEntity;createFromNbt(Lnet/minecraft/util/math/BlockPos;" +
               "Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/NbtCompound;" +
               "Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/block/entity/BlockEntity;"
  )
  )
  private BlockEntity spliceItemsIntoNbt(
      BlockPos pos,
      BlockState state,
      NbtCompound nbt,
      RegistryWrapper.WrapperLookup registries,
      Operation<BlockEntity> original
  ) {
    BlockState adjustedState = LoadFromNbtEvents.BLOCK_ENTITY.invoker()
        .beforeBlockEntityLoaded(nbt, this.world, pos, state, registries);
    return original.call(pos, adjustedState, nbt, registries);
  }
}
