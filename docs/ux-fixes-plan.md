# LearnLock UX and generation fixes

Status: planning only, prepared 2026-09-30. Implement after the user switches to the coding model. No application changes or commits were made during planning.

## Visual direction

Keep the existing Compose Material 3 interface. Use one blue accent family, neutral surfaces, readable typography, 16dp compact-screen margins, and 48dp minimum touch targets. Preserve green/red with icons and labels for answer feedback. No incidental dependency upgrades or wholesale redesign.

Use a complete blue light/dark palette consistently across navigation, buttons, progress, chips, switches, and the unlock activity. Disable wallpaper-based accent substitution for this design so it cannot reintroduce mismatched colors. Settings gets an Appearance control with System / Light / Dark; default to System and persist the choice. This is the planned interpretation of “theme change toggle.”

### Screenshot-specific corrections

- Image 1: the RTI example is Python-like code, unrelated to teaching the legal concept. Replace this class of example with a subject-appropriate practical scenario. Keep question progress, question content, and the primary action in separate layout regions so they cannot overlap.
- Image 2: move Start quiz from immediately below the text to the bottom action area, above system navigation. The concept body can scroll independently.
- Image 3: place Submit quiz below the question content in the same bottom action area. Answer feedback and explanations stay in the scrolling body. The title must remain reachable and never slide underneath an opaque header because of incorrect inset handling.

Concept reading layout:

```text
Unlock quiz                         Star  Close
------------------------------------------------
Subject · difficulty
Concept title
Definition and key points                       |
                                               | scroll
Example: practical subject-specific scenario    |
------------------------------------------------
[                 Start quiz                   ]
system navigation inset
```

Quiz layout:

```text
Unlock quiz                         Star  Close
------------------------------------------------
Concept title · Review concept
Question 1 of 3 / progress
Question text                                   |
Answer choices                                  | scroll
Feedback / explanation, when appropriate         |
------------------------------------------------
[       Next question / Submit quiz             ]
system navigation inset
```

Keep the full lesson accessible through Review concept after starting, without forcing the user to scroll through it to reach every question. Preserve existing grading timing unless a change is necessary to prevent duplicate submissions or invalid states. One primary action is visible at a time; it is disabled until the current question is answerable and answered. Use a Scaffold bottom action region with body padding that accounts for it, system bars, and the keyboard. Avoid overlapping siblings inside AnimatedContent; each transition needs a single layout root.

History layout:

```text
History                                  Clear
[ Search concepts or subjects...             x ]
Topic filters
All / Passed / Retry pending / Starred

Constitutional Supremacy                    Star
Law in India · Passed · latest activity
2 attempts · View details
```

Display one concept entry. A resolved old failure must not show Retry. Keep attempts available in details and retain their original answers for learning analytics. Search and filters operate on the grouped concept entries.

Home generation button states:

```text
[ Generate new concepts ]
[ spinner  Generating concepts... ]
[ check    3 concepts added ]
[          Retry generation ]
```

Keep button dimensions stable, expose loading semantics, and show a short inline error when needed. Use indeterminate progress unless a real measured fraction exists. No modal or operating-system notification for generation. Other tabs stay usable throughout.

## Findings from the current source

- `HistoryScreen.kt` lists every history row. Its card label and Retry action depend on `isCorrect`, while filtering uses `status`. `HistoryDao.markConceptPassed` updates status on older failed attempts, leaving their historically correct `isCorrect = false`; this explains contradictory cards.
- `HistoryDao.getRecentlyViewedHistory` limits raw attempts to ten without grouping. Deduplicating only after that limit would still omit eligible concepts.
- History currently has no stable concept identifier. Several status/star/analytics operations match title alone, which can collide across subjects.
- `UnlockQuizScreen.kt` places actions within scrolling/animated content. The action branches also emit multiple sibling composables, which needs inspection for the screenshot overlaps.
- `MainActivity.kt` uses fixed `ElegantPrimary` purple navigation colors, while `UnlockLearnTheme` can select the device's dynamic scheme. The fallback theme is dark-only.
- `LearnScreen.kt` reads window start/end directly and displays an active-window label without making the disabled schedule state explicit.
- Manual generation uses an unmanaged IO coroutine and composable-owned flags plus `GenerationProgressModal`. Navigation can detach the UI state from the ongoing operation.
- `GeminiConceptGenerator.kt` asks a generic `codeExample` field to carry everything from prose to code. `UnlockQuizScreen.kt` always renders it in monospace.
- `UnlockOverlayService.kt` starts a foreground service notification; `MainActivity.kt` requests notification permission. This has platform constraints, described below.
- Existing stack: Compose Material 3 through BOM 2024.09.00, Room 2.7.0, min SDK 24, target SDK 36. Reuse supported components; verify APIs against the installed versions during coding.

