package dev.jason.gboardpatches.patches.gboard.features.cursortrackpad

import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.gboardFlagFamilyFeaturePatch

internal val gboardCursorTrackpadFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "啟用 free_cursor 與 lock mode，並使 lock detection 安全地近乎即時完成",
    feature = GboardFlagFamilyFeature.ENABLE_CURSOR_TRACKPAD_MODE,
)
