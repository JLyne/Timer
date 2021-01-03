/*
 * Project: Timer
 * Class: com.leontg77.timer.handling.handlers.BossBarHandler
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

package com.leontg77.timer.handling.handlers;

import com.leontg77.timer.Main;
import com.leontg77.timer.handling.TimerHandler;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Boss bar timer handler.
 *
 * @author LeonTG
 */
public class BossBarHandler implements TimerHandler, Listener {
    private final Main plugin;

    public BossBarHandler(Main plugin, String color, String style) {
        this.plugin = plugin;
        this.color = BarColor.valueOf(color);
        this.style = BarStyle.valueOf(style);
    }

    private BossBar bossBar = null;
    private BarColor color;
    private BarStyle style;

    @Override
    public void startTimer(String text) {
        bossBar = Bukkit.createBossBar(text, color, style);
        bossBar.setProgress(1.0);

        for (Player online : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(online);
        }
    }

    @Override
    public void onCancel() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bossBar.setVisible(false);
            bossBar = null;
        }, 20L);
    }

    @Override
    public void sendText(String text) {
        bossBar.setTitle(text);
    }

    /**
     * Update the color and style of this boss bar.
     *
     * @param newColor The new color.
     * @param newStyle The new style.
     */
    public void update(String newColor, String newStyle) {
        this.color = BarColor.valueOf(newColor);
        this.style = BarStyle.valueOf(newStyle);

        if (bossBar == null) {
            return;
        }

        bossBar.setColor(color);
        bossBar.setStyle(style);
    }

    /**
     * Update the progress bar on the dragon timer.
     *
     * @param remaining The remaining seconds.
     * @param total The total seconds.
     */
    public void updateProgress(long remaining, long total) {
        bossBar.setProgress(((double) remaining) / ((double) total));
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (bossBar != null) {
                bossBar.addPlayer(player);
            }
        });
    }
}