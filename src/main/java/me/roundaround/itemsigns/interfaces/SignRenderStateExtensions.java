package me.roundaround.itemsigns.interfaces;

import me.roundaround.gradle.api.annotation.InjectedInterface;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.apache.commons.lang3.NotImplementedException;

@InjectedInterface
public interface SignRenderStateExtensions {
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
