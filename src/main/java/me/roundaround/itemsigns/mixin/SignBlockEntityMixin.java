package me.roundaround.itemsigns.mixin;

import me.roundaround.itemsigns.attachment.ItemSignsAttachmentTypes;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.block.entity.SignBlockEntityExtensions;
import me.roundaround.itemsigns.server.SignItemStorage;
import me.roundaround.itemsigns.util.ClearableExtended;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Function;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityExtensions, ClearableExtended {
  protected SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Shadow
  public abstract boolean isPlayerFacingFront(PlayerEntity player);

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
  public void clear() {
    this.itemsigns$editAttachment(SignItemsAttachment::clear);
  }

  @Override
  protected void readComponents(BlockEntity.ComponentsAccess components) {
    super.readComponents(components);
    this.itemsigns$editAttachment((attachment) -> attachment.editAsList(components.getOrDefault(
        DataComponentTypes.CONTAINER,
        ContainerComponent.DEFAULT
    )::copyTo));
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(this.itemsigns$getAttachment().getAll()));
  }

  @SuppressWarnings("deprecation")
  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove("Items");
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
  private ItemStack itemsigns$getItem(int index) {
    return this.itemsigns$getAttachment().get(index);
  }

  @Unique
  public boolean itemsigns$hasItem(boolean front) {
    return this.itemsigns$hasItem(this.itemsigns$getItemIndex(front));
  }

  @Unique
  public boolean itemsigns$hasItem(int index) {
    return this.itemsigns$getAttachment().hasItem(index);
  }

  @Unique
  private void itemsigns$setItemAndUpdate(World world, int index, ItemStack stack) {
    BlockPos blockPos = this.getPos();

    this.itemsigns$editAttachment((attachment) -> attachment.set(index, stack));
    this.markDirty();

    world.playSound(
        null,
        blockPos,
        stack.isEmpty() ? SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM : SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM,
        SoundCategory.NEUTRAL,
        1f,
        1f
    );

    //    world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(player, this.getCachedState()));
    //    world.updateListeners(blockPos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Unique
  private SignItemsAttachment itemsigns$getAttachment() {
    return this.getAttachedOrCreate(ItemSignsAttachmentTypes.SIGN_ITEMS);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Unique
  private void itemsigns$editAttachment(Function<SignItemsAttachment, SignItemsAttachment> editor) {
    SignItemsAttachment edited = editor.apply(this.getAttachedOrCreate(ItemSignsAttachmentTypes.SIGN_ITEMS));
    this.setAttached(ItemSignsAttachmentTypes.SIGN_ITEMS, edited);

    World world = this.getWorld();
    if (world instanceof ServerWorld serverWorld) {
      SignItemStorage.getInstance(serverWorld).set(this.getPos(), edited);
    }
  }
}
