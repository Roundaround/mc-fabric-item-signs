package me.roundaround.itemsigns.interfaces;

import me.roundaround.gradle.api.annotation.InjectedInterface;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@InjectedInterface
public interface SignBlockEntityExtensions extends Clearable {
  default boolean itemsigns$placeItemFacingPlayer(Level world, Player player, ItemStack stack) {
    return false;
  }

  default boolean itemsigns$hasItemFacingPlayer(Player player) {
    return false;
  }

  default void itemsigns$dropItemFacingPlayer(Level world, Player player) {
  }

  default ItemStack itemsigns$getFrontStack() {
    return ItemStack.EMPTY;
  }

  default ItemStack itemsigns$getBackStack() {
    return ItemStack.EMPTY;
  }

  default NonNullList<ItemStack> itemsigns$getItems() {
    return null;
  }

  default void itemsigns$setItem(int index, ItemStack stack) {
  }

  @Override
  default void clearContent() {
  }
}
