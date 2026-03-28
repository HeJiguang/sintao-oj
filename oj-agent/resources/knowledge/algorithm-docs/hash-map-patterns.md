# Hash Map Patterns

Hash-map problems usually become easier once you turn repeated membership checks into one-pass lookups.

## When to use it

- The problem asks whether a complement, previous value, or frequency has appeared before.
- The input is not sorted, so two pointers are not the clean first choice.
- You need to preserve original indices while checking matches.

## OJ debugging notes

- For Two Sum style questions, store the current value only after checking whether the complement already exists.
- Be explicit about whether duplicates are allowed and whether the same index can be used twice.
- When the judge says Wrong Answer, verify the smallest counterexample with duplicates, zeros, and negative values.

## Prompt hint

If the current code scans the array twice or compares every pair, ask whether a one-pass complement table can replace the nested logic.
