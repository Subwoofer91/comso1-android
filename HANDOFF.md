# Comso1 development handoff

Updated: 2026-10-21 (Asia/Shanghai)

## Repository state

- Repository: `https://github.com/Subwoofer91/comso1-android.git`
- Branch: `main`
- Last pushed baseline before this handoff: `d7fb016 fix: refine terminal layout and navigation`
- Current app version in the working tree: `0.4.4` (`versionCode 16`)
- Connected test device during this work: `deeb09eb` (Realme RMX3888)

## Implemented in the current working tree

- Persist model discovery results, timestamp, and success/error state.
- Automatically refresh the model catalog only when entering a conversation and the cached state is older than 20 minutes. Startup counts as entering a conversation. Manual refresh in the model picker always bypasses the cache.
- Keep the connection indicator on the last cached blue/red state while the cache is fresh.
- Make short user-message bubbles shrink to content instead of always occupying 86% width.
- Raise the empty-state greeting while retaining its keyboard movement.
- Constrain the drawer width in landscape and extend it to full available height.
- Improve the interactive terminal startup handshake, error reporting, keepalive, pinch font sizing, and unexpected-disconnect reconnect behavior.
- Debounce xterm fitting during keyboard animation and hide its scrollbar so a temporary scrollbar cannot change the visible column count.
- Track the newest xterm grid size and use it for reconnects. Only send a PTY resize when the SSH shell channel is connected.

## Open terminal issue

The remaining high-priority issue is terminal cursor/grid desynchronization at font sizes other than 14, especially after opening the software keyboard:

- Backspace can stop at the beginning of a wrapped visual line.
- In some runs the prompt appears deletable, or a space appears at the first column of another line.
- These are display symptoms of the remote PTY column count differing from xterm's local column count; Bash itself is not deleting the prompt.
- Before the final patch in this handoff, an SSH `window-change` during keyboard resize sometimes caused the shell channel to close. Automatic reconnect restored a prompt but previously reused the initial grid, which could preserve the mismatch.

The latest unverified patch does three things: it reports one stable size 360 ms after IME/font animation, sends PTY resize work from `Dispatchers.IO`, and reconnects with the latest known grid. It builds successfully but still needs real-device verification. Do not claim this terminal issue is fixed until the test matrix passes.

## Relevant files

- `app/src/main/assets/terminal/index.html`: xterm configuration, fit debounce, viewport observation.
- `app/src/main/java/com/codexremote/terminal/TerminalWebView.kt`: WebView bridge and pinch gesture.
- `app/src/main/java/com/codexremote/ssh/JschPtyTerminal.kt`: JSch shell channel, handshake, read loop, PTY resize.
- `app/src/main/java/com/codexremote/MainActivity.kt`: terminal lifecycle/UI, model refresh policy, chat/drawer layout.
- `app/src/main/java/com/codexremote/store/PreferencesConfigStore.kt`: persisted model-refresh cache.

## Next verification steps

1. Build and install:

   ```powershell
   gradle "-Duser.home=$PWD\.gradle-user-home" -g .gradle-user-home --offline --no-daemon testDebugUnitTest assembleDebug
   adb -s deeb09eb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

2. Test font sizes 14, 15, 18, and one pinch-selected size. At each size, open and close the keyboard and confirm the SSH channel stays connected.
3. Run `stty size` after every stable layout and compare the reported columns with the xterm grid. Add a temporary bridge diagnostic if the xterm column value is needed on screen.
4. Type at least 80 ASCII characters one key event at a time, delete across two wrapped rows, append a marker, and execute. Verify both the edited command and command output.
5. Rotate portrait to landscape and back. Confirm the same shell session remains active, dimensions update, and the drawer does not exceed its intended width.
6. If `ChannelShell.setPtySize` still closes the remote channel, add structured JSch logging and record the exact size sequence before changing transport libraries. Do not mask the issue by reconnecting with a stale grid.

## Build status

The full `testDebugUnitTest assembleDebug` command passed after the final resize debounce patch. It had to be run outside the workspace sandbox because the sandbox could not open `.gradle-user-home/caches/modules-2/modules-2.lock`; this was a local lock-file access issue, not a compilation failure.

## Workspace hygiene

- Do not commit `.tmp-terminal-*.png`, `.gradle-user-home`, `app/build`, or other generated output.
- Preserve the user's SSH settings and installed app data during `adb install -r` testing.
