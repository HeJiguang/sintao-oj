# Dynamic Programming

Dynamic programming works when the global answer can be built from overlapping subproblems with stable state transitions.

## When it fits

- You can clearly define a state such as `dp[i]` or `dp[i][j]`.
- The same smaller subproblems repeat.
- The question asks for an optimum, a count, or a yes/no feasibility result.

## OJ debugging notes

- Write the state meaning in plain language before coding transitions.
- Most Wrong Answer results come from bad initialization, missing base cases, or invalid transition order.
- Test the first two or three states by hand before trusting the recurrence.
