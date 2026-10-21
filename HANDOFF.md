# Comso1 development handoff

Updated: 2026-10-21 (Asia/Shanghai)

## Repository state

- Repository: `https://github.com/Subwoofer91/comso1-android.git`
- Branch: `main`
- Last pushed baseline before this round: `c658e0d feat: start new Codex sessions with workspace-write profile`
- Current app version in the working tree: `0.4.7` (`versionCode 19`)
- Connected test device during this work: `deeb09eb` (Realme RMX3888, Android 16)

## Terminal fixes in this round

The cursor/grid desynchronization reported in the previous handoff was reproduced and
fixed. The local xterm grid and the remote PTY are now kept in lockstep:

- `MainActivity` lays the terminal out in a `Column` and pads it with
  `WindowInsets.safeDrawing` at the bottom, so the WebView shrinks when the IME opens
  instead of being covered by it. The IME-driven height change now reaches the PTY.
- PTY resize requests are serialized through a conflated `Channel` and applied on one
  worker, so a burst of fits cannot be applied out of order.
- `JschPtyTerminal` records the newest requested grid even before the shell channel is
  connected and applies it right after `connect()`. The old code silently dropped those
  requests, which left the PTY at its creation size.
- Only one `run()` session may exist at a time (`Mutex`). Previously a reconnect could
  start a second session while the cancelled one was still in its blocking read loop;
  the stale `finally` block then cleared the new session's channel and PTY updates
  stopped. Double-tapping refresh now serializes reconnects.
- xterm now uses the full width: the bundled xterm reserves 15px for a scrollbar when it
  measures 0, so the hidden scrollbar no longer costs one or two columns (39 → 41 in
  portrait at font size 14).
- The xterm grid is re-asserted every 1.5s. `JschPtyTerminal` ignores duplicate grids, so
  this only heals a resize dropped during a handshake or a late layout change.
- Landscape + IME no longer squeezes the terminal to zero rows: the top bar is hidden
  while typing in landscape and the system status bar is temporarily hidden, leaving
  about three rows above the keyboard. Both return when the IME closes.
- The landscape drawer no longer leaves a ~60dp strip of the closed sheet over the
  terminal. Material3 anchors a closed drawer at `DrawerDefaults.MaximumDrawerWidth`
  (360dp); the previous 420dp sheet exceeded that cap. Drawer width is now capped at the
  same 360dp.

## Permission profile for every turn + in-app approval (0.4.7)

The first implementation only passed the relaxed profile to a brand-new thread. The
second `codex exec resume` turn fell back to `read-only` + `approval_policy=never`, which
is what the user observed. Fixed and extended:

- `codex exec resume` rejects `--approve-for-me`/`--sandbox`, but it accepts config
  overrides. Resumed turns now pass `-c sandbox_mode=workspace-write`,
  `-c approval_policy=on-request`, `-c approvals_reviewer=auto_review`,
  `-c sandbox_workspace_write.network_access=true` and the writable-roots array.
- `ShellCommand.codexExec` also accepts `extraWritableRoots` for explicit escalation.
- Rollout metadata of an app thread now shows workspace-write/on-request/auto_review on
  **both** turn contexts (the 21:44 thread `01a15eb5`), not just the first.
- Codex keeps `~/.codex`, `~/.git`, `~/.aws` read-only even inside the workspace. The
  chat top bar now has a lock button that opens a "沙箱权限" dialog with a switch for
  "允许修改 ~/.codex". While enabled (per app run), every message adds
  `<workDir>/.codex` to `writable_roots`.
- If a command still fails with `Read-only file system`/`Permission denied` and Codex's
  automatic reviewer did not resolve it, the app records the failing path. After the
  turn ends it shows an approval dialog; approving retries the turn with that path's
  parent directory added to the writable roots.
- `detectSandboxDenial` has unit tests in `SandboxDenialTest`.

`codex exec` has no interactive per-command approval channel: the JSON stream exposes no
approval events, and auto-review runs inside the CLI. The app's approval model is therefore
(a) auto-review for ordinary escalations, and (b) explicit user approval via the lock
dialog or the post-hoc denial dialog. A true per-command approval UI would require moving
the chat transport to `codex app-server` and its JSON-RPC approval requests.

## Backend routing issue (transient)

