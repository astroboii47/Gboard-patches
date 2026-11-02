package dev.jason.gboardpatches.patches.gboard.features.customtheme

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.addHelperMethodIfMissing
import dev.jason.gboardpatches.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import dev.jason.gboardpatches.patches.gboard.shared.runtimeabi.RuntimeCallId
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

internal val gboardStockThemeColorsPatch = bytecodePatch(
    description = "Add key and accent colors to Gboard's stock image-theme builder.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        val activityOnCreate = GboardMethodTarget(
            "Lkft;", "onCreate", listOf("Landroid/os/Bundle;"), "V",
        ).resolve(this)
        val activityReturns = activityOnCreate.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        check(activityReturns.size == 1)
        activityOnCreate.addInstructions(activityReturns.single(),
            RuntimeCallEmitter.invoke(RuntimeCallId.STOCK_THEME_COLORS_RUNTIME_ATTACH_ACTIVITY, "p0"))

        val constructor = GboardMethodTarget(
            "Lkgf;", "<init>",
            listOf("Landroid/content/Context;", "Lkge;", "Lkgh;", "I"), "V",
        ).resolve(this)
        val returns = constructor.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        check(returns.size == 1)
        constructor.addInstructions(returns.single(),
            RuntimeCallEmitter.invoke(RuntimeCallId.STOCK_THEME_COLORS_RUNTIME_ATTACH, "p0"))

        val save = GboardMethodTarget("Lkgh;", "j", listOf("Ljava/io/File;"), "Z")
            .resolve(this)
        val owner = mutableClass(save.definingClass)
        val helper = "morpheStockThemeColorsStockJ"
        if (owner.methods.none { it.name == helper }) {
            val stock = ImmutableMethod(save.definingClass, helper, save.parameters,
                save.returnType, AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                save.annotations, save.hiddenApiRestrictions, save.implementation).toMutable()
            check(owner.methods.remove(save) && owner.methods.add(stock))
            owner.addHelperMethodIfMissing("j", save.parameterTypes.map(CharSequence::toString), "Z", save.accessFlags, 3, """
                ${RuntimeCallEmitter.invoke(RuntimeCallId.STOCK_THEME_COLORS_RUNTIME_PREPARE_SAVE, "p0")}
                invoke-direct {p0, p1}, ${save.definingClass}->$helper(Ljava/io/File;)Z
                move-result v0
                ${RuntimeCallEmitter.invoke(RuntimeCallId.STOCK_THEME_COLORS_RUNTIME_POST_PROCESS, "p0, p1, v0")}
                move-result v0
                return v0
                """.trimIndent())
        }
    }
}
