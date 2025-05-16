package me.roundaround.itemsigns;

import me.roundaround.gradle.api.annotation.Entrypoint;
import me.roundaround.itemsigns.generated.Constants;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entrypoint(Entrypoint.MAIN)
public class ItemSignsMod implements ModInitializer {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

  @Override
  public void onInitialize() {
  }
}
