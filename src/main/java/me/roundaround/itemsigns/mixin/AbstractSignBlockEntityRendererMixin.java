package me.roundaround.itemsigns.mixin;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignBlockEntityRenderer.class)
public abstract class AbstractSignBlockEntityRendererMixin {
  @Unique
  private ItemModelManager itemsigns$itemModelManager;

  @Shadow
  protected abstract Vec3d getTextOffset();

  @Shadow
  protected abstract float getTextScale();

  @Inject(method = "<init>", at = @At("RETURN"))
  private void atEndOfConstructor(BlockEntityRendererFactory.Context context, CallbackInfo ci) {
    this.itemsigns$itemModelManager = context.itemModelManager();
  }

  @Inject(
      method = "updateRenderState(Lnet/minecraft/block/entity/SignBlockEntity;" +
               "Lnet/minecraft/client/render/block/entity/state/SignBlockEntityRenderState;" +
               "FLnet/minecraft/util/math/Vec3d;" +
               "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V", at = @At("TAIL")
  )
  private void afterUpdateRenderState(
      SignBlockEntity entity,
      SignBlockEntityRenderState state,
      float tickProgress,
      Vec3d cameraPos,
      ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand,
      CallbackInfo ci
  ) {
    int baseHash = HashCommon.long2int(entity.getPos().asLong());

    ItemStack frontStack = entity.itemsigns$getFrontStack();
    if (!frontStack.isEmpty()) {
      ItemRenderState frontState = new ItemRenderState();
      this.itemsigns$itemModelManager.clearAndUpdate(
          frontState,
          frontStack,
          ItemDisplayContext.ON_SHELF,
          entity.getWorld(),
          null,
          baseHash
      );
      state.itemsigns$setFrontItemRenderState(frontState);
    } else {
      state.itemsigns$setFrontItemRenderState(null);
    }

    ItemStack backStack = entity.itemsigns$getBackStack();
    if (!backStack.isEmpty()) {
      ItemRenderState backState = new ItemRenderState();
      this.itemsigns$itemModelManager.clearAndUpdate(
          backState,
          entity.itemsigns$getBackStack(),
          ItemDisplayContext.ON_SHELF,
          entity.getWorld(),
          null,
          baseHash + 1
      );
      state.itemsigns$setBackItemRenderState(backState);
    } else {
      state.itemsigns$setBackItemRenderState(null);
    }
  }

  @Inject(
      method = "render(Lnet/minecraft/client/render/block/entity/state/SignBlockEntityRenderState;" +
               "Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/block/BlockState;" +
               "Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;" +
               "Lnet/minecraft/client/model/Model$SinglePartModel;" +
               "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;" +
               "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
  )
  )
  protected void beforeMatrixStackPop(
      SignBlockEntityRenderState state,
      MatrixStack matrices,
      BlockState blockState,
      AbstractSignBlock block,
      WoodType woodType,
      Model.SinglePartModel model,
      ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
      OrderedRenderCommandQueue queue,
      CallbackInfo ci
  ) {
    this.itemsigns$renderItem(state, state.itemsigns$getFrontItemRenderState(), matrices, queue, true);
    this.itemsigns$renderItem(state, state.itemsigns$getBackItemRenderState(), matrices, queue, false);
  }

  @Unique
  private void itemsigns$renderItem(
      SignBlockEntityRenderState state,
      ItemRenderState itemRenderState,
      MatrixStack matrices,
      OrderedRenderCommandQueue queue,
      boolean front
  ) {
    if (itemRenderState == null) {
      return;
    }

    matrices.push();
    if (!front) {
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
    }
    matrices.translate(this.getTextOffset());
    float scale = 0.5f * this.getTextScale();
    matrices.scale(-scale, scale, -scale);
    itemRenderState.render(matrices, queue, state.lightmapCoordinates, OverlayTexture.DEFAULT_UV, 0);
    matrices.pop();
  }
}
