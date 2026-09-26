package dev.mrturtle.analog.client;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.ModDataComponents;
import dev.mrturtle.analog.ModItems;
import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import dev.mrturtle.analog.client.gui.RadioScreen;
import dev.mrturtle.analog.client.gui.ReceiverScreen;
import dev.mrturtle.analog.client.gui.TransmitterScreen;
import dev.mrturtle.analog.item.component.RadioComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Analog.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AnalogClient {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemProperties.register(
					ModItems.RADIO_ITEM.get(),
					ResourceLocation.fromNamespaceAndPath(Analog.MODID, "enabled"),
					(stack, level, entity, seed) -> {
						RadioComponent component = stack.get(ModDataComponents.RADIO.get());
						return component != null && component.enabled() ? 1.0F : 0.0F;
					}
			);
		});
	}

	public static void openRadioScreen(ItemStack stack, InteractionHand hand) {
		Minecraft.getInstance().setScreen(new RadioScreen(stack, hand));
	}

	public static void openTransmitterScreen(TransmitterBlockEntity transmitter) {
		Minecraft.getInstance().setScreen(new TransmitterScreen(transmitter));
	}

	public static void openReceiverScreen(ReceiverBlockEntity receiver) {
		Minecraft.getInstance().setScreen(new ReceiverScreen(receiver));
	}
}
