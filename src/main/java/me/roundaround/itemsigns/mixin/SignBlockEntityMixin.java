package me.roundaround.itemsigns.mixin;

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
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityExtensions, ClearableExtended {
  @Unique
  private final DefaultedList<ItemStack> itemsigns$items = DefaultedList.ofSize(2, ItemStack.EMPTY);

  protected SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Shadow
  public abstract boolean isPlayerFacingFront(PlayerEntity player);

  @Inject(method = "readNbt", at = @At("RETURN"))
  private void readAdditionalNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
    SignItemStorage.getInstance(this).ifPresent((manager) -> manager.read(this.getPos(), this.itemsigns$items));
  }

  @Unique
  private int itemsigns$getIndexForSide(boolean front) {
    return front ? 0 : 1;
  }

  @Unique
  private void itemsigns$updateListeners() {
    this.markDirty();

    BlockPos blockPos = this.getPos();
    Objects.requireNonNull(this.getWorld())
        .updateListeners(blockPos, this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
    SignItemStorage.getInstance(this).ifPresent((manager) -> manager.write(blockPos, this.itemsigns$items));
  }

  @Unique
  private void itemsigns$setItemOnPlayerSide(World world, PlayerEntity player, ItemStack stack, int index) {
    this.itemsigns$items.set(index, stack);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, this.getPos(), GameEvent.Emitter.of(player, this.getCachedState()));
    this.itemsigns$updateListeners();
    world.playSound(
        null,
        this.getPos(),
        stack.isEmpty() ? SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM : SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM,
        SoundCategory.NEUTRAL,
        1f,
        1f
    );
  }

  @Override
  public boolean itemsigns$placeItemFacingPlayer(World world, PlayerEntity player, ItemStack stack) {
    int index = this.itemsigns$getIndexForSide(this.isPlayerFacingFront(player));
    if (!this.itemsigns$items.get(index).isEmpty()) {
      return false;
    }

    this.itemsigns$setItemOnPlayerSide(world, player, stack.splitUnlessCreative(1, player), index);
    return true;
  }

  @Override
  public boolean itemsigns$hasItemFacingPlayer(PlayerEntity player) {
    return !this.itemsigns$items.get(this.itemsigns$getIndexForSide(this.isPlayerFacingFront(player))).isEmpty();
  }

  @Override
  public void itemsigns$dropItemFacingPlayer(World world, PlayerEntity player) {
    int index = this.itemsigns$getIndexForSide(this.isPlayerFacingFront(player));
    ItemStack stack = this.itemsigns$items.get(index);
    if (stack.isEmpty()) {
      return;
    }

    this.itemsigns$setItemOnPlayerSide(world, player, ItemStack.EMPTY, index);
    if (player.isInCreativeMode()) {
      return;
    }

    Block.dropStack(world, this.getPos(), stack);
  }

  @Override
  public ItemStack itemsigns$getFrontStack() {
    return this.itemsigns$items.get(this.itemsigns$getIndexForSide(true));
  }

  @Override
  public ItemStack itemsigns$getBackStack() {
    return this.itemsigns$items.get(this.itemsigns$getIndexForSide(false));
  }

  @Override
  public void clear() {
    this.itemsigns$items.clear();
    SignItemStorage.getInstance(this).ifPresent((manager) -> manager.clear(this.getPos()));
  }

  @Override
  protected void readComponents(BlockEntity.ComponentsAccess components) {
    super.readComponents(components);
    components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(this.itemsigns$items);
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(this.itemsigns$items));
  }

  @SuppressWarnings("deprecation")
  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove("Items");
  }
}
