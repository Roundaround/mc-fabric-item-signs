package me.roundaround.itemsigns.interfaces;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.apache.commons.lang3.NotImplementedException;

public interface SignBlockEntityRenderStateExtensions {
  default void itemsigns$setFrontItemRenderState(ItemStackRenderState state) {
    throw new NotImplementedException();
  }

  default void itemsigns$setBackItemRenderState(ItemStackRenderState state) {
    throw new NotImplementedException();
  }

  default ItemStackRenderState itemsigns$getFrontItemRenderState() {
    throw new NotImplementedException();
  }

  default ItemStackRenderState itemsigns$getBackItemRenderState() {
    throw new NotImplementedException();
  }
}
