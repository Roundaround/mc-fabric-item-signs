package me.roundaround.itemsigns;

import me.roundaround.gradle.api.annotation.Entrypoint;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.event.LoadFromNbtEvents;
import me.roundaround.itemsigns.generated.Constants;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.fabricmc.api.ModInitializer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.tag.BlockTags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entrypoint(Entrypoint.MAIN)
public class ItemSignsMod implements ModInitializer {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

  @Override
  public void onInitialize() {
    LoadFromNbtEvents.BLOCK_ENTITY.register((nbt, world, pos, state, registries) -> {
      if (!state.isIn(BlockTags.ALL_SIGNS)) {
        return state;
      }

      SignItemsAttachment attachment = SignItemStorage.getInstance(world).get(pos);
      if (attachment != null) {
        nbt.put(SignItemsAttachment.NBT_KEY, SignItemsAttachment.CODEC, registries.getOps(NbtOps.INSTANCE), attachment);
      }
      return state;
    });
  }
}
