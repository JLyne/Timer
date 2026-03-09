package com.leontg77.timer.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Argument parser for {@link Overlay BarStyles}
 *
 * @since 1.1.0
 */
public final class BossBarOverlayArgumentType implements CustomArgumentType.Converted<Overlay, String> {
	private final List<String> styles = Arrays.stream(Overlay.values()).map(c -> c.name().toLowerCase()).toList();

	@Override
	public @NonNull Overlay convert(@NonNull String input) throws CommandSyntaxException {
		if(!styles.contains(input.toLowerCase())) {
			throw new SimpleCommandExceptionType(new LiteralMessage(input + " is not a valid bossbar style"))
					.create();
		}

		return Overlay.valueOf(input.toUpperCase());
	}

	@Override
	public @NonNull StringArgumentType getNativeType() {
		return StringArgumentType.word();
	}

	@Override
	public @NonNull <S> CompletableFuture<Suggestions> listSuggestions(
			com.mojang.brigadier.context.@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
		String search = builder.getRemainingLowerCase();

		styles.stream().filter(style -> style.startsWith(search)).forEach(builder::suggest);

		return CompletableFuture.completedFuture(builder.build());
	}
}
