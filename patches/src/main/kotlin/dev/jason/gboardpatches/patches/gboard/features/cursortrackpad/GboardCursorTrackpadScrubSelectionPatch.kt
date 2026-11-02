package dev.jason.gboardpatches.patches.gboard.features.cursortrackpad

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.addHelperMethodIfMissing
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardCursorTrackpadScrubSelectionPatch = bytecodePatch(
    description = "讓一般空白鍵游標觸控板的第二根手指暫時選取文字。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        val method = GboardMethodTarget(
            "Lcom/google/android/libraries/inputmethod/motioneventhandler/scrubmove/ScrubMotionEventHandler;",
            "g", listOf("Landroid/view/MotionEvent;"), "V",
        ).resolve(this)
        val owner = mutableClass(method.definingClass)
        val helper = "morpheScrubSelectStockG"
        if (owner.methods.none { it.name == helper }) {
            val renamed = ImmutableMethod(method.definingClass, helper, method.parameters,
                method.returnType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                method.annotations, method.hiddenApiRestrictions, method.implementation).toMutable()
            check(owner.methods.remove(method) && owner.methods.add(renamed))
            owner.addHelperMethodIfMissing("g", listOf("Landroid/view/MotionEvent;"), "V",
                method.accessFlags, 2, """
                invoke-direct {p0, p1}, ${method.definingClass}->$helper(Landroid/view/MotionEvent;)V
                ${RuntimeCallEmitter.invoke(RuntimeCallId.CURSOR_TRACKPAD_RUNTIME_UPDATE_SCRUB_SELECTION, "p0, p1")}
                return-void
                """.trimIndent())
        }
    }
}
