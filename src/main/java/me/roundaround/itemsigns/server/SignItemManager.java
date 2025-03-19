package me.roundaround.itemsigns.server;

import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;

public class SignItemManager extends PersistentState {
  private static final String NBT_SIGNS = "Signs";
  private static final String NBT_X = "X";
  private static final String NBT_Y = "Y";
  private static final String NBT_Z = "Z";
  private static final String NBT_STACK = "ItemStack";

  private final ServerWorld world;
  private final HashMap<BlockPos, ItemStack> signs = new HashMap<>();

  private SignItemManager(ServerWorld world) {
    this.world = world;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList nbtList = new NbtList();
    this.signs.forEach((blockPos, stack) -> {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putInt(NBT_X, blockPos.getX());
      nbtCompound.putInt(NBT_Y, blockPos.getY());
      nbtCompound.putInt(NBT_Z, blockPos.getZ());
      nbtCompound.put(NBT_STACK, stack.toNbt(registryLookup));
      nbtList.add(nbtCompound);
    });
    nbt.put(NBT_SIGNS, nbtList);

    return nbt;
  }

  private static SignItemManager fromNbt(ServerWorld world, NbtCompound nbt) {
    SignItemManager manager = new SignItemManager(world);



    return manager;
  }

  public static SignItemManager getInstance(ServerWorld world) {
    Type<SignItemManager> persistentStateType = new PersistentState.Type<>(() -> new SignItemManager(world),
        (nbt, registryLookup) -> fromNbt(world, nbt), null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, Constants.MOD_ID);
  }
}
