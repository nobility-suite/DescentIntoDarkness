package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.config.InvalidConfigException;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GrammarGraph {
	private final Map<Character, RuleSet> ruleSets;

	public GrammarGraph(Map<Character, RuleSet> ruleSets) {
		this.ruleSets = ruleSets;
	}

	public boolean hasRuleSet(char symbol) {
		return ruleSets.containsKey(symbol);
	}

	public RuleSet getRuleSet(char symbol) {
		return ruleSets.get(symbol);
	}

	public void serialize(ConfigurationSection map) {
		ruleSets.forEach((symbol, ruleSet) -> {
			ConfigurationSection ruleSection = map.createSection(String.valueOf(symbol));
			for (Pair<Integer, String> entry : ruleSet.getEntries()) {
				ruleSection.set(entry.getRight(), entry.getLeft());
			}
		});
	}

	public static GrammarGraph deserialize(ConfigurationSection map) {
		Map<Character, RuleSet> ruleSets = new HashMap<>();

		for (String symbolStr : map.getKeys(false)) {
			if (symbolStr.length() != 1) {
				throw new InvalidConfigException("Symbol must be a single character");
			}
			char symbol = symbolStr.charAt(0);
			ConfigurationSection ruleSection = map.getConfigurationSection(symbolStr);
			if (ruleSection != null) {
				List<Pair<Integer, String>> entries = new ArrayList<>();
				for (String substitution : ruleSection.getKeys(false)) {
					int weight = ruleSection.getInt(substitution, 1);
					if (weight <= 0) {
						throw new InvalidConfigException("Rule must have a positive weight");
					}
					entries.add(Pair.of(weight, substitution));
				}
				if (entries.isEmpty()) {
					throw new InvalidConfigException("Rule has no substitutions");
				}
				ruleSets.put(symbol, new RuleSet(entries));
			}
		}

		return new GrammarGraph(ruleSets);
	}

	public void validate(Set<Character> roomSymbols) {
		if (roomSymbols.stream().anyMatch(ruleSets::containsKey)) {
			throw new InvalidConfigException("Room cannot use the same symbol as a grammar rule set");
		}
		if (ruleSets.isEmpty()) {
			if (!roomSymbols.contains('C')) {
				throw new InvalidConfigException("Could not find starting 'C' symbol");
			}
			return;
		}

		if (!ruleSets.containsKey('C')) {
			throw new InvalidConfigException("Could not find starting 'C' symbol");
		}

		for (RuleSet ruleSet : ruleSets.values()) {
			for (Pair<Integer, String> entry : ruleSet.getEntries()) {
				String substitution = entry.getRight();
				for (int i = 0; i < substitution.length(); i++) {
					char target = substitution.charAt(i);
					if (!ruleSets.containsKey(target) && !roomSymbols.contains(target)) {
						throw new InvalidConfigException("Substitution contains symbol that is not a rule or a room");
					}
				}
			}
		}

		checkIllegalRecursion('C', new HashSet<>(), new HashSet<>());
	}

	// Checks for all recursion in the grammar and complains about it, except in the special case where a rule directly
	// references itself at the *end* of a substitution. Grammars where this rule is enforced, along with the other
	// trivial checks performed by this class, guarantee that the cave will terminate.
	private void checkIllegalRecursion(char fromSymbol, Set<Character> recursionStack, Set<Character> visitedSymbols) {
		if (recursionStack.contains(fromSymbol)) {
			throw new InvalidConfigException("Complex recursion detected in the cave grammar");
		}

		if (!visitedSymbols.add(fromSymbol)) {
			return;
		}

		recursionStack.add(fromSymbol);

		RuleSet ruleSet = ruleSets.get(fromSymbol);
		for (Pair<Integer, String> entry : ruleSet.getEntries()) {
			String substitution = entry.getRight();
			for (int i = 0; i < substitution.length(); i++) {
				char target = substitution.charAt(i);
				if (target == fromSymbol) {
					if (i != substitution.length() - 1) {
						throw new InvalidConfigException("Self-referential rule detected in cave grammar. Self-reference is only allowed at the end of a substitution");
					}
				} else {
					if (ruleSets.containsKey(target)) {
						checkIllegalRecursion(target, recursionStack, visitedSymbols);
					}
				}
			}
		}

		recursionStack.remove(fromSymbol);
	}

	public static class RuleSet {
		// A list of strings that this character may be replaced with, each with a weight attached
		private final List<Pair<Integer, String>> entries;
		private final int totalWeight;

		public RuleSet(List<Pair<Integer, String>> entries) {
			this.entries = entries;
			int totalWeight = 0;
			for (Pair<Integer, String> entry : entries) {
				totalWeight += entry.getLeft();
			}
			this.totalWeight = totalWeight;
		}

		public List<Pair<Integer, String>> getEntries() {
			return entries;
		}

		public String getRandomSubstitution(CaveGenContext ctx) {
			int randVal = ctx.rand.nextInt(totalWeight);
			for (Pair<Integer, String> entry : entries) {
				randVal -= entry.getLeft();
				if (randVal < 0) {
					return entry.getRight();
				}
			}
			throw new AssertionError("The total weight of the entries is greater than totalWeight?");
		}
	}
}
