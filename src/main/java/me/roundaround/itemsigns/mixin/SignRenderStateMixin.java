package me.roundaround.itemsigns.mixin;

import me.roundaround.itemsigns.interfaces.SignBlockEntityRenderStateExtensions;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignRenderState.class)
public class SignRenderStateMixin implements SignBlockEntityRenderStateExtensions {
  @Unique
  private ItemStackRenderState itemsigns$frontState;

  @Unique
  private ItemStackRenderState itemsigns$backState;

  @Override
  public void itemsigns$setFrontItemRenderState(ItemStackRenderState state) {
    this.itemsigns$frontState = state;
  }

  @Override
  public void itemsigns$setBackItemRenderState(ItemStackRenderState state) {
    this.itemsigns$backState = state;
  }

  @Override
  public ItemStackRenderState itemsigns$getFrontItemRenderState() {
    return this.itemsigns$frontState;
  }

  @Override
  public ItemStackRenderState itemsigns$getBackItemRenderState() {
    return this.itemsigns$backState;
  }
}
