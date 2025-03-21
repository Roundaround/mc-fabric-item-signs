package me.roundaround.itemsigns;

import me.roundaround.itemsigns.attachment.ItemSignsAttachmentTypes;
import me.roundaround.roundalib.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint(Entrypoint.MAIN)
public class ItemSignsMod implements ModInitializer {
  @Override
  public void onInitialize() {
    ItemSignsAttachmentTypes.init();
  }
}
