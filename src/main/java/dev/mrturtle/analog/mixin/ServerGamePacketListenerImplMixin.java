package dev.mrturtle.analog.mixin;

import dev.mrturtle.analog.ModDataComponents;
import dev.mrturtle.analog.ModItems;
import dev.mrturtle.analog.item.component.RadioComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	public void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
		if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND)
			return;
		ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!stack.is(ModItems.RADIO_ITEM.get()))
			return;
		RadioComponent component = stack.getOrDefault(ModDataComponents.RADIO.get(), RadioComponent.DEFAULT);
		boolean isTransmitting = component.transmit();
		stack.set(ModDataComponents.RADIO.get(), component.withTransmit(!isTransmitting));
		int channel = component.channel();
		player.displayClientMessage(Component.translatable(!isTransmitting ? "gui.analog.radio.started_transmitting" : "gui.analog.radio.stopped_transmitting", channel), true);
		ci.cancel();
	}
}
