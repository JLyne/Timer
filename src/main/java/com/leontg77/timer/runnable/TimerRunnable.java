/*
 * Project: Timer
 * Class: com.leontg77.timer.runnable.TimerRunnable
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

package com.leontg77.timer.runnable;

import com.leontg77.timer.Main;
import com.leontg77.timer.handling.TimerHandler;
import com.leontg77.timer.handling.handlers.BossBarHandler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Timer runnable class.
 *
 * @author Jim, LeonTG & ghowdenb
 */
public final class TimerRunnable implements Runnable {
    private final TimerHandler handler;
    private final Main plugin;

    private final NamespacedKey id;
    private final Component message;
    private final @Nullable Instant endTime;

    private final boolean infinite;
    private final long total;

    private long remaining;
    private int jobId = -1;
    private boolean cancelled = false;

    public TimerRunnable(NamespacedKey id, Component message, @Nullable Instant endTime, TimerHandler handler) {
        plugin = Main.getInstance();
        this.handler = handler;

        this.id = id;
        this.message = message;
        this.endTime = endTime;
        infinite = endTime == null;

        if(infinite) {
            handler.show(message);
            remaining = Long.MAX_VALUE;
            total = Long.MAX_VALUE;
        } else {
            Instant now = Instant.now();
            total = endTime.getEpochSecond() - now.getEpochSecond();
            remaining = total;
            handler.show(message.append(Component.text(" " + getFriendlyTime(remaining))));
            jobId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1L);
        }
    }

    @Override
    public void run() {
        if(infinite) {
            return;
        }

        long newRemaining = Duration.between(Instant.now(), endTime).getSeconds();

        if (remaining != newRemaining) {
            remaining = newRemaining;
            handler.setText(message.append(Component.text(" " + getFriendlyTime(remaining))));
            handler.updateProgress(remaining, total);

            if(remaining == 0) {
                plugin.getLogger().info("Timer " + id + " has ended");
                cancel();
            }
        }
    }

    /**
     * Cancel the timer task if it's running.
     */
    public void cancel() {
        if (cancelled) {
            return;
        }

        handler.hide();
        cancelled = true;

        if (!infinite) {
            Bukkit.getScheduler().cancelTask(jobId);
        }
    }

    /**
     * Check if the timer is currently running.
     *
     * @return True if it is, false otherwise.
     */
    public boolean isRunning() {
        return !cancelled;
    }

    private static final long SECONDS_PER_DAY = 86400;
    private static final long SECONDS_PER_HOUR = 3600;
    private static final long SECONDS_PER_MINUTE = 60;

    /**
     * Converts the seconds into a string with hours, minutes and seconds.
     *
     * @param seconds the number of seconds.
     * @return The converted seconds.
     */
    private String getFriendlyTime(long seconds) {
        int days = (int) Math.floor(seconds / (double) SECONDS_PER_DAY);
        seconds -= days * SECONDS_PER_DAY;

        int hours = (int) Math.floor(seconds / (double) SECONDS_PER_HOUR);
        seconds -= hours * SECONDS_PER_HOUR;

        int minutes = (int) Math.floor(seconds / (double) SECONDS_PER_MINUTE);
        seconds -= minutes * SECONDS_PER_MINUTE;

        ArrayList<String> parts = new ArrayList<>();

        if (days > 0) {
            parts.add(days + "d");
        }

        if (hours > 0) {
            parts.add(hours + "h");
        }

        if (minutes > 0) {
            parts.add(minutes + "m");
        }

        if(seconds > 0 || parts.isEmpty()) {
            parts.add(seconds + "s");
        }

        return String.join(" ", parts);
    }

    private String getClockTime(long seconds) {
        int days = (int) Math.floor(seconds / (double) SECONDS_PER_DAY);
        seconds -= days * SECONDS_PER_DAY;

        int hours = (int) Math.floor(seconds / (double) SECONDS_PER_HOUR);
        seconds -= hours * SECONDS_PER_HOUR;

        int minutes = (int) Math.floor(seconds / (double) SECONDS_PER_MINUTE);
        seconds -= minutes * SECONDS_PER_MINUTE;

        ArrayList<String> parts = new ArrayList<>();

        if (days > 0) {
            parts.add(String.valueOf(days));
        }

        if (hours > 0) {
            parts.add(String.valueOf(hours));
        }

        parts.add(String.format("%02d", minutes));
        parts.add(String.format("%02d", seconds));

        return String.join(":", parts);
    }

    public NamespacedKey getId() {
        return id;
    }

    public Component getMessage() {
        return message;
    }

    public @Nullable Instant getEndTime() {
        return endTime;
    }

    public long getRemaining() {
        return remaining;
    }

    public String getFriendlyRemaining() {
        return getFriendlyTime(remaining);
    }

    public String getClockRemaining() {
        return getClockTime(remaining);
    }

    public long getTotal() {
        return total;
    }

    public boolean isInfinite() {
        return infinite;
    }

    public BossBar.@Nullable Color getColorOverride() {
        if (!(handler instanceof BossBarHandler bossBarHandler)) {
            return null;
        }

        return bossBarHandler.getColorOverride();
    }

    public BossBar.@Nullable Overlay getStyleOverride() {
        if (!(handler instanceof BossBarHandler bossBarHandler)) {
            return null;
        }

        return bossBarHandler.getStyleOverride();
    }

    public void setColorOverride(BossBar.Color color) {
        if (handler instanceof BossBarHandler bossBarHandler) {
            bossBarHandler.setColorOverride(color);
        }
    }

    public void setStyleOverride(BossBar.Overlay style) {
        if (handler instanceof BossBarHandler bossBarHandler) {
            bossBarHandler.setStyleOverride(style);
        }
    }

    public void resetColor() {
        if (handler instanceof BossBarHandler bossBarHandler) {
            bossBarHandler.resetColor();
        }
    }

    public void resetStyle() {
        if (handler instanceof BossBarHandler bossBarHandler) {
            bossBarHandler.resetStyle();
        }
    }
}