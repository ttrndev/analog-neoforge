package dev.mrturtle.analog.client.gui;

import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ReceiverScreen extends Screen {
	private final ReceiverBlockEntity receiver;
	private int channel;
	private boolean enabled;

	private EditBox channelEditBox;
	private Button powerButton;

	public ReceiverScreen(ReceiverBlockEntity receiver) {
		super(Component.translatable("block.analog.receiver"));
		this.receiver = receiver;
		this.channel = receiver.channel;
		this.enabled = receiver.enabled;
	}

	@Override
	protected void init() {
		super.init();
		int centerX = this.width / 2;
		int startY = this.height / 2 - 40;

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
		).bounds(centerX - 60, startY + 30, 120, 20).build());
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

	private void syncChanges() {
		PacketDistributor.sendToServer(new ModNetwork.ReceiverConfigPayload(receiver.getBlockPos(), channel, enabled));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
