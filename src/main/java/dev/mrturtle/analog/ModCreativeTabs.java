package dev.mrturtle.analog;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Analog.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANALOG_TAB = CREATIVE_MODE_TABS.register(
			"analog",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.analog"))
					.icon(() -> new ItemStack(ModItems.RADIO_ITEM.get()))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.RADIO_ITEM.get());
						output.accept(ModItems.TRANSMITTER_ITEM.get());
						output.accept(ModItems.RECEIVER_ITEM.get());
					})
					.build()
	);
}
