# 0.3.3

Fixed:
* Indentation

# 0.3.2

Fixed:
* Intermittent crash caused by attempting to show a notification without a subject

Misc:
* Updated `backpack` to `4.2.0`

# 0.3.1

Fixed:
* Intermittent crash caused by The end instant must be greater than the start instant #6

Misc:
* Updated `backpack` to `4.0.1`
* Updated `clj-time` to `0.15.2`
* Updated `encore` to `2.117.0`

# 0.3.0

Added:
* `--notify` flag to notify of a failed build during `--watch`. Supports `osascript` and `terminal-notifier`

# 0.2.0

Added:
* `:build-link` and `:workflow-link` columns for TERM supported hyperlinking
* `projects` sub-command to print a list of followed projects

Fixed:
* Watch mode now clears scrollback for TERM supported terminals
* Tables print as markdown compatible (for copying and pasting)
* Project filter is now project specific (instead of from all recent)
* Shortened humanised time output
* Removed Timezone offset from formatted dates

Misc:
* Upgraded `backpack` dependency

# 0.1.1

Fixed:
* Screen now clears when watch arg is specified for recent list
* Recent now uses recur instead to save on call stack

# 0.1.0

Added:
* `--watch` argument to refresh recent activity without exiting the JVM

# 0.0.2

Fixed:
* Removed `gen-class`
* Added dependency exclusions

# 0.0.1

Initial release
