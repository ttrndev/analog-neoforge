package dev.mrturtle.analog.network;

import dev.mrturtle.analog.Analog;
import dev.mrturtle.analog.ModDataComponents;
import dev.mrturtle.analog.ModItems;
import dev.mrturtle.analog.block.ReceiverBlockEntity;
import dev.mrturtle.analog.block.TransmitterBlockEntity;
import dev.mrturtle.analog.item.component.RadioComponent;
import dev.mrturtle.analog.util.RadioUtil;
import dev.mrturtle.analog.world.GlobalRadioState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(Analog.MODID).versioned("1");

		registrar.playToServer(
				RadioConfigPayload.TYPE,
				RadioConfigPayload.STREAM_CODEC,
				ModNetwork::handleRadioConfig
		);

		registrar.playToServer(
				TransmitterConfigPayload.TYPE,
				TransmitterConfigPayload.STREAM_CODEC,
				ModNetwork::handleTransmitterConfig
		);

		registrar.playToServer(
				ReceiverConfigPayload.TYPE,
				ReceiverConfigPayload.STREAM_CODEC,
				ModNetwork::handleReceiverConfig
		);
	}

	public record RadioConfigPayload(boolean isMainHand, int channel, boolean enabled, boolean transmit, boolean receive) implements CustomPacketPayload {
		public static final Type<RadioConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Analog.MODID, "radio_config"));

		public static final StreamCodec<ByteBuf, RadioConfigPayload> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, RadioConfigPayload::isMainHand,
				ByteBufCodecs.VAR_INT, RadioConfigPayload::channel,
				ByteBufCodecs.BOOL, RadioConfigPayload::enabled,
				ByteBufCodecs.BOOL, RadioConfigPayload::transmit,
				ByteBufCodecs.BOOL, RadioConfigPayload::receive,
				RadioConfigPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TransmitterConfigPayload(BlockPos pos, int channel, boolean enabled) implements CustomPacketPayload {
		public static final Type<TransmitterConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Analog.MODID, "transmitter_config"));

		public static final StreamCodec<ByteBuf, TransmitterConfigPayload> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, TransmitterConfigPayload::pos,
				ByteBufCodecs.VAR_INT, TransmitterConfigPayload::channel,
				ByteBufCodecs.BOOL, TransmitterConfigPayload::enabled,
				TransmitterConfigPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ReceiverConfigPayload(BlockPos pos, int channel, boolean enabled) implements CustomPacketPayload {
		public static final Type<ReceiverConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Analog.MODID, "receiver_config"));

		public static final StreamCodec<ByteBuf, ReceiverConfigPayload> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, ReceiverConfigPayload::pos,
				ByteBufCodecs.VAR_INT, ReceiverConfigPayload::channel,
				ByteBufCodecs.BOOL, ReceiverConfigPayload::enabled,
				ReceiverConfigPayload::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static void handleRadioConfig(RadioConfigPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) {
			InteractionHand hand = payload.isMainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(ModItems.RADIO_ITEM.get())) {
				RadioComponent newComponent = new RadioComponent(payload.enabled(), payload.transmit(), payload.receive(), payload.channel());
				stack.set(ModDataComponents.RADIO.get(), newComponent);
			}
		}
	}

	private static void handleTransmitterConfig(TransmitterConfigPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) < 64.0) {
				if (level.getBlockEntity(payload.pos()) instanceof TransmitterBlockEntity transmitter) {
					transmitter.channel = payload.channel();
					transmitter.enabled = payload.enabled();

					GlobalRadioState globalRadioState = RadioUtil.getGlobalRadioState(level);
					if (!transmitter.enabled) {
						globalRadioState.audioManager.stopTransmitter(payload.pos());
					} else {
						globalRadioState.audioManager.changeTransmitterChannel(payload.pos(), transmitter.channel);
					}
					transmitter.setChanged();
					level.sendBlockUpdated(payload.pos(), transmitter.getBlockState(), transmitter.getBlockState(), 3);
				}
			}
		}
	}

	private static void handleReceiverConfig(ReceiverConfigPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) < 64.0) {
				if (level.getBlockEntity(payload.pos()) instanceof ReceiverBlockEntity receiver) {
					receiver.channel = payload.channel();
					receiver.enabled = payload.enabled();
					receiver.setChanged();
					level.sendBlockUpdated(payload.pos(), receiver.getBlockState(), receiver.getBlockState(), 3);
				}
			}
		}
	}
}
