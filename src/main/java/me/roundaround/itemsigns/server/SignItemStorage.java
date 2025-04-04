package me.roundaround.itemsigns.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.List;

public class SignItemStorage extends PersistentState {
  public static final Codec<SignItemStorage> CODEC = RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
      Entry.CODEC).fieldOf("Signs").forGetter(SignItemStorage::getEntries)).apply(instance, SignItemStorage::new));
  public static final PersistentStateType<SignItemStorage> STATE_TYPE = new PersistentStateType<>(
      Constants.MOD_ID,
      SignItemStorage::new,
      CODEC,
      null
  );

  private final HashMap<BlockPos, SignItemsAttachment> attachments = new HashMap<>();

  private SignItemStorage() {
    this.markDirty();
  }

  private SignItemStorage(List<Entry> entries) {
    for (Entry entry : entries) {
      this.attachments.put(entry.blockPos(), entry.attachment());
    }
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
    if (this.attachments.remove(blockPos) != null) {
      this.markDirty();
    }
  }

  private List<Entry> getEntries() {
    return this.attachments.entrySet().stream().map((entry) -> new Entry(entry.getKey(), entry.getValue())).toList();
  }

  public static SignItemStorage getInstance(ServerWorld world) {
    return world.getPersistentStateManager().getOrCreate(STATE_TYPE);
  }

  private record Entry(BlockPos blockPos, SignItemsAttachment attachment) {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
        BlockPos.CODEC.fieldOf(
            "BlockPos").forGetter(Entry::blockPos),
        SignItemsAttachment.CODEC.fieldOf("Attachment").forGetter(Entry::attachment)
    ).apply(instance, Entry::new));
  }
}
