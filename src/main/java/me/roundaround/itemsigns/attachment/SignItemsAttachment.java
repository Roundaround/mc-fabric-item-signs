package me.roundaround.itemsigns.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.roundaround.itemsigns.generated.Constants;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class SignItemsAttachment {
  public static final String NBT_KEY = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "items").toString();
  public static final Codec<SignItemsAttachment> CODEC =
      RecordCodecBuilder.create((instance) -> instance.group(Codec.list(
          ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter((inst) -> inst.items))
      .apply(instance, SignItemsAttachment::new));
  public static final StreamCodec<ByteBuf, SignItemsAttachment> PACKET_CODEC = ByteBufCodecs.fromCodec(CODEC);
  public static final SignItemsAttachment DEFAULT = new SignItemsAttachment();

  private final NonNullList<ItemStack> items;

  private SignItemsAttachment() {
    this(createEmptyList());
  }

  private SignItemsAttachment(List<ItemStack> items) {
    this.items = copyFromList(items);
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

  public NonNullList<ItemStack> getAll() {
    return copyFromList(this.items);
  }

  public SignItemsAttachment editAsList(Consumer<NonNullList<ItemStack>> editor) {
    NonNullList<ItemStack> list = this.getAll();
    editor.accept(list);
    return new SignItemsAttachment(list);
  }

  public static NonNullList<ItemStack> createEmptyList() {
    return NonNullList.withSize(2, ItemStack.EMPTY);
  }

  private static NonNullList<ItemStack> copyFromList(List<ItemStack> source) {
    NonNullList<ItemStack> dest = createEmptyList();
    for (int i = 0; i < Math.min(source.size(), 2); i++) {
      dest.set(i, source.get(i).copy());
    }
    return dest;
  }
}
