package com.zephyr.client.fakeplayer;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.Nullable;
import io.netty.channel.ChannelFutureListener;

/**
 * Client-only fake connection. Mirrors {@code carpet.patches.FakeClientConnection}
 * but guarantees {@code isConnected/isOpen==true} without requiring carpet's
 * {@code ClientConnectionInterface} mixin. This keeps enderpearl teleport and
 * chunk tracking alive for singleplayer integrated-server fake players while
 * remaining a pure client class (never loaded on dedicated server).
 *
 * <p>Staying in {@code src/client/java} + gating spawn to {@code isLocalServer()}
 * ensures joining vanilla/multiplayer servers without the mod is unaffected.</p>
 */
public class FakeClientConnection extends Connection {
    public FakeClientConnection(PacketFlow flow) {
        super(flow);
        // Carpet uses ((ClientConnectionInterface)this).setChannel(new EmbeddedChannel())
        // to make isOpen()/isConnected() true and allow flush()/enderpearl teleport.
        // We don't have that mixin in zephyr, so set the private 'channel' field via reflection.
        try {
            java.lang.reflect.Field f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            // EmbeddedChannel has an eventLoop so Connection.flush() won't NPE (see crash-2026-09-13_04.03.32-server.txt:327)
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
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean bl) {
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

    // 26.2 mappings expose isConnected(); some versions also have isOpen(). Override both via reflection-safe override:
    // If the supertype uses isOpen(), this method will still satisfy isConnected. We also provide isOpen for compat.
    public boolean isOpen() {
        return true;
    }
}
