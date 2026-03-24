# SynCode `front-ai` Design

Date: 2026-03-24
Status: Draft for user review
Scope: `D:\Project\OnlineOJ\bite-oj-master\bite-oj-master`

## 1. Summary

`front-ai` will be a single frontend application for SynCode.

It will use a dual-domain architecture:

- `user` domain: learning-first coding experience for ordinary users
- `admin` domain: reserved for later integration of management features

The first delivery will prioritize the user domain and frame SynCode as an AI-assisted coding learning platform, not only an online judge.

The frontend will unify:

- design language
- routing model
- gateway-based API access
- auth and role guards
- shared state and component foundation

The product center of gravity will be the learning workspace:

`read problem -> write code -> run/submit -> inspect result -> ask AI -> continue learning`

## 2. Goals

- Build a strong first version of the SynCode user experience.
- Make the product feel closer to an AI learning platform than a traditional course-project OJ site.
- Keep the engineering structure ready for later `/admin` integration without reworking the foundation.
- Route all frontend traffic through the gateway instead of binding the UI to individual microservice addresses.
- Treat AI assistance as a core learning capability, not an afterthought chat box.

## 3. Non-goals

- Rebuild or optimize backend services in this phase.
- Depend on future AI backend upgrades to ship the first frontend version.
- Implement the admin domain screens in the first delivery.
- Clone LeetCode visually. The interaction model may be similar, but SynCode should keep its own visual identity.

## 4. Product Positioning

SynCode should be presented as:

`AI coding learning platform + OJ training engine`

This positioning changes the frontend priorities:

- the homepage should highlight learning value, not only problem counts
- the problem page should feel like a learning entry, not just a list
- the workspace should optimize for understanding, iteration, and reflection
- the personal center should evolve toward a learning profile, not only user info

## 5. Current System Constraints

Current repo evidence shows:

- ordinary-user flows are mainly exposed through `oj-friend`
- admin flows are mainly exposed through `oj-system`
- AI assistant flows are exposed through `oj-ai`
- the old frontend already assumes gateway-based access for AI and user/admin traffic
- AI currently supports plain chat, structured detail output, and SSE streaming

The first frontend version must therefore:

- call the gateway only
- isolate API modules by domain, not by direct service URL
- support both regular AI responses and streaming AI responses

## 6. Information Architecture

### 6.1 Route domains

- `/`
- `/login`
- `/problems`
- `/problems/:questionId`
- `/contests`
- `/contests/:examId`
- `/workspace/practice/:questionId`
- `/workspace/contest/:examId/:questionId`
- `/me`
- `/me/contests`
- `/me/messages`
- `/me/submissions`
- `/admin/*` reserved for later

### 6.2 Shells

The app will use four shell layers:

- `PublicShell`
  Used for homepage and login.
- `UserShell`
  Used for browsing pages like problems, contests, and personal center.
- `WorkspaceShell`
  Used only for practice and contest coding workspaces.
- `AdminShell`
  Reserved for future management pages.

This separation is important because the coding workspace needs a focused, immersive environment and should not inherit the browsing-page layout.

## 7. First Delivery Scope

The first delivery should implement:

- homepage
- login page
- problems page
- practice workspace
- contests page
- basic personal center

Contest workspace may be scaffolded if time permits, but the deepest effort should go into:

- homepage
- problems page
- AI learning workspace

## 8. User Experience Principles

- Learning first: every major screen should help the user understand, not only submit.
- Continuous context: do not force the user to jump between multiple pages during solving.
- AI as co-pilot: the AI panel should feel like a study coach, not like customer support chat.
- Clear hierarchy: reading, coding, judging, and asking AI should be visually and cognitively separate.
- Product identity: the interface should look like a modern technical product, not a classroom demo or generic admin panel.

## 9. Visual Direction

### 9.1 Visual intent

The frontend should feel:

- technical
- calm
- modern
- focused
- slightly futuristic

### 9.2 Color and mood

Recommended direction:

- bright browsing pages with cold-white surfaces
- graphite and deep-slate workspace surfaces
- electric-blue accents for key actions and data emphasis
- limited use of glow, grid, and structural background texture

Avoid:

- heavy neon cyberpunk styling
- purple-heavy gradients
- default admin-dashboard aesthetics
- visually noisy decoration

### 9.3 Workspace visual contrast

The product should use different moods for different contexts:

- browsing pages: lighter, cleaner, product-like
- coding workspace: darker, more immersive, editor-centered

This contrast helps the workspace feel intentional and premium.

## 10. Workspace Design

The workspace is the core of the product.

It should support the learning loop:

`understand -> attempt -> judge -> reflect -> ask AI -> iterate`

### 10.1 Top context bar

The top bar should communicate current context, not generic navigation.

Practice mode should show:

- problem title
- difficulty
- knowledge tags
- latest submission status
- return-to-problems action

Contest mode should additionally show:

- contest title
- current question index
- countdown timer
- contest status
- return-to-contest action

### 10.2 Left learning panel

The left side should not be a plain long problem description.

It should be structured into learning-focused sections:

- `Problem`
  Full statement, IO, examples, constraints
- `Understand`
  Simplified goal, key constraints, common misunderstandings
- `Knowledge`
  Tags, what this question trains, recommended prerequisites
