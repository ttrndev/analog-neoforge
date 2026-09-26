package dev.mrturtle.analog;

import dev.mrturtle.analog.item.RadioItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Analog.MODID);

	public static final DeferredItem<RadioItem> RADIO_ITEM = ITEMS.register(
			"radio",
			() -> new RadioItem(new Item.Properties().stacksTo(1))
	);

	public static final DeferredItem<BlockItem> TRANSMITTER_ITEM = ITEMS.register(
			"transmitter",
			() -> new BlockItem(ModBlocks.TRANSMITTER_BLOCK.get(), new Item.Properties())
	);

	public static final DeferredItem<BlockItem> RECEIVER_ITEM = ITEMS.register(
			"receiver",
			() -> new BlockItem(ModBlocks.RECEIVER_BLOCK.get(), new Item.Properties())
	);
}
