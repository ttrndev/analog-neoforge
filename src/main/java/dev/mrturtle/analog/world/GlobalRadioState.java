package dev.mrturtle.analog.world;

import dev.mrturtle.analog.audio.RadioAudioManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GlobalRadioState extends SavedData {
	private final ArrayList<BlockPos> transmitterList;
	private final ArrayList<BlockPos> receiverList;

	public final RadioAudioManager audioManager = new RadioAudioManager();

	public GlobalRadioState() {
		transmitterList = new ArrayList<>();
		receiverList = new ArrayList<>();
	}

	public static SavedData.Factory<GlobalRadioState> factory() {
		return new SavedData.Factory<>(
				GlobalRadioState::new,
				GlobalRadioState::load,
				DataFixTypes.LEVEL
		);
	}

	public void createTransmitter(BlockPos pos) {
		transmitterList.add(pos);
		setDirty();
	}

	public void removeTransmitter(BlockPos pos) {
		transmitterList.remove(pos);
		audioManager.stopTransmitter(pos);
		setDirty();
	}

	public void createReceiver(BlockPos pos) {
		receiverList.add(pos);
		setDirty();
	}

	public void removeReceiver(BlockPos pos) {
		receiverList.remove(pos);
		audioManager.receiverTurnedOff(pos);
		setDirty();
	}

	public List<BlockPos> getTransmitters() {
		return transmitterList;
	}

	public List<BlockPos> getReceivers() {
		return receiverList;
	}

	public static GlobalRadioState load(CompoundTag tag, HolderLookup.Provider registries) {
		GlobalRadioState state = new GlobalRadioState();
		ListTag transmitterList = tag.getList("globalTransmitters", Tag.TAG_COMPOUND);
		for (int i = 0; i < transmitterList.size(); i++) {
			CompoundTag compound = transmitterList.getCompound(i);
			Optional<BlockPos> pos = NbtUtils.readBlockPos(compound, "pos");
			pos.ifPresent(state.transmitterList::add);
		}
		ListTag receiverList = tag.getList("globalReceivers", Tag.TAG_COMPOUND);
		for (int i = 0; i < receiverList.size(); i++) {
			CompoundTag compound = receiverList.getCompound(i);
			Optional<BlockPos> pos = NbtUtils.readBlockPos(compound, "pos");
			pos.ifPresent(state.receiverList::add);
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag transmitterList = new ListTag();
		for (BlockPos pos : this.transmitterList) {
			CompoundTag compound = new CompoundTag();
			compound.put("pos", NbtUtils.writeBlockPos(pos));
			transmitterList.add(compound);
		}
		tag.put("globalTransmitters", transmitterList);
		ListTag receiverList = new ListTag();
		for (BlockPos pos : this.receiverList) {
			CompoundTag compound = new CompoundTag();
			compound.put("pos", NbtUtils.writeBlockPos(pos));
			receiverList.add(compound);
		}
		tag.put("globalReceivers", receiverList);
		return tag;
	}
}
