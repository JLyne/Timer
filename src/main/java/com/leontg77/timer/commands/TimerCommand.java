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
import com.leontg77.timer.handling.handlers.BossBarHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.component;

/**
 * Timer command class.
 *
 * @author LeonTG
 */
@SuppressWarnings("UnstableApiUsage")
public final class TimerCommand {
    private final Main plugin;
    private static final String PERMISSION = "timer.manage";

    public TimerCommand(Main plugin, Commands commands) {
        this.plugin = plugin;

		LiteralCommandNode<CommandSourceStack> timerCommand = literal("timer")
                .requires(ctx -> ctx.getSender().hasPermission(PERMISSION))
                .then(literal("start")
                              .then(literal("duration").then(argument("duration", integer(1))
                                            .then(argument("text", component())
                                                          .executes(ctx -> onStart(ctx, true)))))
                              .then(literal("endtime").then(argument("endtime", longArg())
                                            .then(argument("text", component())
                                                          .executes(ctx -> onStart(ctx, false))))))
                .then(literal("setstyle")
                              .then(argument("color", new BossBarColorArgumentType())
                                            .then(argument("style", new BossBarOverlayArgumentType())
                                                          .executes(this::onSetStyle))))
                .then(literal("cancel").executes(this::onCancel))
                .then(literal("reload").executes(this::onReload))
                .build();

        commands.register(timerCommand, "Manage the bossbar timer");
    }

    private int onStart(CommandContext<CommandSourceStack> ctx, @NotNull TimerType type) {
        CommandSender sender = ctx.getSource().getSender();

        if (plugin.getRunnable().isRunning()) {
            sender.sendMessage(Component.text("Timer is already running, cancel with /timer cancel.")
                                       .color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Component text = ctx.getArgument("text", Component.class);

        Instant endTime;

        if(isDuration) {
            endTime = Instant.now().plusSeconds(ctx.getArgument("duration", int.class));
        } else {
            endTime = Instant.ofEpochSecond(ctx.getArgument("endtime", long.class));
        }

        plugin.getConfig().set("timer.last-end-time", endTime.getEpochSecond());
        plugin.getConfig().setRichMessage("timer.last-message", text);
        plugin.saveConfig();

        plugin.getRunnable().startSendingMessage(text, endTime);
        plugin.getLogger().info("Starting timer for \"" + Main.plain.serialize(text) + "\"");
        sender.sendMessage(Component.text("Timer started.").color(NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int onSetStyle(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        BossBarHandler handler = (BossBarHandler) plugin.getRunnable().getHandler();
        BossBar.Color color = ctx.getArgument("color", BossBar.Color.class);
        BossBar.Overlay style = ctx.getArgument("style", BossBar.Overlay.class);

        handler.update(color, style);

        plugin.getConfig().set("bossbar.color", color.name());
        plugin.getConfig().set("bossbar.style", style.name());
        plugin.saveConfig();

        sender.sendMessage(Component.text("Timer style updated").color(NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;
    }

    private int onCancel(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!plugin.getRunnable().isRunning()) {
            sender.sendMessage(Component.text("No timer is running").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        plugin.getRunnable().cancel();
        sender.sendMessage(Component.text("Timer cancelled").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    public int onReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (plugin.getRunnable().isRunning()) {
            sender.sendMessage(Component.text("Cannot reload while a timer is running").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        plugin.getRunnable().cancel();
        plugin.reloadConfig();

        sender.sendMessage(Component.text("Timer config has been reloaded").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}