package me.roundaround.itemsigns.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Clearable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public interface SignBlockEntityExtensions extends Clearable {
  default boolean itemsigns$placeItemFacingPlayer(World world, PlayerEntity player, ItemStack stack) {
    return false;
  }

  default boolean itemsigns$hasItemFacingPlayer(PlayerEntity player) {
    return false;
  }

  default void itemsigns$dropItemFacingPlayer(World world, PlayerEntity player) {
  }

  default ItemStack itemsigns$getFrontStack() {
    return ItemStack.EMPTY;
  }

  default ItemStack itemsigns$getBackStack() {
    return ItemStack.EMPTY;
  }

  default DefaultedList<ItemStack> itemsigns$getItems() {
    return null;
  }

  default void itemsigns$setItem(int index, ItemStack stack) {
  }

  @Override
  default void clear() {
  }
}
