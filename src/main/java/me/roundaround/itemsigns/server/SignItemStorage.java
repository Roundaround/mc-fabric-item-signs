package me.roundaround.itemsigns.server;

import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Optional;

public class SignItemStorage extends PersistentState {
  private static final String NBT_SIGNS = "Signs";
  private static final String NBT_BLOCK_POS = "BlockPos";

  private final HashMap<BlockPos, DefaultedList<ItemStack>> signs = new HashMap<>();

  private SignItemStorage() {
  }

  @Override
  public NbtCompound writeNbt(NbtCompound rootNbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList nbtList = new NbtList();
    this.signs.forEach((blockPos, stacks) -> {
      NbtCompound signNbt = new NbtCompound();
      signNbt.put(NBT_BLOCK_POS, NbtHelper.fromBlockPos(blockPos));
      Inventories.writeNbt(signNbt, stacks, true, registryLookup);
      nbtList.add(signNbt);
    });
    rootNbt.put(NBT_SIGNS, nbtList);

    return rootNbt;
  }

  public void write(BlockPos blockPos, DefaultedList<ItemStack> stacks) {
    DefaultedList<ItemStack> stored = this.signs.computeIfAbsent(blockPos, (key) -> emptyStacks());
    for (int i = 0; i < stored.size(); i++) {
      stored.set(i, stacks.get(i).copy());
    }
    this.markDirty();
  }

  public void clear(BlockPos blockPos) {
    this.signs.remove(blockPos);
    this.markDirty();
  }

  public void read(BlockPos blockPos, DefaultedList<ItemStack> stacks) {
    if (!this.signs.containsKey(blockPos)) {
      return;
    }

    DefaultedList<ItemStack> stored = this.signs.get(blockPos);
    for (int i = 0; i < stored.size(); i++) {
      stacks.set(i, stored.get(i).copy());
    }
  }

  private static DefaultedList<ItemStack> emptyStacks() {
    return DefaultedList.ofSize(2, ItemStack.EMPTY);
  }

  private static SignItemStorage fromNbt(NbtCompound nbtRoot, RegistryWrapper.WrapperLookup registryLookup) {
    SignItemStorage manager = new SignItemStorage();

    if (!nbtRoot.contains(NBT_SIGNS)) {
      return manager;
    }

    NbtList nbtList = nbtRoot.getList(NBT_SIGNS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < nbtList.size(); i++) {
      NbtCompound signNbt = nbtList.getCompound(i);
      BlockPos blockPos = NbtHelper.toBlockPos(signNbt, NBT_BLOCK_POS).orElseThrow();
      DefaultedList<ItemStack> stacks = emptyStacks();
      Inventories.readNbt(signNbt, stacks, registryLookup);

      manager.signs.put(blockPos, stacks);
    }

    return manager;
  }

  public static Optional<SignItemStorage> getInstance(BlockEntity blockEntity) {
    World world = blockEntity.getWorld();
    if (!(blockEntity instanceof SignBlockEntity) || !(world instanceof ServerWorld serverWorld)) {
      return Optional.empty();
    }
    return Optional.of(getInstance(serverWorld));
  }

  public static SignItemStorage getInstance(ServerWorld world) {
    Type<SignItemStorage> persistentStateType = new PersistentState.Type<>(
        SignItemStorage::new,
        (nbt, registryLookup) -> fromNbt(nbt, world.getRegistryManager()),
        null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, Constants.MOD_ID);
  }
}
