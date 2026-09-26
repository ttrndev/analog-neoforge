package dev.mrturtle.analog.item;

import dev.mrturtle.analog.client.AnalogClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RadioItem extends Item {
	public RadioItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			AnalogClient.openRadioScreen(stack, hand);
			return InteractionResultHolder.success(stack);
		}
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		tooltipComponents.add(Component.translatable("item.analog.radio.tooltip.open", Component.keybind("key.use").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
		tooltipComponents.add(Component.translatable("item.analog.radio.tooltip.transmit", Component.keybind("key.swapOffhand").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
	}
}
