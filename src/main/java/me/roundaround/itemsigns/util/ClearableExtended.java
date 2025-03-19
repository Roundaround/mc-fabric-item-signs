package me.roundaround.itemsigns.util;

import net.minecraft.util.Clearable;

public interface ClearableExtended extends Clearable {
  @Override
  default void clear() {}
}
