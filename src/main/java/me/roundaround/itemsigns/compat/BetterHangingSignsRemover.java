package me.roundaround.itemsigns.compat;

import me.roundaround.itemsigns.ItemSignsMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Util;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.world.entity.Display.ItemDisplay;

public final class BetterHangingSignsRemover {
  private static final String TAG_MAIN = "ketketbetterhangingsign";
  private static final String TAG_HANG = "hangdisplay";
  private static final String TAG_HASITEM = "hasitem";
  private static final String TAG_GLOW = "glow";
  private static final String TAG_MINECART = "hs.holder";

  private final HashMap<ResourceKey<Level>, HashMap<UUID, Entity>> trackedEntities = new HashMap<>();

  private Boolean loaded = null;
  private long scheduled = 0L;

  public void init() {
    ServerLifecycleEvents.SERVER_STARTING.register((server) -> this.loaded = null);
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> this.loaded = null);

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (this.isBetterHangingSignsLoaded(world)) {
        return;
      }

      if (entity.getTags().contains(TAG_MAIN) || entity.getTags().contains(TAG_MINECART)) {
        this.trackEntity(entity, world);
      }
    });

    ServerTickEvents.END_WORLD_TICK.register((world) -> {
      if (!this.hasTrackedEntities(world) || this.isBetterHangingSignsLoaded(world)) {
        return;
      }

      if (this.scheduled == 0L) {
        this.scheduled = Util.getMillis() + 5000L;
        return;
      }

      if (Util.getMillis() < this.scheduled) {
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

  public boolean isBetterHangingSignsLoaded(ServerLevel world) {
    if (this.loaded != null) {
      return this.loaded;
    }

    if (FabricLoader.getInstance().isModLoaded("mr_better_hangingsigns")) {
      return this.cacheLoaded(true);
    }

    ServerFunctionManager manager = world.getServer().getFunctions();
    for (Identifier id : manager.getFunctionNames()) {
      if (id.equals(Identifier.fromNamespaceAndPath("ketket_signs", "tick"))) {
        return this.cacheLoaded(true);
      }
    }

    PackRepository resourcePackManager = world.getServer().getPackRepository();
    resourcePackManager.reload();
    return this.cacheLoaded(resourcePackManager.getSelectedPacks()
        .stream()
        .anyMatch((profile) -> profile.getDescription().getString().contains("Better Hanging Signs")));
  }

  private void processTrackedEntities(ServerLevel world) {
    HashMap<UUID, Entity> entities = this.getTrackedEntities(world);

    HashMap<UUID, Interaction> roots = new HashMap<>();
    entities.forEach((uuid, entity) -> {
      if (entity instanceof Interaction root) {
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

  private ArrayDeque<ItemStack> handleRoot(Interaction root, HashMap<UUID, Entity> entities, ServerLevel world) {
    ItemStack frame = this.getFrameItem(root);
    ItemStack held = ItemStack.EMPTY;

    Optional<ItemDisplay> hangDisplay = this.findHangDisplay(root, entities.values());
    if (hangDisplay.isPresent()) {
      ItemDisplay display = hangDisplay.get();

      if (root.getTags().contains(TAG_HASITEM)) {
        held = display.getItemStack().copy();
      }

      display.getPassengers().forEach((p) -> {
        entities.remove(p.getUUID());
        p.remove(Entity.RemovalReason.DISCARDED);
      });
      display.ejectPassengers();
      entities.remove(display.getUUID());
      display.remove(Entity.RemovalReason.DISCARDED);
    }

    return Stream.of(held, frame).filter((stack) -> !stack.isEmpty()).collect(Collectors.toCollection(ArrayDeque::new));
  }

  private void returnItems(Entity root, ServerLevel world, ArrayDeque<ItemStack> items) {
    BlockPos pos = BlockPos.containing(root.getBoundingBox().getCenter());
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (!(blockEntity instanceof SignBlockEntity sign)) {
      Containers.dropContents(world, pos, this.createDefaultedList(items));
      return;
    }

    NonNullList<ItemStack> slots = sign.itemsigns$getItems();
    for (int i = 0; i < slots.size(); i++) {
      if (slots.get(i).isEmpty() && !items.isEmpty()) {
        sign.itemsigns$setItem(i, items.poll());
      }
    }

    if (!items.isEmpty()) {
      Containers.dropContents(world, pos, this.createDefaultedList(items));
      String message = String.format(
          "Couldn't find a suitable sign to attach items to while cleaning up Better Hanging Signs datapack. Dropped " +
          "%d item(s) at [x, y, z]=[%s]", items.size(), pos.toShortString()
      );
      ItemSignsMod.LOGGER.warn(message);
      world.getPlayers((p) -> p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
          .forEach((p) -> p.sendSystemMessage(Component.nullToEmpty(message)));
    }

    ItemSignsMod.LOGGER.info(
        "Cleaned up phantom entities from Better Hanging Signs datapack at [x, y, z]=[{}]",
        pos.toShortString()
    );
  }

  private NonNullList<ItemStack> createDefaultedList(ArrayDeque<ItemStack> items) {
    NonNullList<ItemStack> list = NonNullList.createWithCapacity(items.size());
    list.addAll(items);
    return list;
  }

  private boolean cacheLoaded(boolean loaded) {
    this.loaded = loaded;
    return this.loaded;
  }

  private void trackEntity(Entity entity, ServerLevel world) {
    this.trackedEntities.computeIfAbsent(world.dimension(), (k) -> new HashMap<>()).put(entity.getUUID(), entity);
  }

  private boolean hasTrackedEntities(ServerLevel world) {
    var forWorld = this.trackedEntities.get(world.dimension());
    if (forWorld == null) {
      return false;
    }
    return !forWorld.isEmpty();
  }

  private HashMap<UUID, Entity> getTrackedEntities(ServerLevel world) {
    var forWorld = this.trackedEntities.get(world.dimension());
    if (forWorld == null) {
      return new HashMap<>();
    }
    return new HashMap<>(forWorld);
  }

  private void clearTrackedEntities(ServerLevel world) {
    this.trackedEntities.remove(world.dimension());
  }

  private ItemStack getFrameItem(Entity entity) {
    return new ItemStack(entity.getTags().contains(TAG_GLOW) ? Items.GLOW_ITEM_FRAME : Items.ITEM_FRAME);
  }

  private Optional<ItemDisplay> findHangDisplay(Interaction root, Collection<Entity> entities) {
    return entities.stream().map((e) -> {
      if (e.getTags().contains(TAG_HANG) && e instanceof ItemDisplay display) {
        return display;
      }
      return null;
    }).filter(Objects::nonNull).min(Comparator.comparing(root::distanceToSqr));
  }
}