Between roughly 20:00 and 21:00 on 2026-10-21 new `codex exec` sessions stalled with
`Reconnecting... N/5 (workspace routing discovery failed)` and `rmcp::transport` errors for
`https://chatgpt.com/backend-api/ps/mcp`. This was reproduced from the app and from manual
CLI runs, with `--no-daemon`, `approval_policy=never`, `approvals_reviewer=user`, and no
extra writable roots, so it was not caused by the permission flags. It stopped reproducing
around 21:07; the 21:07, 21:21, 21:44 and 21:51 sessions completed normally. If it returns,
check the VPS's route to the Codex backend and the account's workspace routing.

## Verification performed on `deeb09eb`

- Initial portrait layout at font 14: xterm `41x32`; `stty size` → `32 41`.
- Keyboard open at font 15: xterm and PTY `38x13`; `stty size` → `13 38`; the echoed
  command wrapped exactly at column 38.
- Font 16 with keyboard open: `35x12`; `stty size` → `12 35`. Keyboard closed:
  `35x26`; `stty size` → `26 35`.
- Rotated with the keyboard open and closed: PTY `92x9` and `41x32`; the same shell PID
  (`echo $$` → `190245`) survived both rotations.
- Typed an 87-character command one key event at a time, deleted six characters across a
  wrapped row, appended `ZZZ`, and executed it: the edited line and command output were
  both correct.
- Double-tapped refresh: one PTY creation per tap, no concurrent session; afterwards
  `stty size` and `echo RECONNECT_OK` worked.
- Open/close the keyboard in landscape: terminal stays visible above the IME, status bar
  and top bar are restored after the IME closes.
- Two-turn app thread (21:44, `01a15eb5`): both `turn_context` entries record
  `sandbox_policy=workspace-write`, `approval_policy=on-request`,
  `approvals_reviewer=auto_review`.
- Enabled "允许修改 ~/.codex" in the lock dialog, then asked Codex to create
  `/home/codex/.codex/app_perm_test2.txt`. It succeeded (`OK3`), and the rollout recorded
  `writable_roots=['/home/codex/.codex','/srv/agent']` with write entries for `.codex`.
- Unit tests: `newSessionRequestsWorkspaceWriteProfile`,
  `extraWritableRootsAreMergedForSandboxEscalation`, `SandboxDenialTest`.

## Relevant files

- `app/src/main/assets/terminal/index.html`: xterm configuration, fit debounce, hidden
  scrollbar correction, size heartbeat.
- `app/src/main/java/com/codexremote/terminal/TerminalWebView.kt`: WebView bridge and
  pinch gesture.
- `app/src/main/java/com/codexremote/ssh/JschPtyTerminal.kt`: JSch session, desired PTY
  grid, serialized connection, read loop, resize application.
- `app/src/main/java/com/codexremote/MainActivity.kt`: terminal lifecycle/UI, keyboard
  insets, landscape app-bar hiding, drawer width cap, model refresh policy, sandbox
  permission dialog and denial detection.
- `app/src/main/java/com/codexremote/domain/CodexRemoteSession.kt`: chat transport and
  extra writable roots for approved escalations.
- `app/src/main/java/com/codexremote/protocol/ShellCommand.kt`: `codex exec` command,
  new-session profile and resumed-turn config overrides.
- `app/src/test/java/com/codexremote/protocol/CodexJsonlParserTest.kt`: command/profile
  assertions.
- `app/src/test/java/com/codexremote/SandboxDenialTest.kt`: read-only path detection.
- `app/src/main/java/com/codexremote/store/PreferencesConfigStore.kt`: persisted
  model-refresh cache.

## Build status

`testDebugUnitTest assembleDebug` passes inside the workspace sandbox. The previous
handoff needed to run Gradle outside the sandbox because `.gradle-user-home/caches/modules-2`
was a junction to `C:\Users\Subwoofer\.gradle\caches\modules-2`, which is outside the
sandbox write grant. That junction has been replaced by a real directory inside the
workspace (the global cache was copied in), so builds no longer need elevated access.

## Remaining notes

- The "允许修改 ~/.codex" switch is remembered for the current app state (saveable) but is
  not persisted as a preference; it resets on a cold start for safety.
- True per-command approval prompts (pause a running command and ask yes/no) need the
  `codex app-server` JSON-RPC transport. The current app handles approvals via auto-review
  plus the lock dialog / post-hoc denial dialog.
- In landscape with the IME open only ~3 terminal rows fit above the Sogou keyboard and
  the extra-key row. This is a screen-height limitation, not a sizing mismatch.
- The app still exits on Back from terminal mode; there is no BackHandler that returns to
  chat. This is unchanged and was not part of the sizing work.
- Do not commit `.tmp-*.png`, `work/`, `.gradle-user-home`, `app/build`, or other
  generated output.
- Preserve the user's SSH settings and installed app data during `adb install -r` testing.
