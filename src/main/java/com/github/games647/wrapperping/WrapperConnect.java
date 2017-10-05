package com.github.games647.wrapperping;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class WrapperConnect implements Runnable {

    private static final int TIMEOUT = 700;
    private static final String RESPONSE_KEY = "response:";
    private static final int BUFFER_SIZE = Short.MAX_VALUE * 2;

    private final WrapperPing plugin;
    private final String src;
    private final String command;

    private final int port;

    public WrapperConnect(WrapperPing plugin, String src, String command, int port) {
        this.plugin = plugin;
        this.src = src;
        this.command = command;
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            InetAddress targetHost = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, targetHost, port);

            socket.send(packet);

            byte[] data = new byte[BUFFER_SIZE];
            DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
            try {
                socket.setSoTimeout(TIMEOUT);
                socket.receive(incomingPacket);

                String result = new String(data).trim();
                if ((RESPONSE_KEY + "success").equalsIgnoreCase(result)) {
                    sendMessage("Done!");
                } else if ((RESPONSE_KEY + "command_error").equalsIgnoreCase(result)) {
                    sendMessage("Invalid restart time");
                } else {
                    sendMessage("Invalid response");
                }
            } catch (SocketTimeoutException timeoutEx) {
                sendMessage("Received no data from the wrapper!");
            } catch (IOException ioEx) {
                plugin.getLogger().error("Failed to receive udp packet", ioEx);
            }
        } catch (IOException ioEx) {
            plugin.getLogger().error("Failed to send udp packet", ioEx);
        }
    }

    private void sendMessage(String content) {
        Task.builder().execute(() -> {
            Text message = Text.of(TextColors.RED, content);

            if ("CONSOLE".equalsIgnoreCase(src)) {
                Sponge.getServer().getConsole().sendMessage(message);
            } else {
                Optional<Player> player = Sponge.getServer().getPlayer(src);
                if (player.isPresent()) {
                    player.get().sendMessage(message);
                }
            }
        }).submit(plugin);
    }
}
