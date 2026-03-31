package me.roundaround.itemsigns.compat;

import me.roundaround.gradle.api.annotation.Entrypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Entrypoint("ebe_v1")
public class EnhancedBlockEntitiesCompat implements BiConsumer<Properties, Map<String, Component>>, Consumer<Runnable> {
  @Override
  public void accept(Runnable runnable) {
  }

  @Override
  public void accept(Properties config, Map<String, Component> reasons) {
    config.setProperty("render_enhanced_signs", "false");
    reasons.put("render_enhanced_signs", Component.translatable("itemsigns.ebecompat").withStyle(ChatFormatting.YELLOW));
  }
}
