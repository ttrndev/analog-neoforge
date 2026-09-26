package dev.mrturtle.analog.api;

import dev.mrturtle.analog.AnalogPlugin;
import dev.mrturtle.analog.util.RadioAudioUtil;
import dev.mrturtle.analog.util.RadioUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

import java.nio.file.Path;
import java.util.List;

public class AnalogAPI {
	public static void playSoundOverChannel(ServerLevel world, ResourceLocation soundID, int channel) {
		Path path = ModList.get().getModContainerById(soundID.getNamespace())
				.orElseThrow()
				.getModInfo()
				.getOwningFile()
				.getFile()
				.findResource("radio_sounds/" + soundID.getPath() + ".wav");
		try {
			short[] audio = RadioAudioUtil.getAudioData(path);
			RadioUtil.transmitDataOnChannel(AnalogPlugin.API, world, audio, channel);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void playSoundsOverChannel(ServerLevel world, List<ResourceLocation> soundIDs, int channel) {
		ResourceLocation soundID = soundIDs.remove(0);
		Path path = ModList.get().getModContainerById(soundID.getNamespace())
				.orElseThrow()
				.getModInfo()
				.getOwningFile()
				.getFile()
				.findResource("radio_sounds/" + soundID.getPath() + ".wav");
		try {
			short[] audio = RadioAudioUtil.getAudioData(path);
			RadioUtil.transmitDataOnChannel(AnalogPlugin.API, world, audio, channel, soundIDs.isEmpty() ? null : () -> playSoundsOverChannel(world, soundIDs, channel));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
