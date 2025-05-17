package me.roundaround.itemsigns.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class LoadFromNbtEvents {
  public static Event<BlockEntityLoad> BLOCK_ENTITY = EventFactory.createArrayBacked(
      BlockEntityLoad.class, (callbacks) -> (nbt, world, pos, state, registries) -> {
        BlockState workingState = state;
        for (BlockEntityLoad callback : callbacks) {
          workingState = callback.beforeBlockEntityLoaded(nbt, world, pos, workingState, registries);
        }
        return workingState;
      }
  );

  @FunctionalInterface
  public interface BlockEntityLoad {
    BlockState beforeBlockEntityLoaded(
        NbtCompound nbt,
        ServerWorld world,
        BlockPos pos,
        BlockState state,
        RegistryWrapper.WrapperLookup registries
    );
  }
}
