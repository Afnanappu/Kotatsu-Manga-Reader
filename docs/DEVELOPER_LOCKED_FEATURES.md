# Developer-Locked Features

Some settings in this app can be **hard-locked** by the developer: the switch/menu item
stays visible and tappable in the UI so users understand the option still exists, but the
underlying value can no longer actually change. Tapping it shows an explanatory message
instead of doing nothing silently.

This document explains the pattern currently used for the **"Disable NSFW"** switch, and
how to reuse, enable, or revert it (for this setting or a similar one in the future).

---

## 1. Where the lock lives

The lock is implemented in three places for `isNsfwContentDisabled`:

| Layer | File | What it does |
| --- | --- | --- |
| Data layer | [`AppSettings.kt`](app/src/main/kotlin/org/koitharu/kotatsu/core/prefs/AppSettings.kt#L122) | The `isNsfwContentDisabled` property getter/setter always resolve to `true`, no matter what is stored in `SharedPreferences` or passed to the setter. This is the source of truth — even if a UI layer were bypassed, this guarantees the app-wide behavior stays disabled. |
| Settings screen | [`SourcesSettingsFragment.kt`](app/src/main/kotlin/org/koitharu/kotatsu/settings/sources/SourcesSettingsFragment.kt) | Forces the `SwitchPreferenceCompat` (`key = "no_nsfw"`) to display checked, and registers an `OnPreferenceChangeListener` that rejects any attempt to uncheck it (`false`), showing a `Toast` with `R.string.nsfw_locked_by_developer` and returning `false` so the switch visually snaps back to checked. |
| Sources manager menu | [`SourcesManageFragment.kt`](app/src/main/kotlin/org/koitharu/kotatsu/settings/sources/manage/SourcesManageFragment.kt#L170) | The checkable toolbar menu item (`R.id.action_no_nsfw`) shows the same `Toast` instead of unchecking when tapped while already checked. |

The user-facing message lives in `strings.xml`:

```xml
<string name="nsfw_locked_by_developer">This has been permanently disabled by the developer for safety reasons and cannot be turned off</string>
```

---

## 2. How to re-enable user control (undo the lock)

The `AppSettings.kt` change was written so the original logic is preserved as a comment,
making the revert a one-line swap:

```kotlin
// Currently (locked):
var isNsfwContentDisabled: Boolean
    get() = true // prefs.getBoolean(KEY_DISABLE_NSFW, false)
    set(value) = prefs.edit { putBoolean(KEY_DISABLE_NSFW, true) } // putBoolean(KEY_DISABLE_NSFW, value)

// To restore user control, replace with:
var isNsfwContentDisabled: Boolean
    get() = prefs.getBoolean(KEY_DISABLE_NSFW, false)
    set(value) = prefs.edit { putBoolean(KEY_DISABLE_NSFW, value) }
```

After that, also undo the two UI-layer locks so the switch behaves normally again:

1. In `SourcesSettingsFragment.kt`, remove (or comment out) the block that forces
   `isChecked = true` and the `setOnPreferenceChangeListener` that blocks unchecking.
2. In `SourcesManageFragment.kt`, restore the menu handler to its original form:

   ```kotlin
   R.id.action_no_nsfw -> {
       settings.isNsfwContentDisabled = !menuItem.isChecked
       true
   }
   ```

No other files need to change — every other consumer of `isNsfwContentDisabled`
(`MangaSourcesRepository`, `SearchV2Helper`, `MangaListViewModel`, `SuggestionsWorker`,
`TrackerNotificationHelper`, etc.) just reads the property and will automatically follow
whatever the getter above returns.

---

## 3. How to apply the same pattern to a different setting

To hard-lock any other boolean setting (e.g. some future safety toggle) the same way:

1. **Data layer**: in `AppSettings.kt`, hardcode the getter to the "safe" value and make
   the setter always persist that same value, keeping the real logic commented above/below
   it for easy revert.
2. **UI layer(s)**: find every `SwitchPreferenceCompat` / checkable `MenuItem` bound to that
   setting and:
   - Force it to reflect the locked value when the screen is created / the menu is prepared.
   - Attach a change listener (`setOnPreferenceChangeListener` for preferences, or an
     `if (menuItem.isChecked) { ... } else { ... }` guard for menu items) that intercepts any
     attempt to move away from the locked value, shows a `Toast`/`Snackbar` explaining why,
     and returns `false` / re-applies the locked state.
3. **String resource**: add (or reuse) a message string explaining the lock, e.g.
   `nsfw_locked_by_developer`, and reference it from the toast/snackbar.
4. **Do not remove the control** — keep it visible and tappable. Users should be able to see
   the setting exists and get an explanation, rather than the option silently disappearing.

---

## 4. Notes / caveats

- This lock is a **client-side UI/logic restriction**, not a build flag or remote config.
  Anyone building the app from source can revert it by editing the code above — that's
  expected and by design (open-source app), not a security boundary.
- Because the `AppSettings` getter is the single source of truth, this lock is effective
  even against any code path that writes directly to `SharedPreferences` under the
  `no_nsfw` key — reads always come back `true`.
- If more preference screens/entry points are added later that touch `KEY_DISABLE_NSFW`,
  remember to add the same "attempt to uncheck → toast" guard there too, otherwise a stray
  screen could look like it toggles off even though the underlying setting won't budge.
