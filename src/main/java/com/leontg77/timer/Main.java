/*
 * Project: Timer
 * Class: com.leontg77.timer.Main
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

package com.leontg77.timer;

import com.leontg77.timer.commands.TimerCommand;
import com.leontg77.timer.handling.handlers.BossBarHandler;
import com.leontg77.timer.runnable.TimerRunnable;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.logging.Level;

/**
 * Main class of the plugin.
 * 
 * @author LeonTG
 */
@SuppressWarnings("UnstableApiUsage")
public class Main extends JavaPlugin {
    public static final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private static Main instance;

    private BossBar.Color bossBarColor = BossBar.Color.PINK;
    private BossBar.Overlay bossBarOverlay = BossBar.Overlay.PROGRESS;

    @Override
    public void onEnable() {
        Main.instance = this;
        reloadConfig();

        LifecycleEventManager<Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> new TimerCommand(this, event.registrar()));
    }

    private TimerRunnable activeTimer = null;

    /**
     * Get the current runnable for the timer.
     *
     * @return The current runnable.
     */
    public @Nullable TimerRunnable getActiveTimer() {
        if(activeTimer != null && !activeTimer.isRunning()) {
            activeTimer = null;
        }

        return activeTimer;
    }

    public TimerRunnable createTimer(Component message, @Nullable Instant endTime) {
        if(activeTimer != null) {
            throw new IllegalStateException("Timer is already running");
        }

        getConfig().set("timer.last-end-time", endTime != null ? endTime.getEpochSecond() : null);
        getConfig().setRichMessage("timer.last-message", message);
        saveConfig();

        activeTimer = new TimerRunnable(message, endTime, new BossBarHandler(bossBarColor, bossBarOverlay));
        return activeTimer;
    }

    public void setStyle(BossBar.Color color, BossBar.Overlay overlay) {
        bossBarColor = color;
        bossBarOverlay = overlay;

        getConfig().set("bossbar.color", color.name());
        getConfig().set("bossbar.style", overlay.name());
        saveConfig();

        if(activeTimer != null && activeTimer.getHandler() instanceof BossBarHandler bossBarHandler) {
            bossBarHandler.setStyle(color, overlay);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if(activeTimer != null) {
            throw new IllegalStateException("Cannot reload while timer is running");
        }

        if (getConfig().getConfigurationSection("bossbar") == null) {
            getConfig().set("bossbar.color", bossBarColor.name());
            getConfig().set("bossbar.style", bossBarOverlay.name());
            saveConfig();
        }

        FileConfiguration config = getConfig();

        try {
            bossBarColor = BossBar.Color.valueOf(config.getString("bossbar.color", "pink").toUpperCase());
            bossBarOverlay = BossBar.Overlay.valueOf(config.getString("bossbar.style", "progress").toUpperCase());

            if(config.getConfigurationSection("timer") != null) {
                long endTimestamp = config.getLong("timer.last-end-time");
                Component message = config.getRichMessage("timer.last-message");

                if(endTimestamp > 0 && message != null) {
                    Instant endTime = Instant.ofEpochSecond(endTimestamp);

                    if(endTime.isAfter(Instant.now())) {
                        getLogger().info("Resuming saved timer \"" + plain.serialize(message) + "\"");
                        createTimer(message, endTime);
                    }
                }
            }
        } catch(Exception ex) {
            getLogger().log(Level.WARNING,"Failed to resume saved timer", ex);
        }
    }

    public static Main getInstance() {
        return Main.instance;
    }
}
