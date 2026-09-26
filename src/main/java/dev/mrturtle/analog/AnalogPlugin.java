package dev.mrturtle.analog;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.util.RadioUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

@ForgeVoicechatPlugin
public class AnalogPlugin implements VoicechatPlugin {
	public static VoicechatServerApi API;

	public static String RADIO_CATEGORY = "radios";

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
	}

	private void onServerStarted(VoicechatServerStartedEvent event) {
		API = event.getVoicechat();
		// Register radio volume category
		VolumeCategory radios = API.volumeCategoryBuilder()
				.setId(RADIO_CATEGORY)
				.setName("Radios")
				.setDescription("The volume of all radios")
				.build();
		API.registerVolumeCategory(radios);
	}

	private void onMicrophonePacket(MicrophonePacketEvent event) {
		VoicechatConnection connection = event.getSenderConnection();
		VoicechatServerApi serverApi = event.getVoicechat();
		if (connection == null)
			return;
		if (event.getPacket().getOpusEncodedData().length == 0)
			return;
		ServerPlayer sourcePlayer = (ServerPlayer) connection.getPlayer().getPlayer();
		if (sourcePlayer.isSpectator() && !serverApi.getServerConfig().getBoolean("spectator_interaction", false))
			return;
		// Find nearby players that might be carrying radios that could transmit
		sourcePlayer.getServer().execute(() -> {
			int listeningDistance = ConfigManager.config != null ? ConfigManager.config.radioListeningDistance * 2 : 16;
			List<ServerPlayer> playersInRange = sourcePlayer.serverLevel().getEntitiesOfClass(
					ServerPlayer.class,
					AABB.ofSize(sourcePlayer.position(), listeningDistance, listeningDistance, listeningDistance),
					entity -> true
			);
			for (ServerPlayer player : playersInRange) {
				List<ItemStack> radios = RadioUtil.getRadios(player);
				for (ItemStack stack : radios) {
					if (!RadioUtil.isRadioEnabled(stack))
						continue;
					if (!RadioUtil.isRadioTransmitting(stack))
						continue;
					int channel = RadioUtil.getRadioChannel(stack);
					RadioUtil.transmitOnChannel(serverApi, event.getPacket(), player, channel);
				}
			}
			// Find nearby transmitters and transmit on those too
			RadioUtil.transmitOnNearbyTransmitters(serverApi, event.getPacket(), sourcePlayer);
		});
	}

	@Override
	public String getPluginId() {
		return "analog";
	}
}
