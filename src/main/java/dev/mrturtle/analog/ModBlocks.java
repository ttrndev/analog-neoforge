package dev.mrturtle.analog;

import dev.mrturtle.analog.block.ReceiverBlock;
import dev.mrturtle.analog.block.TransmitterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Analog.MODID);

	public static final DeferredBlock<TransmitterBlock> TRANSMITTER_BLOCK = BLOCKS.register(
			"transmitter",
			() -> new TransmitterBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5f).noOcclusion())
	);

	public static final DeferredBlock<ReceiverBlock> RECEIVER_BLOCK = BLOCKS.register(
			"receiver",
			() -> new ReceiverBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5f).noOcclusion())
	);
}
