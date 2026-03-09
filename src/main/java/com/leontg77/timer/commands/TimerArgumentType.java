package com.leontg77.timer.commands;

import com.leontg77.timer.Main;
import com.leontg77.timer.runnable.TimerRunnable;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Argument parser for {@link Overlay BarStyles}
 *
 * @since 1.1.0
 */
public final class TimerArgumentType implements CustomArgumentType.Converted<TimerRunnable, NamespacedKey> {
	private final Main plugin;

	public TimerArgumentType(Main plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public @NonNull TimerRunnable convert(@NonNull NamespacedKey input) throws CommandSyntaxException {
		TimerRunnable timer = plugin.getActiveTimer(input);
		
		if(timer == null) {
			throw new SimpleCommandExceptionType(new LiteralMessage("No timer exists with ID '" + input + "'"))
					.create();
		}

		return timer;
	}

	@Override
	public @NonNull ArgumentType<NamespacedKey> getNativeType() {
		return ArgumentTypes.namespacedKey();
	}

	@Override
	public @NonNull <S> CompletableFuture<Suggestions> listSuggestions(
			com.mojang.brigadier.context.@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
		String search = builder.getRemainingLowerCase();

		plugin.getActiveTimers()
				.keySet().stream()
				.filter(key -> key.namespace().startsWith(search)
						|| key.value().startsWith(search)
						|| key.toString().startsWith(search))
				.map(NamespacedKey::toString)
				.forEach(builder::suggest);

		return CompletableFuture.completedFuture(builder.build());
	}
}
