package com.gmail.sharpcastle33.did.generator;

import org.bukkit.Bukkit;

import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LayoutGenerator {

	public static String generateCave(CaveGenContext ctx, int maxLength) {
		return generateCave(ctx, maxLength, 'C', 'Y');
	}

	public static String generateCave(CaveGenContext ctx, int maxLength, char startingSymbol, char continuationSymbol) {
		Set<Character> roomSymbols = ctx.style.getRooms().stream().map(Room::getSymbol).collect(Collectors.toSet());
		GrammarGraph grammar = ctx.style.getGrammar();

		if (!grammar.hasRuleSet(startingSymbol) && !roomSymbols.contains(startingSymbol)) {
			Bukkit.getLogger().log(Level.SEVERE, "Tried to generate a cave with undefined starting symbol '" + startingSymbol + "'");
			return "";
		}

		StringBuilder cave = new StringBuilder(String.valueOf(startingSymbol));

		boolean needsMoreSubstitution = grammar.hasRuleSet(startingSymbol);
		while (needsMoreSubstitution) {
			needsMoreSubstitution = false;
			for (int i = cave.length() - 1; i >= 0; i--) {
				char symbol = cave.charAt(i);
				if (grammar.hasRuleSet(symbol)) {
					GrammarGraph.RuleSet ruleSet = grammar.getRuleSet(symbol);
					cave.replace(i, i + 1, ruleSet.getRandomSubstitution(ctx));
					needsMoreSubstitution = true;
				}
			}
		}

		// Don't extend empty caves, could lead to an infinite loop. Only a silly cave grammar would produce an empty cave anyway.
		if (grammar.hasRuleSet(continuationSymbol) || roomSymbols.contains(continuationSymbol)) {
			if(cave.length() != 0 && cave.length() < maxLength) {
				int length = maxLength-cave.length();
				cave.append(generateCave(ctx, length, continuationSymbol, continuationSymbol));
			}
		}

		if(cave.length() > maxLength) {
			cave.delete(maxLength, cave.length());
		}

		return cave.toString();
	}
}
