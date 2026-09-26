package dev.mrturtle.analog.client.gui;

import dev.mrturtle.analog.ModDataComponents;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.item.component.RadioComponent;
import dev.mrturtle.analog.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RadioScreen extends Screen {
	private final ItemStack radioStack;
	private final boolean isMainHand;
	private int channel;
	private boolean enabled;
	private boolean transmit;
	private boolean receive;

	private EditBox channelEditBox;
	private Button powerButton;
	private Button transmitButton;
	private Button receiveButton;

	public RadioScreen(ItemStack radioStack, InteractionHand hand) {
		super(Component.translatable("item.analog.radio"));
		this.radioStack = radioStack;
		this.isMainHand = hand == InteractionHand.MAIN_HAND;

		RadioComponent component = radioStack.getOrDefault(ModDataComponents.RADIO.get(), RadioComponent.DEFAULT);
		this.channel = component.channel();
		this.enabled = component.enabled();
		this.transmit = component.transmit();
		this.receive = component.receive();
	}

	@Override
	protected void init() {
		super.init();
		int centerX = this.width / 2;
		int startY = this.height / 2 - 60;

		// Channel controls: [-] [ EditBox ] [+]
		addRenderableWidget(Button.builder(Component.literal("-"), b -> setChannel(channel - 1))
				.bounds(centerX - 60, startY, 20, 20)
				.build());

		channelEditBox = new EditBox(this.font, centerX - 35, startY, 70, 20, Component.translatable("gui.analog.radio.set_channel"));
		channelEditBox.setValue(String.format("%02d", channel));
		channelEditBox.setResponder(val -> {
			try {
				int parsed = Integer.parseInt(val.trim());
				int maxChannel = ConfigManager.config != null ? ConfigManager.config.maxRadioChannels - 1 : 99;
				if (parsed >= 0 && parsed <= maxChannel) {
					channel = parsed;
					syncChanges();
				}
			} catch (NumberFormatException ignored) {}
		});
		addRenderableWidget(channelEditBox);

		addRenderableWidget(Button.builder(Component.literal("+"), b -> setChannel(channel + 1))
				.bounds(centerX + 40, startY, 20, 20)
				.build());

		// Power button
		powerButton = addRenderableWidget(Button.builder(
				getPowerText(),
				b -> {
					enabled = !enabled;
					powerButton.setMessage(getPowerText());
					syncChanges();
				}
		).bounds(centerX - 60, startY + 25, 120, 20).build());

		// Receive button
		receiveButton = addRenderableWidget(Button.builder(
				getReceiveText(),
				b -> {
					receive = !receive;
					receiveButton.setMessage(getReceiveText());
					syncChanges();
				}
		).bounds(centerX - 60, startY + 50, 120, 20).build());

		// Transmit button
		transmitButton = addRenderableWidget(Button.builder(
				getTransmitText(),
				b -> {
					transmit = !transmit;
					transmitButton.setMessage(getTransmitText());
					syncChanges();
				}
		).bounds(centerX - 60, startY + 75, 120, 20).build());
	}

	private void setChannel(int newChannel) {
		int maxChannel = ConfigManager.config != null ? ConfigManager.config.maxRadioChannels - 1 : 99;
		channel = Math.max(0, Math.min(maxChannel, newChannel));
		channelEditBox.setValue(String.format("%02d", channel));
		syncChanges();
	}

	private Component getPowerText() {
		return Component.translatable(enabled ? "gui.analog.radio.turn_off" : "gui.analog.radio.turn_on");
	}

	private Component getReceiveText() {
		return Component.translatable(receive ? "gui.analog.radio.stop_receiving" : "gui.analog.radio.start_receiving");
	}

	private Component getTransmitText() {
		return Component.translatable(transmit ? "gui.analog.radio.stop_transmitting" : "gui.analog.radio.start_transmitting");
	}

	private void syncChanges() {
		PacketDistributor.sendToServer(new ModNetwork.RadioConfigPayload(isMainHand, channel, enabled, transmit, receive));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
