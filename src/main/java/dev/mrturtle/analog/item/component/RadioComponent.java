package dev.mrturtle.analog.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RadioComponent(boolean enabled, boolean transmit, boolean receive, int channel) {
	public static final Codec<RadioComponent> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
			Codec.BOOL.fieldOf("enabled").forGetter(RadioComponent::enabled),
			Codec.BOOL.fieldOf("transmit").forGetter(RadioComponent::transmit),
			Codec.BOOL.fieldOf("receive").forGetter(RadioComponent::receive),
			Codec.INT.fieldOf("channel").forGetter(RadioComponent::channel)
	).apply(instance, RadioComponent::new));

	public static final StreamCodec<ByteBuf, RadioComponent> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, RadioComponent::enabled,
			ByteBufCodecs.BOOL, RadioComponent::transmit,
			ByteBufCodecs.BOOL, RadioComponent::receive,
			ByteBufCodecs.VAR_INT, RadioComponent::channel,
			RadioComponent::new
	);

	public static final RadioComponent DEFAULT = new RadioComponent(false, false, false, 0);

	public RadioComponent withEnabled(boolean enabled) {
		return new RadioComponent(enabled, transmit, receive, channel);
	}

	public RadioComponent withTransmit(boolean transmit) {
		return new RadioComponent(enabled, transmit, receive, channel);
	}

	public RadioComponent withReceive(boolean receive) {
		return new RadioComponent(enabled, transmit, receive, channel);
	}

	public RadioComponent withChannel(int channel) {
		return new RadioComponent(enabled, transmit, receive, channel);
	}
}
