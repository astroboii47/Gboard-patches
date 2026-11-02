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

internal val gboardSmoothBackspaceSwipePatch = bytecodePatch(
    description = "Add optional fixed-step character/word backspace scrubbing.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        val detect = GboardMethodTarget(
            "Lcom/google/android/libraries/inputmethod/motioneventhandler/scrubmove/ScrubDeleteMotionEventHandler;",
            "b", listOf("Landroid/view/MotionEvent;"), "Z",
        ).resolve(this)
        val detectOwner = mutableClass(detect.definingClass)
        val detectHelper = "morpheSmoothDeleteStockB"
        if (detectOwner.methods.none { it.name == detectHelper }) {
            val stock = ImmutableMethod(detect.definingClass, detectHelper, detect.parameters,
                detect.returnType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                detect.annotations, detect.hiddenApiRestrictions, detect.implementation).toMutable()
            check(detectOwner.methods.remove(detect) && detectOwner.methods.add(stock))
            detectOwner.addHelperMethodIfMissing("b", detect.parameterTypes.map(CharSequence::toString), "Z",
                detect.accessFlags, 2, """
                ${RuntimeCallEmitter.invoke(RuntimeCallId.BACKSPACE_SWIPE_RUNTIME_CONFIGURE, "p0")}
                invoke-direct {p0, p1}, ${detect.definingClass}->$detectHelper(Landroid/view/MotionEvent;)Z
                move-result v0
                return v0
                """.trimIndent())
        }

        val step = GboardMethodTarget("Lozv;", "e", listOf("I"), "V").resolve(this)
        val stepOwner = mutableClass(step.definingClass)
        val stepHelper = "morpheSmoothDeleteStockE"
        if (stepOwner.methods.none { it.name == stepHelper }) {
            val stock = ImmutableMethod(step.definingClass, stepHelper, step.parameters,
                step.returnType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                step.annotations, step.hiddenApiRestrictions, step.implementation).toMutable()
            check(stepOwner.methods.remove(step) && stepOwner.methods.add(stock))
            stepOwner.addHelperMethodIfMissing("e", step.parameterTypes.map(CharSequence::toString), "V",
                step.accessFlags, 3, """
                ${RuntimeCallEmitter.invoke(RuntimeCallId.BACKSPACE_SWIPE_RUNTIME_HANDLE_CHARACTER_STEP, "p0, p1")}
                move-result v0
                if-eqz v0, :stock
                return-void
                :stock
                invoke-direct {p0, p1}, ${step.definingClass}->$stepHelper(I)V
                return-void
                """.trimIndent())
        }
    }
}
