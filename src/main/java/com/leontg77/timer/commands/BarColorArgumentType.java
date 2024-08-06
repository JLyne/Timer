package com.leontg77.timer.commands;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.bukkit.boss.BarColor;
import org.jetbrains.annotations.NotNull;

/**
 * Argument parser for {@link BarColor BarColors}
 *
 * @since 1.1.0
 */
@SuppressWarnings("UnstableApiUsage")
public final class BarColorArgumentType implements CustomArgumentType.Converted<BarColor, String> {
	private final List<String> colors = Arrays.stream(BarColor.values()).map(c -> c.name().toLowerCase()).toList();

	@Override
	public @NotNull BarColor convert(@NotNull String input) throws CommandSyntaxException {
		if(!colors.contains(input.toLowerCase())) {
			throw new SimpleCommandExceptionType(new LiteralMessage(input + " is not a valid bossbar color"))
					.create();
		}

		return BarColor.valueOf(input.toUpperCase());
	}

	@Override
	public @NotNull StringArgumentType getNativeType() {
		return StringArgumentType.word();
	}

	@Override
	public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(
			com.mojang.brigadier.context.@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
		String search = builder.getRemainingLowerCase();

		colors.stream().filter(color -> color.startsWith(search)).forEach(builder::suggest);

		return CompletableFuture.completedFuture(builder.build());
	}
}
