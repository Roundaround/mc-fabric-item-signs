package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.itemsigns.attachment.ItemSignsAttachmentTypes;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SerializedChunk.class)
public abstract class SerializedChunkMixin {
  @SuppressWarnings("UnstableApiUsage")
  @Inject(
      method = "method_61797",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;getBoolean(Ljava/lang/String;)Z")
  )
  private static void beforeProcessingBlockEntity(
      List<NbtCompound> entities,
      ServerWorld world,
      List<NbtCompound> blockEntities,
      WorldChunk chunk,
      CallbackInfo ci,
      @Local NbtCompound nbt
  ) {
    if (!nbt.contains("x", NbtElement.NUMBER_TYPE) || !nbt.contains("y", NbtElement.NUMBER_TYPE) ||
        !nbt.contains("z", NbtElement.NUMBER_TYPE)) {
      return;
    }

    SignItemsAttachment attachment = SignItemStorage.getInstance(world).get(BlockEntity.posFromNbt(nbt));

    if (attachment == null) {
      return;
    }

    nbt.put(ItemSignsAttachmentTypes.SIGN_ITEMS.identifier().toString(), attachment.encode(world.getRegistryManager()));
  }
}