## Implementation order and separate commits

Before coding, inspect the current index and working-tree diff. Many affected files already contain uncommitted changes, including a partially staged generation-modal file. Preserve those changes and existing staged hunks. Use explicit paths/hunk staging; never blanket-stage the repository. Review each commit diff so pre-existing work is not silently attributed to a new fix. If a fix depends on prior uncommitted work, account for that dependency explicitly before committing.

### 1. Unify the accent colors

Commit: `fix(theme): unify navigation and controls with a blue palette`

Define coherent theme roles in `Color.kt` / `Theme.kt`; migrate fixed brand colors in navigation and affected screens to those roles. All activities use the same theme. Keep status feedback semantically distinct.

Acceptance: bottom navigation, buttons, selected chips, switches, and progress share the blue accent on Home, History, Settings, and unlock screens.

### 2. Add appearance settings

Commit: `feat(settings): add persistent system light and dark themes`

Add the preference, observable updates, complete light scheme, and Appearance control in Settings. Apply it at every activity root, including system-bar icon contrast. Keep secrets/API-key storage unaffected.

Acceptance: the selected mode updates immediately, survives restart, and is honored when a fresh unlock quiz opens. System mode follows system appearance changes.

### 3. Fix concept and quiz layout

Commit: `fix(quiz): anchor primary actions below scrollable content`

Implement the layouts above in `UnlockQuizScreen.kt`. Separate reading, answering, submitting, and result state. Reserve action-area space; correct animated layout roots and insets. Preserve answers through rotation, prevent double submission, and ensure result content is usable on short screens.

Acceptance: recreate all three screenshot scenarios with long content and multiple questions; no overlap, clipped titles, covered answers, or unreachable controls. Verify compact portrait, landscape, keyboard-visible text answers, and large fonts.

### 4. Group history and resolve retry state

Commit: `fix(history): group concepts and resolve passed retries`

Introduce a consistent concept identity shared by concepts, attempts, retry resolution, starring, and display grouping. Prefer a persisted stable identity; provide a non-destructive Room migration and deterministic legacy backfill scoped by topic plus normalized title. Never merge identical titles from different topics. Handle orphaned legacy history safely.

Keep raw attempts unchanged for accuracy/mastery calculations. Derive one current concept summary: latest relevant attempt and unresolved retry status, with a successful retry resolving earlier failures. A later genuinely failed review may create a new pending retry. Use current status for the card and Retry action, not the correctness of an old attempt. Preserve original failed answers in the attempt detail.

Acceptance: fail then pass yields one Passed card, no pending retry, and both original attempts in details. Test repeated passes, later failed reviews, same titles across subjects, starred entries, and existing database migration.

### 5. Add History search

Commit: `feat(history): search concepts and subjects`

Add a full-width search field below the title, with a clear action. Match title and subject case-insensitively, trim whitespace, combine with existing topic/status/star filters, and preserve query across recreation. Distinguish empty history from no search matches.

Acceptance: search results contain unique concepts and work with every filter; keyboard does not cover the last result or navigation.

### 6. Deduplicate recently viewed

Commit: `fix(home): show each recently viewed concept once`

Reuse the concept identity from step 4. Track actual last-viewed time separately from answer time where required: inspecting a concept must not create a quiz attempt or alter mastery. Sort unique concepts by last activity/view, then apply the ten-item limit. Backfill existing recency from history without deleting attempts.

Acceptance: reopening one item repeatedly moves that single entry to the top. More than ten repeated attempts still allow ten distinct eligible concepts to appear. Viewing alone does not change quiz counts or accuracy.

### 7. Correct learning-window status

Commit: `fix(home): reflect disabled learning schedules immediately`

Observe the schedule-enabled preference alongside times and tutor enabled/snoozed state. When scheduling is off, show “Schedule off · Learn anytime”; do not label the saved hours active. When on, distinguish current availability from configured hours; tutor off and snooze take precedence over “active.”

Acceptance: changing Settings and returning Home immediately updates the label. Check overnight windows and boundary times against the same logic used for unlock eligibility.

### 8. Make examples subject-aware

Commit: `fix(content): render examples appropriate to each subject`

Add explicit example content/type, such as scenario, worked example, or code, with compatible storage and history/retry propagation. Render prose with normal typography; reserve monospace/code presentation for actual programming lessons. Law gets a clearly hypothetical practical scenario, math a worked example, and programming a relevant code example.

