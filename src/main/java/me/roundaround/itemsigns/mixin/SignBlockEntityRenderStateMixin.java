package me.roundaround.itemsigns.mixin;

import me.roundaround.itemsigns.interfaces.SignBlockEntityRenderStateExtensions;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SignBlockEntityRenderState.class)
public class SignBlockEntityRenderStateMixin implements SignBlockEntityRenderStateExtensions {
  @Unique
  private ItemRenderState itemsigns$frontState;

  @Unique
  private ItemRenderState itemsigns$backState;

  @Override
  public void itemsigns$setFrontItemRenderState(ItemRenderState state) {
    this.itemsigns$frontState = state;
  }

  @Override
  public void itemsigns$setBackItemRenderState(ItemRenderState state) {
    this.itemsigns$backState = state;
  }

  @Override
  public ItemRenderState itemsigns$getFrontItemRenderState() {
    return this.itemsigns$frontState;
  }

  @Override
  public ItemRenderState itemsigns$getBackItemRenderState() {
    return this.itemsigns$backState;
  }
}
