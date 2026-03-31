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
      BlockEntityLoad.class, (callbacks) -> (nbt, level, pos, state, registries) -> {
        BlockState workingState = state;
        for (BlockEntityLoad callback : callbacks) {
          workingState = callback.beforeBlockEntityLoaded(nbt, level, pos, workingState, registries);
        }
        return workingState;
      }
  );

  @FunctionalInterface
  public interface BlockEntityLoad {
    BlockState beforeBlockEntityLoaded(
        CompoundTag nbt,
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        HolderLookup.Provider registries
    );
  }
}
