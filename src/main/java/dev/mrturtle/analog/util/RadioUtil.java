package dev.mrturtle.analog.util;

import com.google.common.collect.ImmutableList;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import dev.mrturtle.analog.ModBlocks;
import dev.mrturtle.analog.ModDataComponents;
import dev.mrturtle.analog.ModItems;
import dev.mrturtle.analog.audio.RadioAudioInstance;
import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.item.component.RadioComponent;
import dev.mrturtle.analog.world.GlobalRadioState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RadioUtil {
	public static void transmitOnChannel(VoicechatServerApi serverApi, MicrophonePacket packet, ServerPlayer sender, int senderChannel) {
		MinecraftServer server = sender.getServer();
		if (server == null)
			return;
		ServerLevel world = sender.serverLevel();
		byte[] encodedData = packet.getOpusEncodedData();

		// Player radios
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player == sender)
				continue;
			if (!isReceivingChannel(player, senderChannel))
				continue;
			// Play voice to nearby players
			int listeningDistance = ConfigManager.config != null ? ConfigManager.config.radioListeningDistance * 2 : 16;
			List<Player> playersInRange = world.getEntitiesOfClass(
					Player.class,
					AABB.ofSize(player.position(), listeningDistance, listeningDistance, listeningDistance),
					entity -> true
			);
			for (Player entity : playersInRange) {
				if (serverApi.getConnectionOf(entity.getUUID()) == null)
					continue;
				if (entity != player && entity != sender && isReceivingChannel(entity, senderChannel))
					continue;
				serverApi.sendLocationalSoundPacketTo(
						serverApi.getConnectionOf(entity.getUUID()),
						packet.locationalSoundPacketBuilder()
								.opusEncodedData(encodedData)
								.position(serverApi.createPosition(player.getX(), player.getY(), player.getZ()))
								.distance(8f)
								.build()
				);
			}
		}

		// Receivers
		server.execute(() -> {
			List<BlockPos> receivers = getGlobalRadioState(world).getReceivers();
			for (BlockPos receiverPos : receivers) {
				if (!world.isLoaded(receiverPos))
					continue;
				if (!(world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver))
					continue;
				if (!receiver.enabled || receiver.channel != senderChannel)
					continue;

				receiver.lastAudioPlayedTick = world.getGameTime();
				world.updateNeighborsAt(receiverPos, ModBlocks.RECEIVER_BLOCK.get());

				List<Player> playersInRange = world.getEntitiesOfClass(
						Player.class,
						AABB.ofSize(receiverPos.getCenter(), 64, 64, 64),
						entity -> true
				);
				for (Player entity : playersInRange) {
					if (serverApi.getConnectionOf(entity.getUUID()) == null)
						continue;
					if (entity != sender && isReceivingChannel(entity, senderChannel))
						continue;
					serverApi.sendLocationalSoundPacketTo(
							serverApi.getConnectionOf(entity.getUUID()),
							packet.locationalSoundPacketBuilder()
									.opusEncodedData(encodedData)
									.position(serverApi.createPosition(receiverPos.getX(), receiverPos.getY(), receiverPos.getZ()))
									.distance(32f)
									.build()
					);
				}
			}
		});
	}

	public static RadioAudioInstance transmitDataOnChannel(VoicechatServerApi serverApi, ServerLevel world, short[] audioData, int senderChannel) {
		return transmitDataOnChannel(serverApi, world, audioData, senderChannel, null);
	}

	public static RadioAudioInstance transmitDataOnChannel(VoicechatServerApi serverApi, ServerLevel world, short[] audioData, int senderChannel, Runnable onAudioStopped) {
		RadioAudioInstance audioInstance = new RadioAudioInstance(senderChannel, audioData, onAudioStopped);
		MinecraftServer server = world.getServer();

		GlobalRadioState globalRadioState = getGlobalRadioState(world);
		globalRadioState.audioManager.activeAudioInstances.add(audioInstance);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (serverApi.getConnectionOf(player.getUUID()) == null)
				continue;
			if (!isReceivingChannel(player, senderChannel))
				continue;
			globalRadioState.audioManager.startReceivingAudioInstance(world, player, audioInstance);
		}

		server.execute(() -> {
			List<BlockPos> receivers = globalRadioState.getReceivers();
			for (BlockPos receiverPos : receivers) {
				if (!world.isLoaded(receiverPos))
					continue;
				if (!(world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver))
					continue;
				if (!receiver.enabled || receiver.channel != senderChannel)
					continue;
				globalRadioState.audioManager.startReceivingAudioInstance(world, receiverPos, audioInstance);
			}
		});

		return audioInstance;
	}

	public static void transmitOnNearbyTransmitters(VoicechatServerApi serverApi, MicrophonePacket packet, ServerPlayer sender) {
		MinecraftServer server = sender.getServer();
		if (server == null)
			return;
		ServerLevel world = sender.serverLevel();
		List<BlockPos> transmitters = getGlobalRadioState(world).getTransmitters();
		Vec3 pos = sender.position();
		server.execute(() -> {
			double maxDist = ConfigManager.config != null ? ConfigManager.config.radioListeningDistance : 8;
			for (BlockPos transmitterPos : transmitters) {
				if (pos.distanceTo(transmitterPos.getCenter()) > maxDist)
					continue;
				if (!(world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitter))
					continue;
				if (!transmitter.enabled)
					continue;
				transmitOnChannel(serverApi, packet, sender, transmitter.channel);
			}
		});
	}

	public static boolean isReceivingChannel(Player player, int channel) {
		List<ItemStack> radios = getRadios(player);
		for (ItemStack stack : radios) {
			RadioComponent component = stack.get(ModDataComponents.RADIO.get());
			if (component == null)
				continue;
			if (!component.enabled() || !component.receive())
				continue;
			if (component.channel() != channel)
				continue;
			return true;
		}
		return false;
	}

	public static boolean isRadioEnabled(ItemStack stack) {
		RadioComponent component = stack.get(ModDataComponents.RADIO.get());
		return component != null && component.enabled();
	}

	public static boolean isRadioTransmitting(ItemStack stack) {
		RadioComponent component = stack.get(ModDataComponents.RADIO.get());
		return component != null && component.transmit();
	}

	public static int getRadioChannel(ItemStack stack) {
		RadioComponent component = stack.get(ModDataComponents.RADIO.get());
		return component != null ? component.channel() : 0;
	}

	public static List<ItemStack> getRadios(Player player) {
		List<List<ItemStack>> inventories = ImmutableList.of(player.getInventory().items, player.getInventory().offhand);
		List<ItemStack> radios = new ArrayList<>();
		for (List<ItemStack> inventory : inventories) {
			for (ItemStack stack : inventory) {
				if (!stack.is(ModItems.RADIO_ITEM.get()))
					continue;
				radios.add(stack);
			}
		}
		return radios;
	}

	public static GlobalRadioState getGlobalRadioState(ServerLevel world) {
		return world.getDataStorage().computeIfAbsent(GlobalRadioState.factory(), "globalRadios");
	}
}
