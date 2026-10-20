# Comso1 development handoff

Updated: 2026-10-21 (Asia/Shanghai)

## Repository state

- Repository: `https://github.com/Subwoofer91/comso1-android.git`
- Branch: `main`
- Last pushed baseline before this round: `6f260e5 feat: cache model discovery and stabilize terminal sizing`
- Current app version in the working tree: `0.4.5` (`versionCode 17`)
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

## Relevant files

- `app/src/main/assets/terminal/index.html`: xterm configuration, fit debounce, hidden
  scrollbar correction, size heartbeat.
- `app/src/main/java/com/codexremote/terminal/TerminalWebView.kt`: WebView bridge and
  pinch gesture.
- `app/src/main/java/com/codexremote/ssh/JschPtyTerminal.kt`: JSch session, desired PTY
  grid, serialized connection, read loop, resize application.
- `app/src/main/java/com/codexremote/MainActivity.kt`: terminal lifecycle/UI, keyboard
  insets, landscape app-bar hiding, drawer width cap, model refresh policy.
- `app/src/main/java/com/codexremote/store/PreferencesConfigStore.kt`: persisted
  model-refresh cache.

## Build status

`testDebugUnitTest assembleDebug` passes inside the workspace sandbox. The previous
handoff needed to run Gradle outside the sandbox because `.gradle-user-home/caches/modules-2`
was a junction to `C:\Users\Subwoofer\.gradle\caches\modules-2`, which is outside the
sandbox write grant. That junction has been replaced by a real directory inside the
workspace (the global cache was copied in), so builds no longer need elevated access.

## Remaining notes

- In landscape with the IME open only ~3 terminal rows fit above the Sogou keyboard and
  the extra-key row. This is a screen-height limitation, not a sizing mismatch.
- The app still exits on Back from terminal mode; there is no BackHandler that returns to
  chat. This is unchanged and was not part of the sizing work.
- Do not commit `.tmp-*.png`, `work/`, `.gradle-user-home`, `app/build`, or other
  generated output.
- Preserve the user's SSH settings and installed app data during `adb install -r` testing.
