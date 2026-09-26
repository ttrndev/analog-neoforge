package dev.mrturtle.analog;

import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Analog.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransmitterBlockEntity>> TRANSMITTER = BLOCK_ENTITY_TYPES.register(
			"transmitter",
			() -> BlockEntityType.Builder.of(TransmitterBlockEntity::new, ModBlocks.TRANSMITTER_BLOCK.get()).build(null)
	);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReceiverBlockEntity>> RECEIVER = BLOCK_ENTITY_TYPES.register(
			"receiver",
			() -> BlockEntityType.Builder.of(ReceiverBlockEntity::new, ModBlocks.RECEIVER_BLOCK.get()).build(null)
	);
}
