package dev.mrturtle.analog;

import dev.mrturtle.analog.audio.assets.MusicAssetManager;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.network.ModNetwork;
import dev.mrturtle.analog.util.RadioUtil;
import dev.mrturtle.analog.world.GlobalRadioState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Analog.MODID)
public class Analog {
	public static final String MODID = "analog";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public Analog(IEventBus modEventBus) {
		ConfigManager.loadConfig();

		ModBlocks.BLOCKS.register(modEventBus);
		ModItems.ITEMS.register(modEventBus);
		ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
		ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
		ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

		modEventBus.addListener(ModNetwork::register);

		NeoForge.EVENT_BUS.addListener(this::onLevelTick);
		NeoForge.EVENT_BUS.addListener(this::onServerStarted);
	}

	private void onLevelTick(LevelTickEvent.Pre event) {
		if (event.getLevel() instanceof ServerLevel level) {
			GlobalRadioState globalRadioState = RadioUtil.getGlobalRadioState(level);
			globalRadioState.audioManager.tick(level);
		}
	}

	private void onServerStarted(ServerStartedEvent event) {
		MusicAssetManager.serverStarted(event.getServer());
	}
}