package dev.jason.gboardpatches.patches.gboard.features.cursortrackpad

import dev.jason.gboardpatches.patches.gboard.shared.GboardFlagFamilyFeature
import dev.jason.gboardpatches.patches.gboard.shared.gboardFlagFamilyFeaturePatch

internal val gboardCursorTrackpadFlagValuePatch = gboardFlagFamilyFeaturePatch(
    description = "啟用 free_cursor 與 lock mode，並將 lock detection countdown 設為零",
    feature = GboardFlagFamilyFeature.ENABLE_CURSOR_TRACKPAD_MODE,
)
