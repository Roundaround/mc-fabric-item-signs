package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.interfaces.SignBlockEntityExtensions;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityExtensions {
  protected SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Unique
  @Nullable
  private SignItemsAttachment itemsigns$attachment = null;

  @Shadow
  public abstract boolean isFacingFrontText(Player player);

  @Shadow
  protected abstract void markUpdated();

  @ModifyReturnValue(method = "getUpdateTag", at = @At("RETURN"))
  private CompoundTag afterInitialChunkDataNbtGenerated(CompoundTag nbt, HolderLookup.Provider registries) {
    Optional.ofNullable(this.itemsigns$attachment).ifPresent((attachment) -> nbt.store(
        SignItemsAttachment.NBT_KEY,
        SignItemsAttachment.CODEC,
        registries.createSerializationContext(NbtOps.INSTANCE),
        attachment
    ));
    return nbt;
  }

  @Inject(method = "loadAdditional", at = @At("RETURN"))
  private void afterReadData(ValueInput view, CallbackInfo ci) {
    view.read(SignItemsAttachment.NBT_KEY, SignItemsAttachment.CODEC)
        .ifPresent((attachment) -> this.itemsigns$attachment = attachment);
  }

  @Override
  public boolean itemsigns$placeItemFacingPlayer(Level world, Player player, ItemStack stack) {
    int index = this.itemsigns$getItemIndex(player);
    if (this.itemsigns$hasItem(index)) {
      return false;
    }
    this.itemsigns$setItem(world, index, stack.consumeAndReturn(1, player));
    return true;
  }

  @Override
  public boolean itemsigns$hasItemFacingPlayer(Player player) {
    return this.itemsigns$hasItem(this.isFacingFrontText(player));
  }

  @Override
  public void itemsigns$dropItemFacingPlayer(Level world, Player player) {
    int index = this.itemsigns$getItemIndex(player);
    if (!this.itemsigns$hasItem(index)) {
      return;
    }

    ItemStack stack = this.itemsigns$getItem(index);
    this.itemsigns$setItem(world, index, ItemStack.EMPTY);
    if (!player.hasInfiniteMaterials()) {
      Block.popResource(world, this.getBlockPos(), stack);
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
  public NonNullList<ItemStack> itemsigns$getItems() {
    if (this.itemsigns$attachment == null) {
      return SignItemsAttachment.createEmptyList();
    }
    return this.itemsigns$attachment.getAll();
  }

  @Override
  public void itemsigns$setItem(int index, ItemStack stack) {
    this.itemsigns$editAttachment((attachment) -> attachment.set(index, stack));
    this.markUpdated();
  }

  @Override
  public void clearContent() {
    if (this.itemsigns$attachment == null) {
      return;
    }
    this.itemsigns$editAttachment(SignItemsAttachment::clear);
  }

  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
    super.preRemoveSideEffects(pos, oldState);

    if (this.level == null || !(this.level instanceof ServerLevel serverWorld)) {
      return;
    }

    Containers.dropContents(serverWorld, pos, this.itemsigns$getItems());
    SignItemStorage.getInstance(serverWorld).remove(pos);
  }

  @Unique
  private int itemsigns$getItemIndex(Player player) {
    return this.itemsigns$getItemIndex(this.isFacingFrontText(player));
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
    if (this.itemsigns$attachment == null) {
      return ItemStack.EMPTY;
    }
    return this.itemsigns$attachment.get(index);
  }

  @Unique
  private boolean itemsigns$hasItem(boolean front) {
    return this.itemsigns$hasItem(this.itemsigns$getItemIndex(front));
  }

  @Unique
  private boolean itemsigns$hasItem(int index) {
    if (this.itemsigns$attachment == null) {
      return false;
    }
    return this.itemsigns$attachment.hasItem(index);
  }

  @Unique
  private void itemsigns$setItem(Level world, int index, ItemStack stack) {
    this.itemsigns$setItem(index, stack);
    world.playSound(
        null,
        this.getBlockPos(),
        stack.isEmpty() ? SoundEvents.ITEM_FRAME_REMOVE_ITEM : SoundEvents.ITEM_FRAME_ADD_ITEM,
        SoundSource.NEUTRAL,
        1f,
        1f
    );
  }

  @Unique
  private void itemsigns$editAttachment(Function<SignItemsAttachment, SignItemsAttachment> editor) {
    this.itemsigns$attachment = editor.apply(Optional.ofNullable(this.itemsigns$attachment)
        .orElse(SignItemsAttachment.DEFAULT));

    Level world = this.getLevel();
    if (world instanceof ServerLevel serverWorld) {
      SignItemStorage.getInstance(serverWorld).set(this.getBlockPos(), this.itemsigns$attachment);
    }
  }
}
