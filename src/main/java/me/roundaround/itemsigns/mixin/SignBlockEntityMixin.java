package me.roundaround.itemsigns.mixin;

import com.mojang.serialization.DynamicOps;
import me.roundaround.itemsigns.block.entity.SignBlockEntityExtensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity implements SignBlockEntityExtensions {
  // TODO: Add items to dropped stacks
  @Unique
  private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);

  protected SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Shadow
  public abstract boolean isPlayerFacingFront(PlayerEntity player);

  @Shadow
  @Final
  private static Logger LOGGER;

  @Inject(method = "writeNbt", at = @At("TAIL"))
  private void writeAdditionalNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
    DynamicOps<NbtElement> dynamicOps = registries.getOps(NbtOps.INSTANCE);
    ItemStack.OPTIONAL_CODEC.encodeStart(dynamicOps, this.itemsigns$getFrontStack())
        .resultOrPartial(LOGGER::error)
        .ifPresent((frontStack) -> nbt.put("front_stack", frontStack));
    ItemStack.OPTIONAL_CODEC.encodeStart(dynamicOps, this.itemsigns$getBackStack())
        .resultOrPartial(LOGGER::error)
        .ifPresent((backStack) -> nbt.put("back_stack", backStack));
  }

  @Inject(method = "readNbt", at = @At("TAIL"))
  private void readAdditionalNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
    DynamicOps<NbtElement> dynamicOps = registries.getOps(NbtOps.INSTANCE);
    if (nbt.contains("front_stack")) {
      ItemStack.OPTIONAL_CODEC.parse(dynamicOps, nbt.getCompound("front_stack"))
          .resultOrPartial(LOGGER::error)
          .ifPresent((frontStack) -> this.items.set(this.getIndex(true), frontStack));
    }
    if (nbt.contains("back_stack")) {
      ItemStack.OPTIONAL_CODEC.parse(dynamicOps, nbt.getCompound("back_stack"))
          .resultOrPartial(LOGGER::error)
          .ifPresent((backStack) -> this.items.set(this.getIndex(false), backStack));
    }
  }

  @Unique
  private int getIndex(boolean front) {
    return front ? 0 : 1;
  }

  @Unique
  private void updateListeners() {
    this.markDirty();
    Objects.requireNonNull(this.getWorld())
        .updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
  }

  @Unique
  private void setItem(World world, PlayerEntity player, ItemStack stack, int index) {
    this.items.set(index, stack);
    world.emitGameEvent(GameEvent.BLOCK_CHANGE, this.getPos(), GameEvent.Emitter.of(player, this.getCachedState()));
    this.updateListeners();
  }

  @Unique
  private void playSound(SoundEvent soundEvent) {
    World world = this.getWorld();
    if (world == null) {
      return;
    }
    world.playSound(null, this.getPos(), soundEvent, SoundCategory.NEUTRAL, 1f, 1f);
  }

  @Override
  public boolean itemsigns$placeItem(World world, PlayerEntity player, ItemStack stack) {
    int index = this.getIndex(this.isPlayerFacingFront(player));
    if (!this.items.get(index).isEmpty()) {
      return false;
    }

    this.setItem(world, player, stack.splitUnlessCreative(1, player), index);
    this.playSound(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM);
    return true;
  }

  @Override
  public boolean itemsigns$hasItem(PlayerEntity player) {
    return !this.items.get(this.getIndex(this.isPlayerFacingFront(player))).isEmpty();
  }

  @Override
  public void itemsigns$dropItem(World world, PlayerEntity player) {
    int index = this.getIndex(this.isPlayerFacingFront(player));
    ItemStack stack = this.items.get(index);
    if (stack.isEmpty()) {
      return;
    }

    this.setItem(world, player, ItemStack.EMPTY, index);
    this.playSound(SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM);
    if (player.isInCreativeMode()) {
      return;
    }

    Block.dropStack(world, this.getPos(), stack);
  }

  @Override
  public ItemStack itemsigns$getFrontStack() {
    return this.items.get(this.getIndex(true));
  }

  @Override
  public ItemStack itemsigns$getBackStack() {
    return this.items.get(this.getIndex(false));
  }
}
