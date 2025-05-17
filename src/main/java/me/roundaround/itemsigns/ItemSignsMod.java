package me.roundaround.itemsigns;

import me.roundaround.gradle.api.annotation.Entrypoint;
import me.roundaround.itemsigns.attachment.SignItemsAttachment;
import me.roundaround.itemsigns.compat.BetterHangingSignsRemover;
import me.roundaround.itemsigns.event.LoadFromNbtEvents;
import me.roundaround.itemsigns.generated.Constants;
import me.roundaround.itemsigns.server.SignItemStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

@Entrypoint(Entrypoint.MAIN)
public class ItemSignsMod implements ModInitializer {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);
  public static final BetterHangingSignsRemover BHS_REMOVER = new BetterHangingSignsRemover();

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

    ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
      SignItemStorage storage = SignItemStorage.getInstance(world);
      HashMap<BlockPos, SignItemsAttachment> signs = storage.allInChunk(chunk.getPos());
      chunk.getBlockEntities().forEach((pos, blockEntity) -> {
        if (blockEntity instanceof SignBlockEntity) {
          signs.remove(pos);
        }
      });

      signs.forEach((pos, attachment) -> {
        if (attachment != null && !attachment.isEmpty()) {
          ItemSignsMod.LOGGER.warn(
              "Attached item data found for sign at {}, but no appropriate sign found. Dropping the attached items.",
              pos.toShortString()
          );

          ItemScatterer.spawn(world, pos, attachment.getAll());
        }

        storage.remove(pos);
      });
    });

    BHS_REMOVER.init();
  }
}
