package me.roundaround.itemsigns.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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
        CompoundTag nbt,
        ServerLevel world,
        BlockPos pos,
        BlockState state,
        HolderLookup.Provider registries
    );
  }
}
