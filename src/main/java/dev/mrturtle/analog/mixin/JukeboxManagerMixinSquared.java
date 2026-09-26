package dev.mrturtle.analog.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.mrturtle.analog.access.JukeboxManagerAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JukeboxSongPlayer.class, priority = 1500)
public abstract class JukeboxManagerMixinSquared {
    @Shadow public abstract boolean isPlaying();

    @Shadow private @Nullable Holder<JukeboxSong> song;

    @TargetHandler(
            mixin = "de.maxhenkel.audioplayer.mixin.JukeboxSongPlayerMixin",
            name = "tick"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"))
    public void tick(LevelAccessor world, BlockState state, CallbackInfo originalCi, CallbackInfo ci) {
        if (!isPlaying())
            return;
        if (song != null)
            return;
        ((JukeboxManagerAccessor) this).analog$makeNearbyTransmittersPlay(world, false);
    }
}
