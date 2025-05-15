package me.roundaround.itemsigns.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BiFunction;

@Mixin(BlockEntityUpdateS2CPacket.class)
public abstract class BlockEntityUpdateS2CPacketMixin {
  @WrapMethod(method = "create(Lnet/minecraft/block/entity/BlockEntity;Ljava/util/function/BiFunction;)" +
                       "Lnet/minecraft/network/packet/s2c/play/BlockEntityUpdateS2CPacket;")
  private static BlockEntityUpdateS2CPacket wrapCreate(
      BlockEntity blockEntity,
      BiFunction<BlockEntity, DynamicRegistryManager, NbtCompound> nbtGetter,
      Operation<BlockEntityUpdateS2CPacket> original
  ) {
    return original.call(blockEntity, itemsigns$wrapNbtGetter(nbtGetter));
  }

  @Unique
  @SuppressWarnings("UnstableApiUsage")
  private static BiFunction<BlockEntity, DynamicRegistryManager, NbtCompound> itemsigns$wrapNbtGetter(BiFunction<BlockEntity, DynamicRegistryManager, NbtCompound> nbtGetter) {
    return (entity, registries) -> {
      NbtCompound nbt = nbtGetter.apply(entity, registries);
      // Include data attachments in block entity update packets: https://github.com/FabricMC/fabric/issues/4638
      // Check for the attachment key first before serializing to avoid doubling work in case FAPI gets fixed.
      if (!nbt.contains(AttachmentTarget.NBT_ATTACHMENT_KEY)) {
        ((AttachmentTargetImpl) entity).fabric_writeAttachmentsToNbt(nbt, registries);
      }
      return nbt;
    };
  }
}
