package me.roundaround.itemsigns.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SignItemStorage extends PersistentState {
  public static final Codec<SignItemStorage> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
      Entry.CODEC).fieldOf("Signs").forGetter(SignItemStorage::getEntries)).apply(instance, SignItemStorage::new));

  private final HashMap<ChunkPos, HashMap<BlockPos, SignItemsAttachment>> attachments = new HashMap<>();

  private SignItemStorage() {
    this.markDirty();
  }

  private SignItemStorage(List<Entry> entries) {
    for (Entry entry : entries) {
      this.put(entry.blockPos(), entry.attachment());
    }
  }

  @Override
  public NbtCompound writeNbt(NbtCompound rootNbt, RegistryWrapper.WrapperLookup registryLookup) {
    return (NbtCompound) CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this).getOrThrow();
  }

  public SignItemsAttachment get(BlockPos blockPos) {
    return this.get(new ChunkPos(blockPos)).map((chunk) -> chunk.get(blockPos)).orElse(null);
  }

  public void set(BlockPos blockPos, @Nullable SignItemsAttachment attachment) {
    if (attachment == null || attachment.isEmpty()) {
      this.remove(blockPos);
      return;
    }

    this.put(blockPos, attachment);
    this.markDirty();
  }

  public void remove(BlockPos blockPos) {
    this.get(new ChunkPos(blockPos)).ifPresent((chunk) -> {
      if (chunk.remove(blockPos) != null) {
        this.markDirty();
      }
    });
  }

  public HashMap<BlockPos, SignItemsAttachment> allInChunk(ChunkPos pos) {
    return this.get(pos).map(HashMap::new).orElseGet(HashMap::new);
  }

  private Optional<HashMap<BlockPos, SignItemsAttachment>> get(ChunkPos pos) {
    return Optional.ofNullable(this.attachments.get(pos));
  }

  private void put(BlockPos blockPos, SignItemsAttachment attachment) {
    this.attachments.computeIfAbsent(new ChunkPos(blockPos), (chunkPos) -> new HashMap<>()).put(blockPos, attachment);
  }

  private List<Entry> getEntries() {
    return this.attachments.values()
        .stream()
        .flatMap((chunk) -> chunk.entrySet().stream())
        .map((entry) -> new Entry(entry.getKey(), entry.getValue()))
        .toList();
  }

  public static SignItemStorage getInstance(ServerWorld world) {
    Type<SignItemStorage> persistentStateType = new PersistentState.Type<>(
        SignItemStorage::new,
        (nbt, registryLookup) -> CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), nbt).getOrThrow(),
        null
    );
    return world.getPersistentStateManager().getOrCreate(persistentStateType, Constants.MOD_ID);
  }

  private record Entry(BlockPos blockPos, SignItemsAttachment attachment) {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        BlockPos.CODEC.fieldOf(
            "BlockPos").forGetter(Entry::blockPos),
        SignItemsAttachment.CODEC.fieldOf("Attachment").forGetter(Entry::attachment)
    ).apply(instance, Entry::new));
  }
}
