package com.github.games647.wrapperping;

import com.google.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import static com.github.games647.wrapperping.PomData.ARTIFACT_ID;
import static org.spongepowered.api.command.CommandResult.success;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.string;
import static org.spongepowered.api.text.Text.of;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

@Plugin(id = ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION
        , url = PomData.URL, description = PomData.DESCRIPTION)
public class WrapperPing {

    private static final String STOP_COMMAND = "stopwrapper:";
    private static final String RESTART_COMMAND = "restart:";
    private static final String SCHEDULE_COMMAND = "reschedulerestart:";
    private static final String HOLD_COMMAND = "hold:";

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configFile;

    private int wrapperPort = 1234;

    @Listener
    //During this state, the plugin gets ready for initialization. Logger and config
    public void onPreInit(GamePreInitializationEvent preInitEvent) {
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(configFile).build();
        if (Files.notExists(configFile)) {
            CommentedConfigurationNode rootNode = loader.createEmptyNode(ConfigurationOptions.defaults());
            rootNode.getNode("wrapper-port").setValue(1234);

            try {
                loader.save(rootNode);
            } catch (IOException ioEx) {
                logger.error("Failed to save config", ioEx);
            }

            logger.info("Set default config. Shutting down for configuring it");
            Sponge.getServer().shutdown();
        } else {
            try {
                wrapperPort = loader.load().getNode("wrapper-port").getInt(1234);
            } catch (IOException ioEx) {
                logger.error("Failed to load config", ioEx);
            }
        }
    }

    @Listener
    //Commands register + events
    public void onInit(GameInitializationEvent initEvent) {
        registerCommand("holdsrv", "Holds the server",
                (src, args) -> sendPacket(buildCommand(src, args, HOLD_COMMAND), src), wrapperArgs());

        registerCommand("restartsrv", "Restarts the server",
                (src, args) -> sendPacket(buildCommand(src, args, RESTART_COMMAND), src), wrapperArgs());

        registerCommand("reschedulerestart", "Reschedules the next restart time", (src, args) -> {
            String time = args.<String>getOne("time").get();
            String command = buildCommand(src, args, SCHEDULE_COMMAND) + ':' + time;

            sendPacket(command, src);
        }, wrapperArgs(string(of("time"))));

        registerCommand("stopwrapper", "Stop both the server and the Toolkit",
                ((src, args) -> sendPacket(buildCommand(src, args, STOP_COMMAND), src)), wrapperArgs());

        registerCommand("ping", "Requests a pong response from the plugin",
                (src, args) -> src.sendMessage(of(YELLOW, "Pong")));
    }

    private CommandElement[] wrapperArgs(CommandElement... args) {
        return Stream.of(optional(string(of("username"))), string(of("password")), args)
                .toArray(CommandElement[]::new);
    }

    private void registerCommand(String command, String description, BiConsumer<CommandSource, CommandContext> executor
            , CommandElement... argDef) {
        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(of(GREEN + description))
                .permission(ARTIFACT_ID + '.' + command)
                .arguments(argDef)
                .executor((src, args) -> {
                    executor.accept(src, args);
                    return success();
                })
                .build(), command);
    }

    private String buildCommand(CommandSource src, CommandContext args, String key) {
        Optional<String> username = args.getOne("username");
        String password = args.<String>getOne("password").get();
        return key + username.map(user -> user + ':').orElseGet(src::getName) + password;
    }

    private void sendPacket(String command, CommandSource source) {
        Task.builder().async().execute(new WrapperConnect(this, command, source.getName(), wrapperPort)).submit(this);
    }

    public Logger getLogger() {
        return logger;
    }
}
