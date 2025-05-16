package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.roundaround.itemsigns.ItemSignsMod;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.StorageKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;

@Mixin(SerializedChunk.class)
public abstract class SerializedChunkMixin {
  @Shadow
  @Final
  private List<NbtCompound> blockEntities;

  @Inject(
      method = "method_61797",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;getBoolean(Ljava/lang/String;Z)Z")
  )
  private static void beforeProcessingBlockEntity(
      List<NbtCompound> entities,
      ServerWorld world,
      List<NbtCompound> blockEntities,
      WorldChunk chunk,
      CallbackInfo ci,
      @Local NbtCompound nbt
  ) {
    if (!nbt.contains("x") || !nbt.contains("y") || !nbt.contains("z")) {
      return;
    }

    SignItemsAttachment attachment = SignItemStorage.getInstance(world)
        .get(BlockEntity.posFromNbt(chunk.getPos(), nbt));

    if (attachment == null) {
      return;
    }

    nbt.put(
        SignItemsAttachment.NBT_KEY,
        SignItemsAttachment.CODEC,
        world.getRegistryManager().getOps(NbtOps.INSTANCE),
        attachment
    );
  }

  @Inject(
      method = "convert", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/chunk/WorldChunk;<init>(Lnet/minecraft/world/World;" +
               "Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;" +
               "Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;" +
               "J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;" +
               "Lnet/minecraft/world/gen/chunk/BlendingData;)V"
  )
  )
  private void beforeCreatingWorldChunk(
      ServerWorld world,
      PointOfInterestStorage poiStorage,
      StorageKey key,
      ChunkPos expectedPos,
      CallbackInfoReturnable<ProtoChunk> cir
  ) {
    ServerWorld serverWorld = world.toServerWorld();
    SignItemStorage storage = SignItemStorage.getInstance(serverWorld);
    HashMap<BlockPos, SignItemsAttachment> signs = storage.allInChunk(expectedPos);
    for (NbtCompound nbt : this.blockEntities) {
      signs.remove(BlockEntity.posFromNbt(expectedPos, nbt));
    }

    signs.forEach((pos, attachment) -> {
      if (attachment != null && !attachment.isEmpty()) {
        ItemSignsMod.LOGGER.warn(
            "Attached item data found for sign at {}, but no appropriate sign found. Dropping the attached items.",
            pos.toShortString()
        );

        ItemScatterer.spawn(serverWorld, pos, attachment.getAll());
      }

      storage.remove(pos);
    });
  }
}
