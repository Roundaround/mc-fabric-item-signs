package me.roundaround.itemsigns.server;

import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;

public class SignItemStorage extends PersistentState {
  private static final String NBT_SIGNS = "Signs";
  private static final String NBT_BLOCK_POS = "BlockPos";
  private static final String NBT_ATTACHMENT = "Attachment";

  private final HashMap<BlockPos, SignItemsAttachment> attachments = new HashMap<>();

  private SignItemStorage() {
  }

  @Override
  public NbtCompound writeNbt(NbtCompound rootNbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList nbtList = new NbtList();
    this.attachments.forEach((blockPos, attachment) -> {
      NbtCompound signNbt = new NbtCompound();
      signNbt.put(NBT_BLOCK_POS, NbtHelper.fromBlockPos(blockPos));
      signNbt.put(NBT_ATTACHMENT, attachment.encode(registryLookup));
      nbtList.add(signNbt);
    });
    rootNbt.put(NBT_SIGNS, nbtList);

    return rootNbt;
  }

  public SignItemsAttachment get(BlockPos blockPos) {
    return this.attachments.get(blockPos);
  }

  public void set(BlockPos blockPos, SignItemsAttachment attachment) {
    if (attachment.isEmpty()) {
      this.remove(blockPos);
      return;
    }

    this.attachments.put(blockPos, attachment);
    this.markDirty();
  }

  public void remove(BlockPos blockPos) {
    this.attachments.remove(blockPos);
    this.markDirty();
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
      SignItemsAttachment attachment = SignItemsAttachment.decode(signNbt.get(NBT_ATTACHMENT), registryLookup);
      manager.attachments.put(blockPos, attachment);
    }

    return manager;
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
