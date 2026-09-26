package dev.mrturtle.analog.client.gui;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import dev.mrturtle.analog.config.ConfigManager;
import dev.mrturtle.analog.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

public class TransmitterScreen extends Screen {
	private static final ResourceLocation DISPENSER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");
	private static final ResourceLocation TRANSMITTER_OVERLAY = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/gui/radio/transmitter.png");
	private static final ResourceLocation NUMBERS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/gui/radio/numbers.png");

	private static final ResourceLocation CHANNEL_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/item/gui/radio/channel_down_button.png");
	private static final ResourceLocation CHANNEL_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/item/gui/radio/channel_up_button.png");
	private static final ResourceLocation ENABLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/item/gui/radio/enable_button.png");
	private static final ResourceLocation DISABLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Analog.MODID, "textures/item/gui/radio/disable_button.png");

	private final TransmitterBlockEntity transmitter;
	private int channel;
	private boolean enabled;

	private int leftPos;
	private int topPos;

	public TransmitterScreen(TransmitterBlockEntity transmitter) {
		super(Component.translatable("block.analog.transmitter"));
		this.transmitter = transmitter;
		this.channel = transmitter.channel;
		this.enabled = transmitter.enabled;
	}

	@Override
	protected void init() {
		super.init();
		this.leftPos = (this.width - 176) / 2;
		this.topPos = (this.height - 166) / 2;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int slot0X = leftPos + 62, slot0Y = topPos + 17;
			int slot1X = leftPos + 80, slot1Y = topPos + 17;
			int slot2X = leftPos + 98, slot2Y = topPos + 17;
			int slot4X = leftPos + 80, slot4Y = topPos + 35;

			if (isHovering(slot0X, slot0Y, mouseX, mouseY)) {
				playClickSound();
				setChannel(channel - 1);
				return true;
			}
			if (isHovering(slot1X, slot1Y, mouseX, mouseY)) {
				playClickSound();
				Minecraft.getInstance().setScreen(new RadioSelectChannelScreen(this, channel, this::setChannel));
				return true;
			}
			if (isHovering(slot2X, slot2Y, mouseX, mouseY)) {
				playClickSound();
				setChannel(channel + 1);
				return true;
			}
			if (isHovering(slot4X, slot4Y, mouseX, mouseY)) {
				playClickSound();
				enabled = !enabled;
				syncChanges();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean isHovering(int x, int y, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
	}

	private void playClickSound() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	public void setChannel(int newChannel) {
		int maxChannel = ConfigManager.config != null ? ConfigManager.config.maxRadioChannels - 1 : 99;
		channel = Math.max(0, Math.min(maxChannel, newChannel));
		syncChanges();
	}

	private void syncChanges() {
		PacketDistributor.sendToServer(new ModNetwork.TransmitterConfigPayload(transmitter.getBlockPos(), channel, enabled));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		// 1. Render Dispenser background (176x166)
		guiGraphics.blit(DISPENSER_TEXTURE, leftPos, topPos, 0, 0, 176, 166);

		// 2. Render Device Overlay (transmitter.png, 62x64)
		guiGraphics.blit(TRANSMITTER_OVERLAY, leftPos + 57, topPos + 11, 0, 0, 62, 64, 62, 64);

		// 3. Render Channel LED Digits
		renderChannelDigits(guiGraphics, leftPos + 81, topPos + 21, channel);

		// 4. Render 16x16 Pixel Art Buttons
		int slot0X = leftPos + 62, slot0Y = topPos + 17;
		int slot1X = leftPos + 80, slot1Y = topPos + 17;
		int slot2X = leftPos + 98, slot2Y = topPos + 17;
		int slot4X = leftPos + 80, slot4Y = topPos + 35;

		guiGraphics.blit(CHANNEL_DOWN_TEXTURE, slot0X, slot0Y, 0, 0, 16, 16, 16, 16);
		guiGraphics.blit(CHANNEL_UP_TEXTURE, slot2X, slot2Y, 0, 0, 16, 16, 16, 16);
		guiGraphics.blit(enabled ? DISABLE_TEXTURE : ENABLE_TEXTURE, slot4X, slot4Y, 0, 0, 16, 16, 16, 16);

		// 5. Render Slot Hover Highlight & Tooltips
		Component hoveredTooltip = null;

		if (isHovering(slot0X, slot0Y, mouseX, mouseY)) {
			renderSlotHighlight(guiGraphics, slot0X, slot0Y);
			hoveredTooltip = Component.translatable("gui.analog.radio.channel_down");
		} else if (isHovering(slot1X, slot1Y, mouseX, mouseY)) {
			renderSlotHighlight(guiGraphics, slot1X, slot1Y);
			hoveredTooltip = Component.translatable("gui.analog.radio.set_channel");
		} else if (isHovering(slot2X, slot2Y, mouseX, mouseY)) {
			renderSlotHighlight(guiGraphics, slot2X, slot2Y);
			hoveredTooltip = Component.translatable("gui.analog.radio.channel_up");
		} else if (isHovering(slot4X, slot4Y, mouseX, mouseY)) {
			renderSlotHighlight(guiGraphics, slot4X, slot4Y);
			hoveredTooltip = Component.translatable(enabled ? "gui.analog.radio.turn_off" : "gui.analog.radio.turn_on");
		}

		if (hoveredTooltip != null) {
			guiGraphics.renderTooltip(this.font, hoveredTooltip, mouseX, mouseY);
		}
	}

	private void renderSlotHighlight(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.fillGradient(x, y, x + 16, y + 16, 0x80FFFFFF, 0x80FFFFFF);
	}

	private void renderChannelDigits(GuiGraphics guiGraphics, int x, int y, int ch) {
		String text = String.valueOf(ch);
		if (text.length() == 1)
			text = "0" + text;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c >= '0' && c <= '9') {
				int digit = c - '0';
				guiGraphics.blit(NUMBERS_TEXTURE, x + i * 8, y, digit * 8, 0, 8, 7, 128, 7);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
