package me.roundaround.itemsigns.compat;

import me.roundaround.itemsigns.ItemSignsMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;

public final class BetterHangingSignsRemover {
  private static final String TAG_MAIN = "ketketbetterhangingsign";
  private static final String TAG_HANG = "hangdisplay";
  private static final String TAG_HASITEM = "hasitem";
  private static final String TAG_GLOW = "glow";
  private static final String TAG_MINECART = "hs.holder";

  private final HashMap<RegistryKey<World>, HashMap<UUID, Entity>> trackedEntities = new HashMap<>();

  private Boolean loaded = null;
  private long scheduled = 0L;

  public void init() {
    ServerLifecycleEvents.SERVER_STARTING.register((server) -> this.loaded = null);
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> this.loaded = null);

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (this.isBetterHangingSignsLoaded(world)) {
        return;
      }

      if (entity.getCommandTags().contains(TAG_MAIN) || entity.getCommandTags().contains(TAG_MINECART)) {
        this.trackEntity(entity, world);
      }
    });

    ServerTickEvents.END_WORLD_TICK.register((world) -> {
      if (!this.hasTrackedEntities(world) || this.isBetterHangingSignsLoaded(world)) {
        return;
      }

      if (this.scheduled == 0L) {
        this.scheduled = Util.getMeasuringTimeMs() + 5000L;
        return;
      }

      if (Util.getMeasuringTimeMs() < this.scheduled) {
        return;
      }

      this.processTrackedEntities(world);
      this.scheduled = 0L;
    });

    ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
      this.scheduled = 0L;
      this.trackedEntities.clear();
    });
  }

  public boolean isBetterHangingSignsLoaded(ServerWorld world) {
    if (this.loaded != null) {
      return this.loaded;
    }

    if (FabricLoader.getInstance().isModLoaded("mr_better_hangingsigns")) {
      return this.cacheLoaded(true);
    }

    CommandFunctionManager manager = world.getServer().getCommandFunctionManager();
    for (Identifier id : manager.getAllFunctions()) {
      if (id.equals(Identifier.of("ketket_signs", "tick"))) {
        return this.cacheLoaded(true);
      }
    }

    ResourcePackManager resourcePackManager = world.getServer().getDataPackManager();
    resourcePackManager.scanPacks();
    return this.cacheLoaded(resourcePackManager.getEnabledProfiles()
        .stream()
        .anyMatch((profile) -> profile.getDescription().getString().contains("Better Hanging Signs")));
  }

  private void processTrackedEntities(ServerWorld world) {
    HashMap<UUID, Entity> entities = this.getTrackedEntities(world);

    HashMap<UUID, InteractionEntity> roots = new HashMap<>();
    entities.forEach((uuid, entity) -> {
      if (entity instanceof InteractionEntity root) {
        roots.put(uuid, root);
      }
    });

    roots.forEach((uuid, root) -> {
      entities.remove(uuid);
      ArrayDeque<ItemStack> items = this.handleRoot(root, entities, world);
      this.returnItems(root, world, items);
      root.remove(Entity.RemovalReason.DISCARDED);
    });

    entities.forEach((uuid, entity) -> entity.remove(Entity.RemovalReason.DISCARDED));

    this.clearTrackedEntities(world);
  }

  private ArrayDeque<ItemStack> handleRoot(InteractionEntity root, HashMap<UUID, Entity> entities, ServerWorld world) {
    ItemStack frame = this.getFrameItem(root);
    ItemStack held = ItemStack.EMPTY;

    Optional<ItemDisplayEntity> hangDisplay = this.findHangDisplay(root, entities.values());
    if (hangDisplay.isPresent()) {
      ItemDisplayEntity display = hangDisplay.get();

      if (root.getCommandTags().contains(TAG_HASITEM)) {
        held = display.getStackReference(0).get().copy();
      }

      display.getPassengerList().forEach((p) -> {
        entities.remove(p.getUuid());
        p.remove(Entity.RemovalReason.DISCARDED);
      });
      display.removeAllPassengers();
      entities.remove(display.getUuid());
      display.remove(Entity.RemovalReason.DISCARDED);
    }

    return Stream.of(held, frame).filter((stack) -> !stack.isEmpty()).collect(Collectors.toCollection(ArrayDeque::new));
  }

  private void returnItems(Entity root, ServerWorld world, ArrayDeque<ItemStack> items) {
    BlockPos pos = BlockPos.ofFloored(root.getBoundingBox().getCenter());
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (!(blockEntity instanceof SignBlockEntity sign)) {
      ItemScatterer.spawn(world, pos, this.createDefaultedList(items));
      return;
    }

    DefaultedList<ItemStack> slots = sign.itemsigns$getItems();
    for (int i = 0; i < slots.size(); i++) {
      if (slots.get(i).isEmpty() && !items.isEmpty()) {
        sign.itemsigns$setItem(i, items.poll());
      }
    }

    if (!items.isEmpty()) {
      ItemScatterer.spawn(world, pos, this.createDefaultedList(items));
      String message = String.format(
          "Couldn't find a suitable sign to attach items to while cleaning up Better Hanging Signs datapack. Dropped " +
          "%d item(s) at [x, y, z]=[%s]", items.size(), pos.toShortString()
      );
      ItemSignsMod.LOGGER.warn(message);
      world.getPlayers((p) -> p.hasPermissionLevel(world.getServer().getOpPermissionLevel()))
          .forEach((p) -> p.sendMessage(Text.of(message)));
    }

    ItemSignsMod.LOGGER.info(
        "Cleaned up phantom entities from Better Hanging Signs datapack at [x, y, z]=[{}]",
        pos.toShortString()
    );
  }

  private DefaultedList<ItemStack> createDefaultedList(ArrayDeque<ItemStack> items) {
    DefaultedList<ItemStack> list = DefaultedList.ofSize(items.size());
    list.addAll(items);
    return list;
  }

  private boolean cacheLoaded(boolean loaded) {
    this.loaded = loaded;
    return this.loaded;
  }

  private void trackEntity(Entity entity, ServerWorld world) {
    this.trackedEntities.computeIfAbsent(world.getRegistryKey(), (k) -> new HashMap<>()).put(entity.getUuid(), entity);
  }

  private boolean hasTrackedEntities(ServerWorld world) {
    var forWorld = this.trackedEntities.get(world.getRegistryKey());
    if (forWorld == null) {
      return false;
    }
    return !forWorld.isEmpty();
  }

  private HashMap<UUID, Entity> getTrackedEntities(ServerWorld world) {
    var forWorld = this.trackedEntities.get(world.getRegistryKey());
    if (forWorld == null) {
      return new HashMap<>();
    }
    return new HashMap<>(forWorld);
  }

  private void clearTrackedEntities(ServerWorld world) {
    this.trackedEntities.remove(world.getRegistryKey());
  }

  private ItemStack getFrameItem(Entity entity) {
    return new ItemStack(entity.getCommandTags().contains(TAG_GLOW) ? Items.GLOW_ITEM_FRAME : Items.ITEM_FRAME);
  }

  private Optional<ItemDisplayEntity> findHangDisplay(InteractionEntity root, Collection<Entity> entities) {
    return entities.stream().map((e) -> {
      if (e.getCommandTags().contains(TAG_HANG) && e instanceof ItemDisplayEntity display) {
        return display;
      }
      return null;
    }).filter(Objects::nonNull).min(Comparator.comparing(root::squaredDistanceTo));
  }
}
