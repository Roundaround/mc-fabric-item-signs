package me.roundaround.itemsigns.interfaces;

import net.minecraft.client.render.item.ItemRenderState;
import org.apache.commons.lang3.NotImplementedException;

public interface SignBlockEntityRenderStateExtensions {
  default void itemsigns$setFrontItemRenderState(ItemRenderState state) {
    throw new NotImplementedException();
  }

  default void itemsigns$setBackItemRenderState(ItemRenderState state) {
    throw new NotImplementedException();
  }

  default ItemRenderState itemsigns$getFrontItemRenderState() {
    throw new NotImplementedException();
  }

  default ItemRenderState itemsigns$getBackItemRenderState() {
    throw new NotImplementedException();
  }
}
