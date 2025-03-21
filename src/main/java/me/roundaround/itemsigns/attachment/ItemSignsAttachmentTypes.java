package me.roundaround.itemsigns.attachment;

import me.roundaround.itemsigns.generated.Constants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

@SuppressWarnings("UnstableApiUsage")
public final class ItemSignsAttachmentTypes {
  public static final AttachmentType<SignItemsAttachment> SIGN_ITEMS = AttachmentRegistry.create(
      Identifier.of(Constants.MOD_ID, "items"),
      (builder) -> builder.initializer(() -> SignItemsAttachment.DEFAULT)
          .persistent(SignItemsAttachment.CODEC)
          .syncWith(SignItemsAttachment.PACKET_CODEC, AttachmentSyncPredicate.all())

  );

  public static void init() {
    // Empty method used simply for forced initialization.
  }

  private ItemSignsAttachmentTypes() {
  }
}
