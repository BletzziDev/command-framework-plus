/*
 * Copyright 2020 Luiz Carlos Mourão Paes de Carvalho
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.saiintbrisson.bukkit.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.saiintbrisson.bukkit.command.command.BukkitCommand;
import me.saiintbrisson.bukkit.command.executor.BukkitCommandExecutor;
import me.saiintbrisson.bukkit.command.executor.BukkitCompleterExecutor;
import me.saiintbrisson.minecraft.command.CommandFrame;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Completer;
import me.saiintbrisson.minecraft.command.argument.AdapterMap;
import me.saiintbrisson.minecraft.command.argument.eval.MethodEvaluator;
import me.saiintbrisson.minecraft.command.command.CommandInfo;
import me.saiintbrisson.minecraft.command.exception.CommandException;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The BukkitFrame is the core of the framework,
 * it registers the commands, adapters {@link AdapterMap}
 * and message holders {@link MessageHolder}
 *
 * @author Luiz Carlos Mourão
 */
@Getter
public final class BukkitFrame implements CommandFrame<Plugin, CommandSender, BukkitCommand> {

    private final Plugin plugin;
    private final AdapterMap adapterMap;
    private final MessageHolder messageHolder;

    private final Map<String, BukkitCommand> commandMap;
    private final MethodEvaluator methodEvaluator;

    @Getter(AccessLevel.PRIVATE)
    private final CommandMap bukkitCommandMap;

    @Setter
    private Executor executor;

    /**
     * Creates a new BukkitFrame with the AdapterMap provided.
     *
     * @param plugin     Plugin
     * @param adapterMap AdapterMap
     */
    public BukkitFrame(@NonNull @NotNull Plugin plugin, @NonNull @NotNull AdapterMap adapterMap) {
        this.plugin = plugin;

        this.adapterMap = adapterMap;
        this.messageHolder = new MessageHolder();

        this.commandMap = new HashMap<>();
        this.methodEvaluator = new MethodEvaluator(adapterMap);

        try {
            final Server server = Bukkit.getServer();
            final Method mapMethod = server.getClass().getMethod("getCommandMap");

            this.bukkitCommandMap = (CommandMap) mapMethod.invoke(server);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new CommandException(exception);
        }
    }

    /**
     * Creates a new BukkitFrame with the default AdapterMap. <p>If the registerDefault is true,
     * it registers the default adapters for Bukkit.
     *
     * @param plugin          Plugin
     * @param registerDefault Boolean
     */
    public BukkitFrame(@NonNull @NotNull Plugin plugin, boolean registerDefault) {
        this(plugin, new AdapterMap(registerDefault));

        if (registerDefault) {
            registerAdapter(Player.class, Bukkit::getPlayer);
            registerAdapter(OfflinePlayer.class, Bukkit::getOfflinePlayer);
        }
    }

    /**
     * Creates a new BukkitFrame with the default AdapterMap
     * and default Bukkit adapters.
     *
     * @param plugin Plugin
     */
    public BukkitFrame(Plugin plugin) {
        this(plugin, true);
    }

    /**
     * Get a command by their name in the CommandMap
     *
     * @param name String
     * @return BukkitCommand
     */
    @Override
    public BukkitCommand getCommand(String name) {
        return getOrRegisterCommand(name, null);
    }

    private BukkitCommand getOrRegisterCommand(String name, Consumer<BukkitCommand> whenComplete) {
        String[] segments = name.split("\\.");
        String root = segments[0];

        BukkitCommand command = commandMap.get(root);
        if (command == null) {
            command = new BukkitCommand(this, root, 0);
            commandMap.put(root, command);

            if (segments.length > 1) {
                bukkitCommandMap.register(plugin.getName(), command);
            }
        }

        command = command.createRecursive(name);
        if (whenComplete != null) {
            whenComplete.accept(command);
        }

        if (command.getPosition() == 0) {
            bukkitCommandMap.register(plugin.getName(), command);
        }

	return command;
    }

    /**
     * Registers multiple command objects into the CommandMap.
     *
     * @param objects Object...
     */
    @Override
    public void registerCommands(Object... objects) {
        for (Object object : objects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                final Command command = method.getAnnotation(Command.class);
                if (command != null) {
                    registerCommand(new CommandInfo(command), new BukkitCommandExecutor(this, method, object));
                    continue;
                }

                Completer completer = method.getAnnotation(Completer.class);
                if (completer != null) {
                    registerCompleter(completer.name(), new BukkitCompleterExecutor(method, object));
                }
            }
        }
    }

    /**
     * Registers a command into the CommandMap
     *
     * @param commandInfo     CommandInfo
     * @param commandExecutor CommandExecutor
     */
    @Override
    public void registerCommand(CommandInfo commandInfo, CommandExecutor<CommandSender> commandExecutor) {
        getOrRegisterCommand(commandInfo.getName(), command -> {
            command.initCommand(commandInfo, commandExecutor);
        });
    }

    @Override
    public void registerCompleter(String name, CompleterExecutor<CommandSender> completerExecutor) {
        getOrRegisterCommand(name, command -> {
            command.initCompleter(completerExecutor);
        });
    }

    /**
     * Unregisters a command from the CommandMap by
     * the Command name.
     *
     * <p>Returns a boolean that depends if the
     * operation was successful</p>
     *
     * @param name String | Command name
     * @return boolean
     */
    @Override
    public boolean unregisterCommand(String name) {
        final BukkitCommand command = commandMap.remove(name);
        return command != null && command.unregister(bukkitCommandMap);
    }

    /**
     * Create a command name annotation placeholder
     */
    public void setNamePlaceholder(final String placeholder, final String value) {
        CommandInfo.getNamePlaceholders().put(placeholder, value);
    }
    public void setAliasesPlaceholder(final String placeholder, final String[] aliases) {
        CommandInfo.getAliasesPlaceholders().put(placeholder, aliases);
    }
}