- `Notes`
  Personal notes, latest result summary, AI summary entry

### 10.3 Center coding panel

The center area is the visual and functional core of the page.

It should include:

- language selector
- code editor
- local draft status
- run action
- submit action
- compact result status strip
- expandable result panel beneath the editor

The result panel should show:

- current status
- error summary
- testcase comparison
- runtime or execution information
- action to send the result to AI for explanation

### 10.4 Right AI learning coach panel

The AI panel should be guided, not empty.

At the top, show quick actions:

- `Explain this problem`
- `Analyze my code`
- `Explain this judge result`

The conversation section should support:

- normal question input
- streaming responses
- clear user/assistant separation
- structured metadata when available

If the `detail` endpoint is used, the panel should surface:

- `intent`
- `confidence`
- `nextAction`

This allows the AI area to feel like a learning system rather than a plain answer box.

## 11. AI Assistant Capability Matrix

### 11.1 Capabilities available now

The current backend already supports enough for the first frontend version:

- send question title
- send question content
- send current user code
- send latest judge result summary
- send user free-form question
- receive plain answer text
- receive structured detail output
- receive streaming SSE output

### 11.2 Frontend features that can ship now

The first frontend version can implement:

- one-click problem explanation
- one-click code analysis
- one-click judge-result explanation
- free-form follow-up chat
- streaming answer rendering
- display of `intent`, `confidence`, and `nextAction`
- chat history inside the current workspace session

### 11.3 Deferred backend enhancement needs

These should not block the frontend, but should be tracked for later backend optimization:

- multi-turn conversation memory across sessions
- layered hint system
- structured teaching output such as error category and learning objective
- personalized recommendations based on learning history
- precise code-line localization for errors
- automatic post-solve summaries

The frontend should record these as backend requirements in a dedicated documentation folder once `front-ai` is scaffolded.

Recommended future file:

- `front-ai/docs/backend-requirements/ai-assistant-upgrades.md`

## 12. Frontend Architecture

### 12.1 Technical stack

Recommended baseline:

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia

The new app should not continue as a plain JavaScript rewrite of the old frontends.

### 12.2 Layered structure

Recommended app structure:

- `app`
  app bootstrap, providers, router mount
- `layouts`
  shell-level layout components
- `pages`
  route pages
- `features`
  problem list, workspace AI panel, submissions, profile modules
- `entities`
  problem, contest, submission, user, ai-session data models
- `shared`
  ui kit, tokens, hooks, utils, request layer

### 12.3 Routing and auth

Routing should be guarded by:

- public routes
- ordinary-user routes
- admin routes

The first version needs the role model in place even if admin pages are not yet implemented.

## 13. API Strategy

### 13.1 Gateway-only access

All frontend requests should go through the gateway.

The app should not directly bind to:

- `oj-friend`
- `oj-system`
- `oj-ai`

### 13.2 Domain-based API modules

Recommended API grouping:

- `auth`
- `problems`
- `contests`
- `submissions`
- `profile`
- `ai`

### 13.3 AI integration mode

The AI request layer should support both:

- request-response calls for simple actions
- SSE streaming for richer coaching interactions

The workspace UI should be designed to prefer streaming when available.

## 14. Data Flow

### 14.1 Practice workflow

1. Load problem context
2. Restore draft if present
3. User edits code
4. User runs or submits
5. Judge result updates result panel
6. User triggers AI quick action or asks a follow-up question
7. Frontend sends problem context, code, judge result, and question to AI
8. AI response is rendered in the coach panel

### 14.2 Contest workflow

1. Load contest context
2. Load first or current problem
3. Show timer and contest status
4. Submit within contest context
5. Keep AI context scoped to contest problem and latest result

## 15. Error Handling

The frontend should treat these as first-class scenarios:

- gateway unavailable
- auth expired
- AI response timeout
- AI stream interrupted
- judge result still pending
- problem content load failure

UI expectations:

- explicit empty and error states
- retry actions for recoverable failures
- preserve draft when the network fails
- avoid full-page crashes from panel-level failures

## 16. Testing Strategy

The frontend implementation should verify:

- route guards
- request-layer behavior
- workspace state transitions
- AI panel streaming rendering
- result-panel updates after submission
- responsive layout behavior for the workspace and browsing pages

Recommended verification mix:

- unit tests for utilities and stores
- component tests for critical workspace interactions
- targeted end-to-end smoke coverage for main user flows

## 17. Rollout Notes

Implementation should happen in this order:

1. scaffold `front-ai`
2. establish app structure, tokens, routing, gateway request layer
3. build homepage and user shell
4. build problems page
5. build practice workspace
6. integrate AI coach panel with current backend
7. build personal center basics
8. reserve admin shell and admin route skeleton

## 18. Open Decisions Already Locked

The following decisions are approved for this design:

- product name: `SynCode`
- one frontend project only
- dual-domain architecture
- user domain first
- gateway-only API access
- AI learning platform positioning
- backend optimization requests should be documented rather than implemented in this phase

## 19. Review Checklist

This design is ready for user review against these questions:

- Does SynCode feel like an AI learning platform rather than a generic OJ?
- Is the first delivery scope focused enough?
- Does the workspace design match the intended learning experience?
- Is the separation between frontend scope and backend enhancement scope clear enough?
