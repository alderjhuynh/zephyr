package com.zephyr.client.fakeplayer;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

// Backport of 26.3's FakeClientConnection. 1.21.1 differences: send() takes
// PacketSendListener (not netty ChannelFutureListener).
public class FakeClientConnection extends Connection {
    public FakeClientConnection(PacketFlow flow) {
        super(flow);
        // Carpet uses ((ClientConnectionInterface)this).setChannel(new EmbeddedChannel())
        // to make isOpen()/isConnected() true and allow flush()/enderpearl teleport.
        // We don't have that mixin in zephyr, so set the private 'channel' field via reflection.
        try {
            java.lang.reflect.Field f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            // EmbeddedChannel has an eventLoop so Connection.flush() won't NPE
            f.set(this, new io.netty.channel.embedded.EmbeddedChannel());
        } catch (Exception e) {
            throw new RuntimeException("Failed to init fake channel", e);
        }
    }

    @Override
    public void flushChannel() {
        // No-op: with EmbeddedChannel flush would succeed, but avoid any pipeline work.
    }

    @Override
    public void tick() {
        // No-op: don't try to handle netty queue for fake connection
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener packetSendListener, boolean bl) {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    public boolean isOpen() {
        return true;
    }
}
