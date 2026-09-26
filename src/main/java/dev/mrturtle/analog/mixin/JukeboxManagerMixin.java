package dev.mrturtle.analog.mixin;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.AnalogPlugin;
import dev.mrturtle.analog.access.JukeboxManagerAccessor;
import dev.mrturtle.analog.audio.RadioAudioInstance;
import dev.mrturtle.analog.audio.assets.MusicAssetManager;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.util.RadioAudioUtil;
import dev.mrturtle.analog.util.RadioUtil;
import dev.mrturtle.analog.world.GlobalRadioState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxManagerMixin implements JukeboxManagerAccessor {
	@Shadow public abstract boolean isPlaying();

	@Shadow @Final private BlockPos blockPos;
	@Shadow private long ticksSinceSongStarted;
	@Unique
	private short[] cachedAudio = null;

	@Inject(method = "play", at = @At("TAIL"))
	public void play(LevelAccessor world, Holder<JukeboxSong> song, CallbackInfo ci) {
		if (world.isClientSide())
			return;

		if (!MusicAssetManager.recordsLoaded) {
			Player closestPlayer = world.getNearestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 8, EntitySelector.NO_SPECTATORS);
			if (closestPlayer != null)
				closestPlayer.displayClientMessage(Component.translatable("gui.analog.jukebox.asset_failure"), true);
			return;
		}

		ResourceLocation songId = song.value().soundEvent().value().getLocation();
		if (!songId.getNamespace().equals("minecraft"))
			return;

		cachedAudio = null;
		String songPath = "analog/records/%s.ogg".formatted(songId.getPath().replace("music_disc.", ""));
		try {
			cachedAudio = RadioAudioUtil.getAudioData(FMLPaths.CONFIGDIR.get().resolve(songPath));
		} catch (Exception e) {
			Analog.LOGGER.error("Failed to load music disc for playback from path {}", songPath);
			e.printStackTrace();
		}

		analog$makeNearbyTransmittersPlay(world, true);
	}

	@Inject(method = "stop", at = @At("TAIL"))
	public void stop(LevelAccessor world, BlockState state, CallbackInfo ci) {
		if (world.isClientSide())
			return;
		analog$makeNearbyTransmittersStop(world);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	public void tick(LevelAccessor world, BlockState state, CallbackInfo ci) {
		if (!isPlaying())
			return;
		analog$makeNearbyTransmittersPlay(world, false);
	}

	@Unique
	public void analog$makeNearbyTransmittersStop(LevelAccessor world) {
		if (!(world instanceof ServerLevel serverLevel))
			return;
		GlobalRadioState globalRadioState = RadioUtil.getGlobalRadioState(serverLevel);
		Vec3 center = blockPos.getCenter();
		for (BlockPos transmitterPos : globalRadioState.getTransmitters()) {
			double maxDist = ConfigManager.config != null ? ConfigManager.config.radioListeningDistance : 8;
			if (center.distanceTo(transmitterPos.getCenter()) > maxDist)
				continue;
			if (!(world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitter))
				continue;
			if (!transmitter.enabled)
				continue;

			globalRadioState.audioManager.stopTransmitter(transmitterPos, blockPos.immutable());
		}
	}

	@Unique
	public void analog$makeNearbyTransmittersPlay(LevelAccessor world, boolean overrideExisting) {
		if (cachedAudio == null || !(world instanceof ServerLevel serverLevel))
			return;

		GlobalRadioState globalRadioState = RadioUtil.getGlobalRadioState(serverLevel);
		Vec3 center = blockPos.getCenter();
		for (BlockPos transmitterPos : globalRadioState.getTransmitters()) {
			double maxDist = ConfigManager.config != null ? ConfigManager.config.radioListeningDistance : 8;
			if (center.distanceTo(transmitterPos.getCenter()) > maxDist)
				continue;
			if (!(world.getBlockEntity(transmitterPos) instanceof TransmitterBlockEntity transmitter))
				continue;
			if (!transmitter.enabled)
				continue;
			HashMap<BlockPos, RadioAudioInstance> audioInstances = globalRadioState.audioManager.transmitterAudioInstances.computeIfAbsent(transmitterPos, (playerEntity) -> new HashMap<>());
			if (!audioInstances.containsKey(blockPos.immutable()) || overrideExisting) {
				if (audioInstances.containsKey(blockPos.immutable()))
					globalRadioState.audioManager.stopTransmitter(transmitterPos, blockPos);

				int startIndex = (int) (2400 * ticksSinceSongStarted);

				RadioAudioInstance audioInstance = RadioUtil.transmitDataOnChannel(AnalogPlugin.API, serverLevel, cachedAudio, transmitter.channel);
				audioInstance.setCurrentIndex(startIndex);
				audioInstances.put(blockPos.immutable(), audioInstance);
			}
		}
	}

	@Unique
	public void analog$setCachedAudio(short[] audioData) {
		cachedAudio = audioData;
	}
}
