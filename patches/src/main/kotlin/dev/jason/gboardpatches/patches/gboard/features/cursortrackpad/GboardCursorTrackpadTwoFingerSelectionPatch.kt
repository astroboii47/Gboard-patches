package dev.jason.gboardpatches.patches.gboard.features.cursortrackpad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import dev.jason.gboardpatches.patches.gboard.shared.addHelperMethodIfMissing
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

/**
 * Makes the existing normal Free Cursor surface behave like a desktop trackpad:
 * a second finger holds Shift while the primary finger moves the cursor.
 *
 * The target keyboard class is stable and public in the 18.5.4 target, but the
 * event class is obfuscated.  Passing both as Object keeps the extension ABI
 * narrow and lets the runtime fail closed when a future target drifts.
 */
internal val gboardCursorTrackpadTwoFingerSelectionPatch = bytecodePatch(
    description = "在一般游標觸控板模式中，以第二根手指暫時按住選取鍵。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val original = GboardMethodTarget(
            "Lcom/google/android/libraries/inputmethod/freecursor/FreeCursorKeyboard;",
            "m", listOf("Lohe;"), "Z",
        ).resolve(this)
        check(original.accessFlags == (AccessFlags.PUBLIC.value or AccessFlags.FINAL.value)) {
            "Unexpected FreeCursorKeyboard.m access flags"
        }
        val owner = mutableClass(original.definingClass)
        val helperName = "morpheTwoFingerSelectionStockM"
        if (owner.methods.none { it.name == helperName }) {
            val renamed = ImmutableMethod(
                original.definingClass,
                helperName,
                original.parameters,
                original.returnType,
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                original.annotations,
                original.hiddenApiRestrictions,
                original.implementation,
            ).toMutable()
            check(owner.methods.remove(original) && owner.methods.add(renamed))
            owner.addHelperMethodIfMissing(
                original.name,
                original.parameterTypes.map { it.toString() },
                "Z",
                original.accessFlags,
                3,
                """
                    ${RuntimeCallEmitter.invoke(
                        RuntimeCallId.CURSOR_TRACKPAD_RUNTIME_UPDATE_TWO_FINGER_SELECTION,
                        "p0, p1",
                    )}
                    invoke-direct {p0, p1}, ${original.definingClass}->$helperName(Lohe;)Z
                    move-result v0
                    return v0
                """.trimIndent(),
            )
        }
    }
}
