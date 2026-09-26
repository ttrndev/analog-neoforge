package dev.mrturtle.analog.access;

import net.minecraft.world.level.LevelAccessor;

public interface JukeboxManagerAccessor {
    void analog$makeNearbyTransmittersPlay(LevelAccessor world, boolean overrideExisting);
    void analog$makeNearbyTransmittersStop(LevelAccessor world);
    void analog$setCachedAudio(short[] audioData);
}
