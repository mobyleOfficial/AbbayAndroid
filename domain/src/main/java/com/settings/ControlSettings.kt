package com.settings

/** "On short interruptions". */
enum class ShortInterruptionBehavior { PAUSE, LOWER_VOLUME }

/** What a headset's double press / NEXT and triple press / PREVIOUS do. */
enum class HeadsetSkipAction { SKIP, CHAPTER, TRACK }

/** What a headset's long press does. */
enum class HeadsetLongPressAction { BOOKMARK, SLEEP_TIMER, NONE }
