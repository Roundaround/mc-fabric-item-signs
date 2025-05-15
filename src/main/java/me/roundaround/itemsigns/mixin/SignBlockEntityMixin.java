package me.roundaround.itemsigns.mixin;

import me.roundaround.itemsigns.attachment.ItemSignsAttachmentTypes;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.block.entity.SignBlockEntityExtensions;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Function;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityExtensions {
  protected SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Shadow
  public abstract boolean isPlayerFacingFront(PlayerEntity player);

  @Shadow
  protected abstract void updateListeners();

  @Override
  public boolean itemsigns$placeItemFacingPlayer(World world, PlayerEntity player, ItemStack stack) {
    int index = this.itemsigns$getItemIndex(player);
    if (this.itemsigns$hasItem(index)) {
      return false;
    }
    this.itemsigns$setItemAndUpdate(world, index, stack.splitUnlessCreative(1, player));
    return true;
  }

  @Override
  public boolean itemsigns$hasItemFacingPlayer(PlayerEntity player) {
    return this.itemsigns$hasItem(this.isPlayerFacingFront(player));
  }

  @Override
  public void itemsigns$dropItemFacingPlayer(World world, PlayerEntity player) {
    int index = this.itemsigns$getItemIndex(player);
    if (!this.itemsigns$hasItem(index)) {
      return;
    }

    ItemStack stack = this.itemsigns$getItem(index);
    this.itemsigns$setItemAndUpdate(world, index, ItemStack.EMPTY);
    if (!player.isInCreativeMode()) {
      Block.dropStack(world, this.getPos(), stack);
    }
  }

  @Override
  public ItemStack itemsigns$getFrontStack() {
    return this.itemsigns$getItem(true);
  }

  @Override
  public ItemStack itemsigns$getBackStack() {
    return this.itemsigns$getItem(false);
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public DefaultedList<ItemStack> itemsigns$getItems() {
    SignItemsAttachment attachment = this.getAttached(ItemSignsAttachmentTypes.SIGN_ITEMS);
    if (attachment == null) {
      return SignItemsAttachment.createEmptyList();
    }
    return attachment.getAll();
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void clear() {
    if (!this.hasAttached(ItemSignsAttachmentTypes.SIGN_ITEMS)) {
      return;
    }
    this.itemsigns$editAttachment(SignItemsAttachment::clear);
  }

  @Unique
  private int itemsigns$getItemIndex(PlayerEntity player) {
    return this.itemsigns$getItemIndex(this.isPlayerFacingFront(player));
  }

  @Unique
  private int itemsigns$getItemIndex(boolean front) {
    return front ? 0 : 1;
  }

  @Unique
  private ItemStack itemsigns$getItem(boolean front) {
    return this.itemsigns$getItem(this.itemsigns$getItemIndex(front));
  }

  @Unique
  @SuppressWarnings("UnstableApiUsage")
  private ItemStack itemsigns$getItem(int index) {
    SignItemsAttachment attachment = this.getAttached(ItemSignsAttachmentTypes.SIGN_ITEMS);
    if (attachment == null) {
      return ItemStack.EMPTY;
    }
    return attachment.get(index);
  }

  @Unique
  public boolean itemsigns$hasItem(boolean front) {
    return this.itemsigns$hasItem(this.itemsigns$getItemIndex(front));
  }

  @Unique
  @SuppressWarnings("UnstableApiUsage")
  public boolean itemsigns$hasItem(int index) {
    SignItemsAttachment attachment = this.getAttached(ItemSignsAttachmentTypes.SIGN_ITEMS);
    if (attachment == null) {
      return false;
    }
    return attachment.hasItem(index);
  }

  @Unique
  private void itemsigns$setItemAndUpdate(World world, int index, ItemStack stack) {
    BlockPos blockPos = this.getPos();

    this.itemsigns$editAttachment((attachment) -> attachment.set(index, stack));
    this.updateListeners();

    world.playSound(
        null,
        blockPos,
        stack.isEmpty() ? SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM : SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM,
        SoundCategory.NEUTRAL,
        1f,
        1f
    );
  }

  @Unique
  @SuppressWarnings("UnstableApiUsage")
  private void itemsigns$editAttachment(Function<SignItemsAttachment, SignItemsAttachment> editor) {
    SignItemsAttachment edited = editor.apply(this.getAttachedOrCreate(ItemSignsAttachmentTypes.SIGN_ITEMS));
    this.setAttached(ItemSignsAttachmentTypes.SIGN_ITEMS, edited);

    World world = this.getWorld();
    if (world instanceof ServerWorld serverWorld) {
      SignItemStorage.getInstance(serverWorld).set(this.getPos(), edited);
    }
  }
}
