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
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Main class of the plugin.
 * 
 * @author LeonTG
 */
public class Main extends JavaPlugin implements Listener {
    public static final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private static Main instance;

    private final Map<NamespacedKey, TimerRunnable> activeTimers = new HashMap<>();
    private Placeholders expansion;

    @Override
    public void onEnable() {
        Main.instance = this;
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);

        LifecycleEventManager<Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> new TimerCommand(this, event.registrar()));
    }

    @Override
    public void onDisable() {
        ConfigurationSection timers = new MemoryConfiguration();
        getActiveTimers().forEach((key, value) -> {
            timers.setRichMessage(key + ".message", value.getMessage());

            Instant endTime = value.getEndTime();
            BossBar.Color colorOverride = value.getColorOverride();
            BossBar.Overlay styleOverride = value.getStyleOverride();

            if (endTime != null) {
                timers.set(key + ".end-time", endTime.getEpochSecond());
            }

            if (colorOverride != null) {
                timers.set(key + ".color", colorOverride.name());
            }

            if (styleOverride != null) {
                timers.set(key + ".style", styleOverride.name());
            }
        });

        getConfig().set("timers", timers);
        saveConfig();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        switch (event.getPlugin().getName()) {
            case "PlaceholderAPI" -> {
                getLogger().info("Registering PlaceholderAPI expansion");
                expansion = new Placeholders(this);
                expansion.register();
            }
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        switch (event.getPlugin().getName()) {
            case "PlaceholderAPI" -> {
                getLogger().info("Disabling PlaceholderAPI expansion");
                expansion = null;
            }
        }
    }

    private void updateActiveTimers() {
		activeTimers.entrySet().removeIf(entry -> !entry.getValue().isRunning());
    }

    /**
     * Get the current runnable for the timer.
     *
     * @return The current runnable.
     */
    public Map<NamespacedKey, TimerRunnable> getActiveTimers() {
        updateActiveTimers();
        return Collections.unmodifiableMap(activeTimers);
    }

    public @Nullable TimerRunnable getActiveTimer(NamespacedKey id) {
        updateActiveTimers();
        return activeTimers.get(id);
    }

    public boolean hasActiveTimers() {
        updateActiveTimers();
        return !activeTimers.isEmpty();
    }

    public TimerRunnable createTimer(NamespacedKey id, Component message, @Nullable Instant endTime) {
        updateActiveTimers();

        if (activeTimers.containsKey(id)) {
            throw new IllegalArgumentException("Timer " + id + " already exists");
        }

        getConfig().set("timers." + id + ".end-time", endTime != null ? endTime.getEpochSecond() : null);
        getConfig().setRichMessage("timers." + id + ".message", message);
        saveConfig();

        TimerRunnable timer = new TimerRunnable(id, message, endTime, new BossBarHandler());
        activeTimers.put(id, timer);
        return timer;
    }

    public TimerRunnable createTimer(NamespacedKey id, Component message, @Nullable Instant endTime,
                                     BossBar.@Nullable Color colorOverride, BossBar.@Nullable Overlay styleOverride) {
        updateActiveTimers();

        if (activeTimers.containsKey(id)) {
            throw new IllegalArgumentException("Timer " + id + " already exists");
        }

        getConfig().set("timers." + id + ".end-time", endTime != null ? endTime.getEpochSecond() : null);
        getConfig().setRichMessage("timers." + id + ".message", message);
        saveConfig();

        TimerRunnable timer = new TimerRunnable(id, message, endTime, new BossBarHandler(colorOverride, styleOverride));
        activeTimers.put(id, timer);
        return timer;
    }

    public void setDefaultColor(BossBar.Color color) {
        BossBarHandler.defaultColor = color;

        getConfig().set("bossbar.color", color.name());
        saveConfig();
    }

    public void setDefaultStyle(BossBar.Overlay overlay) {
        BossBarHandler.defaultStyle = overlay;

        getConfig().set("bossbar.style", overlay.name());
        saveConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if(hasActiveTimers()) {
            throw new IllegalStateException("Cannot reload while timer is running");
        }

        if (getConfig().getConfigurationSection("bossbar") == null) {
            getConfig().set("bossbar.color", BossBarHandler.defaultColor.name());
            getConfig().set("bossbar.style", BossBarHandler.defaultStyle.name());
            saveConfig();
        }

        FileConfiguration config = getConfig();

        try {
            BossBarHandler.defaultColor = BossBar.Color.valueOf(config.getString("bossbar.color", "pink").toUpperCase());
            BossBarHandler.defaultStyle = BossBar.Overlay.valueOf(config.getString("bossbar.style", "progress").toUpperCase());

            ConfigurationSection timers = config.getConfigurationSection("timers");

            if(timers != null) {
                Set<String> keys = timers.getKeys(false);

                for (String key : keys) {
                    NamespacedKey id = NamespacedKey.fromString(key);
                    ConfigurationSection timer = timers.getConfigurationSection(key);

                    if (timer != null) {
                        long endTimestamp = timer.getLong("end-time");
                        Component message = timer.getRichMessage("message");
                        String colorString = timer.getString("color");
                        String styleString = timer.getString("style");
                        BossBar.Color colorOverride = colorString != null ? BossBar.Color.valueOf(colorString.toUpperCase()) : null;
                        BossBar.Overlay styleOverride = styleString != null ? BossBar.Overlay.valueOf(styleString.toUpperCase()) : null;;

                        if(endTimestamp > 0 && message != null) {
                            Instant endTime = Instant.ofEpochSecond(endTimestamp);

                            if(endTime.isAfter(Instant.now())) {
                                getLogger().info("Resuming saved timer " + id + "(\"" + plain.serialize(message) + "\")");
                                createTimer(id, message, endTime, colorOverride, styleOverride);
                            }
                        }
                    }
                }
            } else if(config.getConfigurationSection("timer") != null) { // Old config
                NamespacedKey id = NamespacedKey.fromString("timer");
                long endTimestamp = config.getLong("timer.last-end-time");
                Component message = config.getRichMessage("timer.last-message");

                if(endTimestamp > 0 && message != null) {
                    Instant endTime = Instant.ofEpochSecond(endTimestamp);

                    if(endTime.isAfter(Instant.now())) {
                        getLogger().info("Resuming saved timer " + id + "(\"" + plain.serialize(message) + "\")");
                        createTimer(id, message, endTime);
                    }
                }
            }

            // Remove old config
            config.set("timer", null);
        } catch(Exception ex) {
            getLogger().log(Level.WARNING,"Failed to resume saved timers", ex);
        }
    }

    public static Main getInstance() {
        return Main.instance;
    }
}
