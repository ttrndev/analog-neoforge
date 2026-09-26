package dev.mrturtle.analog.audio;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.mrturtle.analog.AnalogPlugin;
import dev.mrturtle.analog.ModBlocks;
import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.util.RadioUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RadioAudioManager {
	public final ArrayList<RadioAudioInstance> activeAudioInstances = new ArrayList<>();
	public final HashMap<BlockPos, HashMap<BlockPos, RadioAudioInstance>> transmitterAudioInstances = new HashMap<>();
	public final HashMap<BlockPos, ArrayList<ReceiverAudioData>> receiverAudioPlayers = new HashMap<>();
	public final HashMap<ServerPlayer, ArrayList<PlayerAudioData>> playerAudioPlayers = new HashMap<>();

	public void tick(ServerLevel world) {
		// Remove audio instances that are done
		activeAudioInstances.removeIf((audioInstance) -> audioInstance.isDone);

		for (ServerPlayer player : world.players()) {
			for (RadioAudioInstance audioInstance : activeAudioInstances) {
				if (RadioUtil.isReceivingChannel(player, audioInstance.channel))
					playerReceiverTurnedOn(player, audioInstance.channel);
				else
					playerReceiverTurnedOff(player, audioInstance.channel);
			}

			if (playerAudioPlayers.containsKey(player)) {
				ArrayList<PlayerAudioData> audioPlayers = playerAudioPlayers.get(player);
				// Remove audio players that are done or invalid
				audioPlayers.forEach((audioData) -> {
					if (audioData.channel != audioData.instance.channel || audioData.instance.isDone)
						audioData.audioPlayer.stopPlaying();
				});
				audioPlayers.removeIf((audioData) -> audioData.audioPlayer.isStopped());

				for (PlayerAudioData audioData : audioPlayers) {
					audioData.audioChannel.updateLocation(AnalogPlugin.API.createPosition(player.getX(), player.getY(), player.getZ()));
				}
			}
		}

		// Remove audio players that are done or invalid
		for (Map.Entry<BlockPos, ArrayList<ReceiverAudioData>> entry : receiverAudioPlayers.entrySet()) {
			ArrayList<ReceiverAudioData> audioPlayers = entry.getValue();

			audioPlayers.forEach((audioData) -> {
				if (audioData.channel != audioData.instance.channel || audioData.instance.isDone)
					audioData.audioPlayer.stopPlaying();
			});
			audioPlayers.removeIf((audioData) -> audioData.audioPlayer.isStopped());

			if (!audioPlayers.isEmpty()) {
				BlockPos receiverPos = entry.getKey();
				if (world.isLoaded(receiverPos)) {
					if (world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver) {
						receiver.lastAudioPlayedTick = world.getGameTime();
						world.updateNeighborsAt(receiverPos, ModBlocks.RECEIVER_BLOCK.get());
					}
				}
			}
		}

		for (BlockPos receiverPos : RadioUtil.getGlobalRadioState(world).getReceivers()) {
			if (!world.isLoaded(receiverPos))
				continue;
			if (!(world.getBlockEntity(receiverPos) instanceof ReceiverBlockEntity receiver))
				continue;
			if (!receiver.enabled)
				continue;

			if (!receiverAudioPlayers.containsKey(receiverPos)) {
				receiverTurnedOn(world, receiverPos, receiver.channel);
			} else {
				ArrayList<ReceiverAudioData> audioPlayers = receiverAudioPlayers.get(receiverPos);
				for (RadioAudioInstance instance : activeAudioInstances) {
					if (instance.channel != receiver.channel)
						continue;

					boolean alreadyPlaying = false;
					for (ReceiverAudioData audioPlayer : audioPlayers) {
						if (audioPlayer.instance != instance)
							continue;
						alreadyPlaying = true;
					}
					if (alreadyPlaying)
						continue;

					startReceivingAudioInstance(world, receiverPos, instance);
				}
			}
		}
	}

	public void receiverTurnedOn(ServerLevel world, BlockPos pos, int receivingChannel) {
		for (RadioAudioInstance instance : activeAudioInstances) {
			if (instance.channel != receivingChannel)
				continue;
			startReceivingAudioInstance(world, pos, instance);
		}
	}

	public void receiverTurnedOff(BlockPos pos) {
		ArrayList<ReceiverAudioData> audioPlayers = receiverAudioPlayers.remove(pos);
		if (audioPlayers == null)
			return;
		for (ReceiverAudioData audioData : audioPlayers) {
			if (audioData.audioPlayer.isPlaying())
				audioData.audioPlayer.stopPlaying();
		}
		receiverAudioPlayers.remove(pos);
	}

	public void playerReceiverTurnedOn(ServerPlayer player, int receivingChannel) {
		ArrayList<PlayerAudioData> audioPlayers = playerAudioPlayers.computeIfAbsent(player, (playerEntity) -> new ArrayList<>());

		for (RadioAudioInstance instance : activeAudioInstances) {
			if (instance.channel != receivingChannel)
				continue;
			boolean alreadyExists = false;
			for (PlayerAudioData audioData : audioPlayers) {
				if (audioData.instance == instance) {
					alreadyExists = true;
					break;
				}
			}
			if (!alreadyExists)
				startReceivingAudioInstance(player.serverLevel(), player, instance);
		}
	}

	public void playerReceiverTurnedOff(ServerPlayer player, int receivingChannel) {
		ArrayList<PlayerAudioData> audioPlayers = playerAudioPlayers.get(player);
		if (audioPlayers == null)
			return;

		ArrayList<PlayerAudioData> toBeRemoved = new ArrayList<>();
		for (PlayerAudioData audioData : audioPlayers) {
			if (audioData.channel == receivingChannel) {
				audioData.audioPlayer.stopPlaying();
				toBeRemoved.add(audioData);
			}
		}
		for (PlayerAudioData audioData : toBeRemoved) {
			audioPlayers.remove(audioData);
		}
		playerAudioPlayers.remove(player);
	}

	public void startReceivingAudioInstance(ServerLevel world, BlockPos pos, RadioAudioInstance instance) {
		VoicechatServerApi serverApi = AnalogPlugin.API;

		LocationalAudioChannel channel = serverApi.createLocationalAudioChannel(UUID.randomUUID(), serverApi.fromServerLevel(world), serverApi.createPosition(pos.getCenter().x, pos.getCenter().y, pos.getCenter().z));
		if (channel == null)
			return;
		// Receivers play arbitrary audio such as music for triple the distance of voices
		channel.setDistance(24f);
		channel.setCategory(AnalogPlugin.RADIO_CATEGORY);

		AudioPlayer audioPlayer = serverApi.createAudioPlayer(channel, serverApi.createEncoder(), instance.audioSupplier);
		audioPlayer.startPlaying();

		ArrayList<ReceiverAudioData> audioPlayers = receiverAudioPlayers.computeIfAbsent(pos, (blockPos) -> new ArrayList<>());
		audioPlayers.add(new ReceiverAudioData(audioPlayer, instance));
	}

	public void startReceivingAudioInstance(ServerLevel world, ServerPlayer player, RadioAudioInstance instance) {
		VoicechatServerApi serverApi = AnalogPlugin.API;

		LocationalAudioChannel channel = serverApi.createLocationalAudioChannel(UUID.randomUUID(), serverApi.fromServerLevel(world), serverApi.createPosition(player.getX(), player.getY(), player.getZ()));
		if (channel == null)
			return;
		channel.setDistance(8f);
		channel.setCategory(AnalogPlugin.RADIO_CATEGORY);

		AudioPlayer audioPlayer = serverApi.createAudioPlayer(channel, serverApi.createEncoder(), instance.audioSupplier);
		audioPlayer.startPlaying();

		ArrayList<PlayerAudioData> audioPlayers = playerAudioPlayers.computeIfAbsent(player, (playerEntity) -> new ArrayList<>());
		audioPlayers.add(new PlayerAudioData(audioPlayer, channel, instance));
	}

	public void changeTransmitterChannel(BlockPos pos, int newChannel) {
		HashMap<BlockPos, RadioAudioInstance> audioInstances = transmitterAudioInstances.get(pos);
		if (audioInstances == null)
			return;
		for (RadioAudioInstance audioInstance : audioInstances.values()) {
			audioInstance.channel = newChannel;
		}
	}

	public void stopTransmitter(BlockPos pos, BlockPos jukeboxPos) {
		HashMap<BlockPos, RadioAudioInstance> audioInstances = transmitterAudioInstances.get(pos);
		if (audioInstances == null)
			return;
		if (audioInstances.containsKey(jukeboxPos)) {
			stopAudioInstance(audioInstances.get(jukeboxPos));
			audioInstances.remove(jukeboxPos);
		}
	}

	public void stopTransmitter(BlockPos pos) {
		HashMap<BlockPos, RadioAudioInstance> audioInstances = transmitterAudioInstances.get(pos);
		if (audioInstances == null)
			return;
		for (RadioAudioInstance instance : audioInstances.values()) {
			stopAudioInstance(instance);
		}
		audioInstances.clear();
	}

	public void stopAudioInstance(RadioAudioInstance instance) {
		instance.interrupt();

		for (ArrayList<ReceiverAudioData> audioPlayers : receiverAudioPlayers.values()) {
			for (ReceiverAudioData audioData : audioPlayers) {
				audioData.audioPlayer.stopPlaying();
			}
		}

		for (ArrayList<PlayerAudioData> audioPlayers : playerAudioPlayers.values()) {
			for (PlayerAudioData audioData : audioPlayers) {
				audioData.audioPlayer.stopPlaying();
			}
		}
	}
}
