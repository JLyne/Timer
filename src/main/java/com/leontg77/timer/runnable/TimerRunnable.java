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
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Timer runnable class.
 *
 * @author LeonTG & ghowdenb
 */
public class TimerRunnable implements Runnable {
    private final TimerHandler handler;
    private final Main plugin;

    public TimerRunnable(Main plugin, TimerHandler handler) {
        this.handler = handler;
        this.plugin = plugin;

        if (handler instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) handler, plugin);
        }
    }

    private boolean countdown = true;
    private int jobId = -1;

    private String message;

    private long remaining = 0;
    private long total = 0;
    private Instant endTime = null;

    @Override
    public void run() {
        if (handler instanceof BossBarHandler) {
            if(countdown) {
                ((BossBarHandler) handler).updateProgress(remaining, total);
            } else {
                ((BossBarHandler) handler).updateProgress(1, 1);
            }
        }

        long newRemaining = Duration.between(Instant.now(), endTime).getSeconds();

        if (countdown && newRemaining < 0) {
            plugin.getLogger().info("Timer has ended for \"" + message + "\"");
            cancel();
            return;
        }

        if (remaining != newRemaining) {
            handler.sendText(message + (countdown ? " " + timeToString(newRemaining) : ""));
            remaining = newRemaining;
        }
    }

    /**
     * Starts this timer with the given message for the amount of invocations.
     * Once complete the task will cancel itself.
     *
     * Overwrites any previous settings
     *
     * @param message the message to send
     * @param endTime the instant the timer should end at
     */
    public void startSendingMessage(String message, Instant endTime) {
        Instant now = Instant.now();
        this.remaining = Duration.between(now, endTime).getSeconds();
        this.total = this.remaining;
        this.endTime = endTime;

        this.countdown = !endTime.isBefore(now);
        this.message = message;

        handler.startTimer(message + (countdown ? " " + timeToString(remaining) : ""));

        cancel();
        jobId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1L);
    }

    /**
     * Cancel the timer task if it's running.
     */
    public void cancel() {
        if (jobId == -1) {
            return;
        }

        Bukkit.getScheduler().cancelTask(jobId);
        jobId = -1;

        handler.onCancel();
    }

    /**
     * Check if the timer is currently running.
     *
     * @return True if it is, false otherwise.
     */
    public boolean isRunning() {
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
    private String timeToString(long seconds) {
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

        if(seconds > 0 || parts.size() == 0) {
            parts.add(seconds + "s");
        }

        return String.join(" ", parts);
    }
}