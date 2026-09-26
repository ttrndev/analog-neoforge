package dev.mrturtle.analog.client.gui;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class RadioSelectChannelScreen extends Screen {
	private static final ResourceLocation SELECT_CHANNEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/gui/radio/select_channel.png");

	private final Screen parentScreen;
	private final int currentChannel;
	private final Consumer<Integer> onChannelSelected;

	private EditBox channelEditBox;
	private int leftPos;
	private int topPos;

	public RadioSelectChannelScreen(Screen parentScreen, int currentChannel, Consumer<Integer> onChannelSelected) {
		super(Component.translatable("gui.analog.radio.set_channel.title"));
		this.parentScreen = parentScreen;
		this.currentChannel = currentChannel;
		this.onChannelSelected = onChannelSelected;
	}

	@Override
	protected void init() {
		super.init();
		this.leftPos = (this.width - 176) / 2;
		this.topPos = (this.height - 64) / 2;

		this.channelEditBox = new EditBox(this.font, this.leftPos + 62, this.topPos + 24, 103, 12, Component.translatable("gui.analog.radio.set_channel.title"));
		this.channelEditBox.setBordered(false);
		this.channelEditBox.setTextColor(0xFFFFFF);
		this.channelEditBox.setMaxLength(3);
		this.channelEditBox.setValue(String.valueOf(currentChannel));
		this.channelEditBox.setResponder(val -> {
			String filtered = val.replaceAll("\\D", "");
			if (!filtered.isEmpty()) {
				try {
					int maxChannel = ConfigManager.config != null ? ConfigManager.config.maxRadioChannels - 1 : 99;
					int parsed = Integer.parseInt(filtered);
					if (parsed > maxChannel) {
						this.channelEditBox.setValue(String.valueOf(maxChannel));
					}
				} catch (NumberFormatException ignored) {}
			}
		});

		this.addWidget(this.channelEditBox);
		this.setInitialFocus(this.channelEditBox);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			applyAndClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		applyAndClose();
	}

	private void applyAndClose() {
		int channel = currentChannel;
		if (channelEditBox != null) {
			String text = channelEditBox.getValue().trim();
			if (!text.isEmpty()) {
				try {
					int maxChannel = ConfigManager.config != null ? ConfigManager.config.maxRadioChannels - 1 : 99;
					channel = Math.max(0, Math.min(maxChannel, Integer.parseInt(text)));
				} catch (NumberFormatException ignored) {}
			}
		}
		onChannelSelected.accept(channel);
		Minecraft.getInstance().setScreen(parentScreen);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.blit(SELECT_CHANNEL_TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 64, 176, 64);
		guiGraphics.drawString(this.font, this.title, this.leftPos + 60, this.topPos + 6, 0x3F3F3F, false);
		this.channelEditBox.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
