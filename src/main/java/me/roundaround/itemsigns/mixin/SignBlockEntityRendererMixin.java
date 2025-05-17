package me.roundaround.itemsigns.mixin;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {
  @Unique
  protected ItemRenderer itemsigns$itemRenderer;

  @Shadow
  abstract Vec3d getTextOffset();

  @Shadow
  public abstract float getTextScale();

  @Inject(method = "<init>", at = @At("RETURN"))
  private void atEndOfConstructor(BlockEntityRendererFactory.Context context, CallbackInfo ci) {
    this.itemsigns$itemRenderer = context.getItemRenderer();
  }

  @Inject(
      method = "render(Lnet/minecraft/block/entity/SignBlockEntity;Lnet/minecraft/client/util/math/MatrixStack;" +
               "Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/block/BlockState;" +
               "Lnet/minecraft/block/AbstractSignBlock;Lnet/minecraft/block/WoodType;" +
               "Lnet/minecraft/client/model/Model;)V", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
  )
  )
  protected void beforeMatrixStackPop(
      SignBlockEntity blockEntity,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      BlockState state,
      AbstractSignBlock block,
      WoodType woodType,
      Model model,
      CallbackInfo ci
  ) {
    this.itemsigns$renderItem(
        blockEntity,
        blockEntity.itemsigns$getFrontStack(),
        true,
        matrices,
        vertexConsumers,
        light,
        overlay
    );
    this.itemsigns$renderItem(
        blockEntity,
        blockEntity.itemsigns$getBackStack(),
        false,
        matrices,
        vertexConsumers,
        light,
        overlay
    );
  }

  @Unique
  protected void itemsigns$renderItem(
      SignBlockEntity blockEntity,
      ItemStack stack,
      boolean front,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay
  ) {
    if (stack.isEmpty()) {
      return;
    }

    World world = blockEntity.getWorld();
    int seed = (int) blockEntity.getPos().asLong() + (front ? 0 : 1);

    matrices.push();
    this.itemsigns$applyItemTransforms(matrices, front);
    this.itemsigns$itemRenderer.renderItem(
        stack,
        ModelTransformationMode.FIXED,
        light,
        overlay,
        matrices,
        vertexConsumers,
        world,
        seed
    );
    matrices.pop();
  }

  @Unique
  protected void itemsigns$applyItemTransforms(MatrixStack matrices, boolean front) {
    if (!front) {
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
    }

    Vec3d translation = this.getTextOffset();
    matrices.translate(translation.x, translation.y, translation.z);
    float scale = 0.5f * this.getTextScale();
    matrices.scale(-scale, scale, -scale);
  }
}
