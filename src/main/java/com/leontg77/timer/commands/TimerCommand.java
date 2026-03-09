/*
 * Project: Timer
 * Class: com.leontg77.timer.commands.TimerCommand
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Leon Vaktskjold <leontg77@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.leontg77.timer.commands;

import com.leontg77.timer.Main;
import com.leontg77.timer.runnable.TimerRunnable;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.component;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.namespacedKey;

/**
 * Timer command class.
 *
 * @author LeonTG
 */
public final class TimerCommand {
    private final Main plugin;
    private static final String PERMISSION = "timer.manage";

    public TimerCommand(Main plugin, Commands commands) {
        this.plugin = plugin;

        TimerArgumentType timerArgumentType = new TimerArgumentType(plugin);

		LiteralCommandNode<CommandSourceStack> timerCommand = literal("timer")
                .requires(ctx -> ctx.getSender().hasPermission(PERMISSION))
                .then(literal("start")
                              .then(argument("id", namespacedKey())
                                            .then(literal("duration")
                                                          .then(argument("duration", integer(1))
                                                                        .then(argument("text", component())
                                                                                      .executes(ctx -> onStart(ctx, TimerType.DURATION)))))
                                            .then(literal("endtime")
                                                          .then(argument("endtime", longArg())
                                                                        .then(argument("text", component())
                                                                                      .executes(ctx -> onStart(ctx, TimerType.END_TIME)))))
                                            .then(literal("infinite")
                                                          .then(argument("text", component())
                                                                        .executes(ctx -> onStart(ctx, TimerType.INFINITE))))))
                .then(literal("edit")
                        .then(argument("timer", timerArgumentType)
                                .then(literal("color")
                                              .executes(ctx -> onResetColor(ctx, ctx.getArgument("timer", TimerRunnable.class)))
                                              .then(argument("color", new BossBarColorArgumentType())
                                                            .executes(ctx -> onSetColor(ctx, ctx.getArgument("timer", TimerRunnable.class)))))
                                .then(literal("style")
                                              .executes(ctx -> onResetStyle(ctx, ctx.getArgument("timer", TimerRunnable.class)))
                                              .then(argument("style", new BossBarOverlayArgumentType())
                                                            .executes(ctx -> onSetStyle(ctx, ctx.getArgument("timer", TimerRunnable.class)))))))
                .then(literal("cancel")
                              .then(argument("timer", timerArgumentType)
                                      .executes(ctx -> onCancel(ctx, ctx.getArgument("timer", TimerRunnable.class)))))
                .then(literal("clear").executes(this::onClear))
                .then(literal("defaultcolor")
                              .then(argument("color", new BossBarColorArgumentType())
                                            .executes(this::onSetDefaultColor)))
                .then(literal("defaultstyle")
                              .then(argument("style", new BossBarOverlayArgumentType())
                                            .executes(this::onSetDefaultStyle)))
                .then(literal("reload").executes(this::onReload))
                .build();

        commands.register(timerCommand, "Manage the bossbar timer");
    }

    private int onStart(CommandContext<CommandSourceStack> ctx, @NonNull TimerType type) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();

        NamespacedKey id = ctx.getArgument("id", NamespacedKey.class);
        Component text = ctx.getArgument("text", Component.class);

        Instant endTime = null;

        switch(type) {
            case DURATION -> endTime = Instant.now().plusSeconds(ctx.getArgument("duration", int.class));
            case END_TIME -> endTime = Instant.ofEpochSecond(ctx.getArgument("endtime", long.class));
        }

        if (plugin.getActiveTimer(id) != null) {
            throw new SimpleCommandExceptionType(
                            new LiteralMessage("A timer already exists with the ID '" + id + "'")).create();
        }

        plugin.createTimer(id, text, endTime);
        plugin.getLogger().info("Starting timer " + id + "(\"" + Main.plain.serialize(text) + "\"");
        sender.sendMessage(Component.text("Timer started"));

        return Command.SINGLE_SUCCESS;
    }

    private int onSetDefaultColor(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        BossBar.Color color = ctx.getArgument("color", BossBar.Color.class);

        plugin.setDefaultColor(color);
        sender.sendMessage(Component.text("Default timer style updated"));

        return Command.SINGLE_SUCCESS;
    }


    private int onSetDefaultStyle(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        BossBar.Overlay style = ctx.getArgument("style", BossBar.Overlay.class);

        plugin.setDefaultStyle(style);
        sender.sendMessage(Component.text("Default timer style updated"));

        return Command.SINGLE_SUCCESS;
    }

    private int onSetColor(CommandContext<CommandSourceStack> ctx, TimerRunnable timer) {
        CommandSender sender = ctx.getSource().getSender();
        BossBar.Color color = ctx.getArgument("color", BossBar.Color.class);

        timer.setColorOverride(color);
        sender.sendMessage(Component.text("Timer color updated"));

        return Command.SINGLE_SUCCESS;
    }

    private int onSetStyle(CommandContext<CommandSourceStack> ctx, TimerRunnable timer) {
        CommandSender sender = ctx.getSource().getSender();
        BossBar.Overlay style = ctx.getArgument("style", BossBar.Overlay.class);

        timer.setStyleOverride(style);
        sender.sendMessage(Component.text("Timer style updated"));

        return Command.SINGLE_SUCCESS;
    }

    private int onResetColor(CommandContext<CommandSourceStack> ctx, TimerRunnable timer) {
        CommandSender sender = ctx.getSource().getSender();

        timer.resetColor();
        sender.sendMessage(Component.text("Timer color reset"));

        return Command.SINGLE_SUCCESS;
    }

    private int onResetStyle(CommandContext<CommandSourceStack> ctx, TimerRunnable timer) {
        CommandSender sender = ctx.getSource().getSender();

        timer.resetStyle();
        sender.sendMessage(Component.text("Timer style reset"));

        return Command.SINGLE_SUCCESS;
    }

    private int onCancel(CommandContext<CommandSourceStack> ctx, TimerRunnable timer) {
        CommandSender sender = ctx.getSource().getSender();

        if (!plugin.hasActiveTimers()) {
            sender.sendMessage(Component.text("No timers are running").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        timer.cancel();
        sender.sendMessage(Component.text("Timer cancelled"));
        return Command.SINGLE_SUCCESS;
    }

    private int onClear(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!plugin.hasActiveTimers()) {
            sender.sendMessage(Component.text("No timers are running").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        plugin.getActiveTimers().values().forEach(TimerRunnable::cancel);
        sender.sendMessage(Component.text("Cancelled all timers"));
        return Command.SINGLE_SUCCESS;
    }

    private int onReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (plugin.hasActiveTimers()) {
            sender.sendMessage(Component.text("Cannot reload while a timer is running").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        plugin.reloadConfig();

        sender.sendMessage(Component.text("Timer config has been reloaded"));
        return Command.SINGLE_SUCCESS;
    }

    private enum TimerType {
        DURATION,
        END_TIME,
        INFINITE
    }
}