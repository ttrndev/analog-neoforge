package dev.mrturtle.analog;

import dev.mrturtle.analog.item.component.RadioComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
	public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Analog.MODID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<RadioComponent>> RADIO = DATA_COMPONENT_TYPES.registerComponentType(
			"radio",
			builder -> builder.persistent(RadioComponent.CODEC).networkSynchronized(RadioComponent.STREAM_CODEC)
	);
}
