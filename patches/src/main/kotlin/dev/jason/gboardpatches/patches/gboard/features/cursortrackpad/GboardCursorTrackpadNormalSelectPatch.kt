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

internal val gboardCursorTrackpadNormalSelectPatch = bytecodePatch(
    description = "在一般游標觸控板顯示 Gboard 原生的選取鍵。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        val method = GboardMethodTarget(
            "Lcom/google/android/libraries/inputmethod/freecursor/FreeCursorKeyboard;",
            "R", listOf("Lonv;"), "V",
        ).resolve(this)
        val owner = mutableClass(method.definingClass)
        val helper = "morpheNormalSelectStockR"
        if (owner.methods.none { it.name == helper }) {
            val renamed = ImmutableMethod(method.definingClass, helper, method.parameters,
                method.returnType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                method.annotations, method.hiddenApiRestrictions, method.implementation).toMutable()
            check(owner.methods.remove(method) && owner.methods.add(renamed))
            owner.addHelperMethodIfMissing("R", listOf("Lonv;"), "V", method.accessFlags, 2, """
                invoke-direct {p0, p1}, ${method.definingClass}->$helper(Lonv;)V
                ${RuntimeCallEmitter.invoke(RuntimeCallId.CURSOR_TRACKPAD_RUNTIME_SHOW_NORMAL_SELECT, "p0, p1")}
                return-void
                """.trimIndent())
        }

        // N() is Gboard's final control-state refresh. It runs after mode entry and
        // otherwise hides the native Select view again in normal (ont) mode.
        val refresh = GboardMethodTarget(
            "Lcom/google/android/libraries/inputmethod/freecursor/FreeCursorKeyboard;",
            "N", emptyList(), "V",
        ).resolve(this)
        val refreshHelper = "morpheNormalSelectStockN"
        if (owner.methods.none { it.name == refreshHelper }) {
            val renamedRefresh = ImmutableMethod(refresh.definingClass, refreshHelper, refresh.parameters,
                refresh.returnType, AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                refresh.annotations, refresh.hiddenApiRestrictions, refresh.implementation).toMutable()
            check(owner.methods.remove(refresh) && owner.methods.add(renamedRefresh))
            owner.addHelperMethodIfMissing("N", emptyList(), "V", refresh.accessFlags, 2, """
                invoke-virtual {p0}, ${refresh.definingClass}->$refreshHelper()V
                iget-object v0, p0, ${refresh.definingClass}->i:Lonv;
                ${RuntimeCallEmitter.invoke(RuntimeCallId.CURSOR_TRACKPAD_RUNTIME_SHOW_NORMAL_SELECT, "p0, v0")}
                return-void
                """.trimIndent())
        }
    }
}
