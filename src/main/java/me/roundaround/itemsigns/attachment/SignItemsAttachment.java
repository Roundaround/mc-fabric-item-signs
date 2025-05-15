package me.roundaround.itemsigns.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;
import java.util.function.Consumer;

public class SignItemsAttachment {
  public static final Codec<SignItemsAttachment> CODEC =
      RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
          ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter((inst) -> inst.items))
      .apply(instance, SignItemsAttachment::new));
  public static final PacketCodec<ByteBuf, SignItemsAttachment> PACKET_CODEC = PacketCodecs.codec(CODEC);
  public static final SignItemsAttachment DEFAULT = new SignItemsAttachment();

  private final DefaultedList<ItemStack> items;

  private SignItemsAttachment() {
    this(createEmptyList());
  }

  private SignItemsAttachment(List<ItemStack> items) {
    this.items = copyFromList(items);
  }

  public NbtElement encode(RegistryWrapper.WrapperLookup registryLookup) {
    return CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this).getOrThrow();
  }

  public ItemStack get(int index) {
    return this.items.get(index);
  }

  public boolean hasItem(int index) {
    return !this.get(index).isEmpty();
  }

  public SignItemsAttachment set(int index, ItemStack stack) {
    return this.editAsList((list) -> list.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copy()));
  }

  public SignItemsAttachment clear() {
    return DEFAULT;
  }

  public boolean isEmpty() {
    return this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty);
  }

  public DefaultedList<ItemStack> getAll() {
    return copyFromList(this.items);
  }

  public SignItemsAttachment editAsList(Consumer<DefaultedList<ItemStack>> editor) {
    DefaultedList<ItemStack> list = this.getAll();
    editor.accept(list);
    return new SignItemsAttachment(list);
  }

  public static SignItemsAttachment decode(NbtElement nbt, RegistryWrapper.WrapperLookup registryLookup) {
    return SignItemsAttachment.CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), nbt).getOrThrow();
  }

  public static DefaultedList<ItemStack> createEmptyList() {
    return DefaultedList.ofSize(2, ItemStack.EMPTY);
  }

  private static DefaultedList<ItemStack> copyFromList(List<ItemStack> source) {
    DefaultedList<ItemStack> dest = createEmptyList();
    for (int i = 0; i < Math.min(source.size(), 2); i++) {
      dest.set(i, source.get(i).copy());
    }
    return dest;
  }
}