For existing content, do not relabel irrelevant code as prose. Suppress clearly incompatible legacy code on nontechnical cards and offer/regenerate a corrected example through the generation path without deleting attempts or mastery. Preserve real code in programming subjects. Avoid a narrow “law” keyword check as the only classification mechanism.

Acceptance: RTI-style law content displays a practical scenario, never Python/JSON as its teaching example. New and legacy cards remain readable in unlock, detail, and retry flows.

### 9. Strengthen generation prompts and validation

Commit: `fix(ai): generate grounded subject-specific lessons and quizzes`

Use one shared contract across providers. Specify topic, learner difficulty, concept count, question count, recent concepts to avoid, and subject-appropriate example type. Treat configured topics/focus areas as data rather than instructions.

Require a concise definition, two or three explanatory points, and a useful example. For legal topics, specify jurisdiction and avoid invented citations, cases, dates, or unsupported current-law claims. Do not require legal trivia or programming metaphors. Require every quiz answer to be taught by the lesson, one unambiguous correct choice, distinct plausible distractors, and an explanation tied to the taught material. Vary correct-answer positions; do not force random question types when inappropriate.

Validate parsed data before storage: required content, allowed types, exact requested question count, appropriate option count, unique options, in-range answers, and compatible examples. Do not silently default malformed answers to option zero. Reject invalid items, use bounded regeneration/repair, and accurately report partial success. Retain backward-compatible parsing for saved content without changing historical grades. Structural validation cannot guarantee factual correctness; do not claim that it does.

Acceptance: fixture tests cover law, programming, math, malformed JSON, missing answers, duplicate choices, wrong counts, and partial responses. Review a small generated sample if configured provider access is available; never expose keys in logs.

### 10. Make generation independent of Home

Commit: `feat(generation): run concept generation with inline button progress`

Move work and state out of the composable into a shared coordinator/repository. Use a regular, unique background worker if persistent execution is needed; verify supported WorkManager APIs before adding a dependency. Do not create another foreground service for this short generation task. Coordinate manual and automatic generation so navigation or repeat taps cannot launch duplicate work.

Expose queued/running/saving/succeeded/failed states. Home observes them and renders the button states above; remove the blocking generation modal from this flow. Persist results exactly once, restore truthful state after recreation, and make cancellation/retry safe. The goal is uninterrupted use of other app screens; Android may defer work when the entire app is backgrounded, and force-stop cannot promise continued execution.

Acceptance: start generation, visit History and Settings, return, rotate, and reopen the app; progress/results remain consistent. Repeated taps do not duplicate batches. Missing key, timeout, offline, invalid response, and partial success each recover without blocking navigation.

### 11. Remove optional notifications and minimize the service notice

Commit: `fix(notifications): remove optional alerts and permission prompts`

Remove unsolicited notification permission prompts and optional notification posting; show generation success/failure inside the app only. Audit unused notification/full-screen-intent permissions and remove only those not needed by the verified unlock path. Keep the required service notification silent and minimal where the OS still displays it, and provide access to Android notification settings for an existing installation with notifications enabled.

Platform constraint: the current continuous unlock listener uses a foreground service, which must supply a notification. Android 13+ can hide it from the notification drawer when notification permission is denied, but still exposes the running service in Task Manager. Completely removing every system service notice while keeping this architecture is not a valid promise. Do not simply delete `startForeground`, replace the listener with periodic polling, or break unlock detection to hide the notice. If full removal remains essential, it requires a separate verified architectural tradeoff.

Source: [Android notification permission and foreground services](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

Acceptance: no notification permission popup, no optional alerts, no generation notification; test upgrade and fresh-install behavior with notification access both allowed and denied, and verify unlock detection remains functional. Record the unavoidable OS-visible behavior by Android version.

## Verification and handoff

Run targeted meaningful data/state tests for identity/migrations, retry resolution, recency, schedule eligibility, response validation, and duplicate work. Use the existing Gradle test/lint/compile setup rather than adding redundant tests for palette literals.

Visual acceptance requires rendered inspection, not compilation alone. Capture before/after images of the unlock reading/quiz screens, Home, History/search, and Settings in light/dark mode on a compact phone; include short-height/landscape, large fonts, gesture insets, keyboard, empty/loading/error states, and state after recreation. Check TalkBack labels and touch targets. Report which emulator/device cases were actually observed.

Finish each change with its focused check and reviewed commit. Final coding handoff should list commit hashes, completed items, checks run, and any platform constraint or unavailable device validation. Do not describe notification removal as complete if a mandatory system notice remains.
