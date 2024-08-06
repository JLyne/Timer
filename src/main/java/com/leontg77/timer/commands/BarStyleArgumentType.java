package com.leontg77.timer.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.boss.BarStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Argument parser for {@link BarStyle BarStyles}
 *
 * @since 1.1.0
 */
@SuppressWarnings("UnstableApiUsage")
public final class BarStyleArgumentType implements CustomArgumentType.Converted<BarStyle, String> {
	private final List<String> styles = Arrays.stream(BarStyle.values()).map(c -> c.name().toLowerCase()).toList();

	@Override
	public @NotNull BarStyle convert(@NotNull String input) throws CommandSyntaxException {
		if(!styles.contains(input.toLowerCase())) {
			throw new SimpleCommandExceptionType(new LiteralMessage(input + " is not a valid bossbar style"))
					.create();
		}

		return BarStyle.valueOf(input.toUpperCase());
	}

	@Override
	public @NotNull StringArgumentType getNativeType() {
		return StringArgumentType.word();
	}

	@Override
	public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(
			com.mojang.brigadier.context.@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
		String search = builder.getRemainingLowerCase();

		styles.stream().filter(style -> style.startsWith(search)).forEach(builder::suggest);

		return CompletableFuture.completedFuture(builder.build());
	}
}
