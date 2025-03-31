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
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

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

    private final Component message;
    private final Instant endTime;

    private final boolean countdown;
    private int jobId = -1;

    private long remaining = 0;
    private long total = 0;

    public TimerRunnable(Component message, @Nullable Instant endTime, TimerHandler handler) {
        this.plugin = Main.getInstance();
        this.handler = handler;

        this.message = message;
        this.endTime = endTime;
        this.countdown = endTime != null;

        if(this.countdown) {
            Instant now = Instant.now();
            this.total = this.remaining = Duration.between(now, endTime).getSeconds();
            handler.show(message.append(Component.text(" " + getFriendlyTime(remaining))));
            jobId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1L);
        } else {
            handler.show(message);
        }
    }

    @Override
    public void run() {
        if(!countdown) {
            return;
        }

        long newRemaining = Duration.between(Instant.now(), endTime).getSeconds();

        if (remaining != newRemaining) {
            handler.setText(message.append(Component.text(" " + getFriendlyTime(remaining))));
            remaining = newRemaining;
            handler.updateProgress(remaining, total);
        }

        if(newRemaining == 0) {
            plugin.getLogger().info("Timer has ended for \"" + Main.plain.serialize(message) + "\"");
            cancel();
        }
    }

    /**
     * Cancel the timer task if it's running.
     */
    public void cancel() {
        Bukkit.getScheduler().cancelTask(jobId);
        handler.hide();
    }

    /**
     * Check if the timer is currently running.
     *
     * @return True if it is, false otherwise.
     */
    public boolean isRunning() {
        if(!countdown) {
            return true;
        }

        BukkitScheduler sch = Bukkit.getScheduler();
        return sch.isCurrentlyRunning(jobId) || sch.isQueued(jobId);
    }

    /**
     * Get the handler for the timer.
     *
     * @return The timer handler.
     */
    public TimerHandler getHandler() {
        return handler;
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

    public Component getMessage() {
        return message;
    }

    public Instant getEndTime() {
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

    public boolean isCountdown() {
        return countdown;
    }
}