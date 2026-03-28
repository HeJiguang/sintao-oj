# Binary Search

Binary search is for ordered search spaces where each comparison lets you remove half of the remaining range.

## When it fits

- The input is already sorted, or the answer space is monotonic.
- You are looking for a boundary such as the first valid position or the last invalid position.

## OJ debugging notes

- Decide once whether your interval is closed `[left, right]` or half-open `[left, right)`.
- Wrong answers often come from inconsistent loop conditions or incorrect boundary updates.
- If the judge fails near edge cases, test single-element arrays, missing targets, and duplicated boundary values.
