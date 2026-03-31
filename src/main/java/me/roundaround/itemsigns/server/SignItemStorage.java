package me.roundaround.itemsigns.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SignItemStorage extends SavedData {
  public static final Codec<SignItemStorage> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
      Entry.CODEC).fieldOf("Signs").forGetter(SignItemStorage::getEntries)).apply(instance, SignItemStorage::new));
  public static final SavedDataType<SignItemStorage> STATE_TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(Constants.MOD_ID,
          "sign_item"
  ), SignItemStorage::new, CODEC, null
  );

  private final HashMap<ChunkPos, HashMap<BlockPos, SignItemsAttachment>> attachments = new HashMap<>();

  private SignItemStorage() {
    this.setDirty();
  }

  private SignItemStorage(List<Entry> entries) {
    for (Entry entry : entries) {
      this.put(entry.blockPos(), entry.attachment());
    }
  }

  public SignItemsAttachment get(BlockPos blockPos) {
    return this.get(ChunkPos.containing(blockPos)).map(chunk -> chunk.get(blockPos)).orElse(null);
  }

  public void set(BlockPos blockPos, @Nullable SignItemsAttachment attachment) {
    if (attachment == null || attachment.isEmpty()) {
      this.remove(blockPos);
      return;
    }

    this.put(blockPos, attachment);
    this.setDirty();
  }

  public void remove(BlockPos blockPos) {
    this.get(ChunkPos.containing(blockPos)).ifPresent((chunk) -> {
      if (chunk.remove(blockPos) != null) {
        this.setDirty();
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
    this.attachments.computeIfAbsent(ChunkPos.containing(blockPos), _ -> new HashMap<>()).put(blockPos, attachment);
  }

  private List<Entry> getEntries() {
    return this.attachments.values()
        .stream()
        .flatMap((chunk) -> chunk.entrySet().stream())
        .map((entry) -> new Entry(entry.getKey(), entry.getValue()))
        .toList();
  }

  public static SignItemStorage getInstance(ServerLevel world) {
    return world.getDataStorage().computeIfAbsent(STATE_TYPE);
  }

  private record Entry(BlockPos blockPos, SignItemsAttachment attachment) {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        BlockPos.CODEC.fieldOf("BlockPos").forGetter(Entry::blockPos),
        SignItemsAttachment.CODEC.fieldOf("Attachment").forGetter(Entry::attachment)
    ).apply(instance, Entry::new));
  }
}
