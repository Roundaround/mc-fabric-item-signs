package me.roundaround.itemsigns.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.HashCommon;
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
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
  @Unique
  private ItemModelResolver itemsigns$itemModelManager;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void atEndOfConstructor(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
    this.itemsigns$itemModelManager = context.itemModelResolver();
  }

  @Inject(
      method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;" +
               "Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;" +
               "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL")
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

  @Inject(method = "submitSignWithText", at = @At(value = "RETURN"))
  protected void beforeMatrixStackPop(
      SignRenderState state,
      PoseStack poseStack,
      ModelFeatureRenderer.CrumblingOverlay breakProgress,
      SubmitNodeCollector submitNodeCollector,
      CallbackInfo ci
  ) {
    this.itemsigns$renderItem(state, state.itemsigns$getFrontItemRenderState(), poseStack, submitNodeCollector, true);
    this.itemsigns$renderItem(state, state.itemsigns$getBackItemRenderState(), poseStack, submitNodeCollector, false);
  }

  @Unique
  private void itemsigns$renderItem(
      SignRenderState state,
      ItemStackRenderState itemRenderState,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      boolean front
  ) {
    if (itemRenderState == null) {
      return;
    }

    AABB bounds = itemRenderState.getModelBoundingBox();
    float depth = (float) bounds.getZsize();
    float scale = depth > 0.25f ? 24f : 32f;

    float scaledDepth = depth * scale;
    float signThickness = 1f;
    float shift = Math.max(0f, (scaledDepth - signThickness) / 2f);

    poseStack.pushPose();
    poseStack.mulPose(front ? state.transformations.frontText() : state.transformations.backText());
    poseStack.translate(0f, 0f, shift);
    poseStack.scale(scale, -scale, scale);
    itemRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    poseStack.popPose();
  }
}
