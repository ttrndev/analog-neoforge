package dev.mrturtle.analog.block;

import dev.mrturtle.analog.ModBlockEntities;
import dev.mrturtle.analog.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReceiverBlockEntity extends BlockEntity {
	public boolean enabled = false;
	public int channel = 0;

	public long lastAudioPlayedTick = -100;

	public ReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RECEIVER.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReceiverBlockEntity receiver) {
		if (level.isClientSide())
			return;
		// Reset comparator output after 20 ticks of no receiving
		if (level.getGameTime() - receiver.lastAudioPlayedTick == 21) {
			level.updateNeighborsAt(pos, ModBlocks.RECEIVER_BLOCK.get());
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		enabled = tag.getBoolean("enabled");
		channel = tag.getInt("channel");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putBoolean("enabled", enabled);
		tag.putInt("channel", channel);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
