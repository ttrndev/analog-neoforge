package dev.mrturtle.analog.mixin;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.access.JukeboxManagerAccessor;
import dev.mrturtle.analog.util.RadioAudioUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity {
    @Shadow @Final private JukeboxSongPlayer jukeboxSongPlayer;

    public JukeboxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "setTheItem", at = @At("TAIL"))
    public void setTheItem(ItemStack stack, CallbackInfo ci) {
        if (level == null || level.isClientSide())
            return;

        boolean isAudioPlayerDisc = false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            if (customData.copyTag().hasUUID("CustomSound")) {
                isAudioPlayerDisc = true;
            }
        }

        Optional<Holder<JukeboxSong>> optionalSongEntry = JukeboxSong.fromStack(level.registryAccess(), stack);
        if (isAudioPlayerDisc && level.getServer() != null) {
            String songName = "%s".formatted(customData.copyTag().getUUID("CustomSound"));
            Path basePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve("audio_player_data");

            String songExtension = ".wav";
            if (!Files.exists(basePath.resolve(songName + songExtension)))
                songExtension = ".mp3";
            try {
                ((JukeboxManagerAccessor) jukeboxSongPlayer).analog$setCachedAudio(RadioAudioUtil.getAudioData(basePath.resolve(songName + songExtension)));
                ((JukeboxManagerAccessor) jukeboxSongPlayer).analog$makeNearbyTransmittersPlay(level, true);
            } catch (Exception e) {
                Analog.LOGGER.error("Failed to load a custom Audio Player music disc for playback from path {}{}", songName, songExtension);
                e.printStackTrace();
            }
        } else if (optionalSongEntry.isEmpty()) {
            ((JukeboxManagerAccessor) jukeboxSongPlayer).analog$makeNearbyTransmittersStop(level);
        }
    }
}
