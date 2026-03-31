package me.roundaround.itemsigns.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
  @Unique
  private ItemModelResolver itemsigns$itemModelManager;

  @Shadow
  protected abstract Vec3 getTextOffset();

  @Shadow
  protected abstract float getSignTextRenderScale();

  @Inject(method = "<init>", at = @At("RETURN"))
  private void atEndOfConstructor(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
    this.itemsigns$itemModelManager = context.itemModelResolver();
  }

  @Inject(
      method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL")
  )
  private void afterUpdateRenderState(
      SignBlockEntity entity,
      SignRenderState state,
      float tickProgress,
      Vec3 cameraPos,
      ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand,
      CallbackInfo ci
  ) {
    int baseHash = HashCommon.long2int(entity.getBlockPos().asLong());

    ItemStack frontStack = entity.itemsigns$getFrontStack();
    if (!frontStack.isEmpty()) {
      ItemStackRenderState frontState = new ItemStackRenderState();
      this.itemsigns$itemModelManager.updateForTopItem(
          frontState,
          frontStack,
          ItemDisplayContext.ON_SHELF,
          entity.getLevel(),
          null,
          baseHash
      );
      state.itemsigns$setFrontItemRenderState(frontState);
    } else {
      state.itemsigns$setFrontItemRenderState(null);
    }

    ItemStack backStack = entity.itemsigns$getBackStack();
    if (!backStack.isEmpty()) {
      ItemStackRenderState backState = new ItemStackRenderState();
      this.itemsigns$itemModelManager.updateForTopItem(
          backState,
          entity.itemsigns$getBackStack(),
          ItemDisplayContext.ON_SHELF,
          entity.getLevel(),
          null,
          baseHash + 1
      );
      state.itemsigns$setBackItemRenderState(backState);
    } else {
      state.itemsigns$setBackItemRenderState(null);
    }
  }

  @Inject(
      method = "submitSignWithText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/SignBlock;Lnet/minecraft/world/level/block/state/properties/WoodType;Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V", at = @At(
      value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
  )
  )
  protected void beforeMatrixStackPop(
      SignRenderState state,
      PoseStack matrices,
      BlockState blockState,
      SignBlock block,
      WoodType woodType,
      Model.Simple model,
      ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
      SubmitNodeCollector queue,
      CallbackInfo ci
  ) {
    this.itemsigns$renderItem(state, state.itemsigns$getFrontItemRenderState(), matrices, queue, true);
    this.itemsigns$renderItem(state, state.itemsigns$getBackItemRenderState(), matrices, queue, false);
  }

  @Unique
  private void itemsigns$renderItem(
      SignRenderState state,
      ItemStackRenderState itemRenderState,
      PoseStack matrices,
      SubmitNodeCollector queue,
      boolean front
  ) {
    if (itemRenderState == null) {
      return;
    }

    matrices.pushPose();
    if (!front) {
      matrices.mulPose(Axis.YP.rotationDegrees(180f));
    }
    matrices.translate(this.getTextOffset());
    float scale = 0.5f * this.getSignTextRenderScale();
    matrices.scale(-scale, scale, -scale);
    itemRenderState.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    matrices.popPose();
  }
}
