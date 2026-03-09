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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.jspecify.annotations.NonNull;

/**
 * Argument parser for {@link Color Colors}
 *
 * @since 1.1.0
 */
public final class BossBarColorArgumentType implements CustomArgumentType.Converted<BossBar.Color, String> {
	private final List<String> colors = Arrays.stream(Color.values()).map(c -> c.name().toLowerCase()).toList();

	@Override
	public @NonNull Color convert(@NonNull String input) throws CommandSyntaxException {
		if(!colors.contains(input.toLowerCase())) {
			throw new SimpleCommandExceptionType(new LiteralMessage(input + " is not a valid bossbar color"))
					.create();
		}

		return Color.valueOf(input.toUpperCase());
	}

	@Override
	public @NonNull StringArgumentType getNativeType() {
		return StringArgumentType.word();
	}

	@Override
	public @NonNull <S> CompletableFuture<Suggestions> listSuggestions(
			com.mojang.brigadier.context.@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
		String search = builder.getRemainingLowerCase();

		colors.stream().filter(color -> color.startsWith(search)).forEach(builder::suggest);

		return CompletableFuture.completedFuture(builder.build());
	}
}
