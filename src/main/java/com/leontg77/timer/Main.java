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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

/**
 * Main class of the plugin.
 * 
 * @author LeonTG
 */
@SuppressWarnings("UnstableApiUsage")
public class Main extends JavaPlugin {
    public static final String PREFIX = "§cTimer §8» §7";
    
    @Override
    public void onEnable() {
        reloadConfig();

        LifecycleEventManager<Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> new TimerCommand(this, event.registrar()));
    }

    private TimerRunnable runnable = null;

    /**
     * Get the current runnable for the timer.
     *
     * @return The current runnable.
     */
    public TimerRunnable getRunnable() {
        return runnable;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if (getConfig().getConfigurationSection("bossbar") == null) {
            getConfig().set("bossbar.color", "pink");
            getConfig().set("bossbar.style", "solid");
            saveConfig();
        }

        if (runnable != null && runnable.getHandler() instanceof Listener) {
            HandlerList.unregisterAll((Listener) runnable.getHandler());
        }

        FileConfiguration config = getConfig();
        BarColor color = BarColor.valueOf(config.getString("bossbar.color", "pink").toUpperCase());
        BarStyle style = BarStyle.valueOf(config.getString("bossbar.style", "solid").toUpperCase());

        runnable = new TimerRunnable(this, new BossBarHandler(this, color, style));

        if(config.getConfigurationSection("timer") != null) {
            long endTimestamp = config.getLong("timer.last-end-time");
            String message = config.getString("timer.last-message");

            if(endTimestamp > 0 && message != null) {
                Instant endTime = Instant.ofEpochSecond(endTimestamp);

                if(endTime.isAfter(Instant.now())) {
                    getLogger().info("Resuming saved timer \"" + message + "\"");
                    getRunnable().startSendingMessage(message, endTime);
                }
            }
        }
    }
}
