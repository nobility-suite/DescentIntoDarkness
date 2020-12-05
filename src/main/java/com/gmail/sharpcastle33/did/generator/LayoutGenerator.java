package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.generator.room.Room;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LayoutGenerator {

	public static Layout generateCave(CaveGenContext ctx, int maxLength, char startingSymbol) {
		return generateCave(ctx, maxLength, startingSymbol, ctx.style.getContinuationSymbol());
	}

	public static Layout generateCave(CaveGenContext ctx, int maxLength, char startingSymbol, char continuationSymbol) {
		Set<Character> roomSymbols = ctx.style.getRooms().stream().map(Room::getSymbol).collect(Collectors.toSet());
		GrammarGraph grammar = ctx.style.getGrammar();

		if (!grammar.hasRuleSet(startingSymbol) && !roomSymbols.contains(startingSymbol)) {
			Bukkit.getLogger().log(Level.SEVERE, "Tried to generate a cave with undefined starting symbol '" + startingSymbol + "'");
			return Layout.EMPTY;
		}

		StringBuilder cave = new StringBuilder(String.valueOf(startingSymbol));
		List<List<String>> tags = new ArrayList<>();
		tags.add(new ArrayList<>());

		boolean needsMoreSubstitution = grammar.hasRuleSet(startingSymbol);
		while (needsMoreSubstitution) {
			needsMoreSubstitution = false;
			for (int i = cave.length() - 1; i >= 0; i--) {
				char symbol = cave.charAt(i);
				if (grammar.hasRuleSet(symbol)) {
					GrammarGraph.RuleSet ruleSet = grammar.getRuleSet(symbol);
					String substitution = ruleSet.getRandomSubstitution(ctx);
					cave.replace(i, i + 1, substitution);
					List<String> prevTags = tags.remove(i);
					prevTags.addAll(ruleSet.getTags());
					List<List<String>> newTags = new ArrayList<>(substitution.length());
					for (int j = 0; j < substitution.length(); j++) {
						newTags.add(new ArrayList<>(prevTags));
					}
					tags.addAll(i, newTags);
					needsMoreSubstitution = true;
				}
			}
		}

		// Don't extend empty caves, could lead to an infinite loop. Only a silly cave grammar would produce an empty cave anyway.
		if (continuationSymbol != 0) {
			if(cave.length() != 0 && cave.length() < maxLength) {
				int length = maxLength-cave.length();
				Layout layout = generateCave(ctx, length, continuationSymbol, continuationSymbol);
				cave.append(layout.getValue());
				tags.addAll(layout.getTags());
			}
		}

		if (ctx.style.shouldTruncateCaves() && cave.length() > maxLength) {
			cave.delete(maxLength, cave.length());
			tags.subList(maxLength, cave.length()).clear();
		}

		return new Layout(cave.toString(), tags);
	}

	public static class Layout {
		public static final Layout EMPTY = new Layout("", new ArrayList<>());

		private String value;
		private final List<List<String>> tags;

		public Layout(String value, List<List<String>> tags) {
			this.value = value;
			this.tags = tags;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public List<List<String>> getTags() {
			return tags;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
