package me.roundaround.itemsigns.mixin;

import me.roundaround.itemsigns.attachment.ItemSignsAttachmentTypes;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements AttachmentTargetImpl {
  @Shadow
  public abstract World getWorld();

  @Inject(method = "read", at = @At("HEAD"))
  private void beforeReadNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
    if (!(this.self() instanceof SignBlockEntity)) {
      return;
    }

    String key = ItemSignsAttachmentTypes.SIGN_ITEMS.identifier().toString();
    World world = this.getWorld();

    if (!nbt.contains(key) || world == null || world.isClient()) {
      return;
    }

    this.getAttachedOrCreate(
        ItemSignsAttachmentTypes.SIGN_ITEMS,
        () -> SignItemsAttachment.decode(nbt.get(key), registries)
    );
  }

  @Unique
  private BlockEntity self() {
    return (BlockEntity) (Object) this;
  }
}
