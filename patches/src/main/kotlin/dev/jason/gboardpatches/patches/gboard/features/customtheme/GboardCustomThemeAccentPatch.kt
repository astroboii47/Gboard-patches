package dev.jason.gboardpatches.patches.gboard.features.customtheme

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

/** Overrides only the stock action-key colour after Gboard resolves a theme selector. */
internal val gboardCustomThemeAccentPatch = bytecodePatch(
    description = "讓自訂主題的重音色套用至 Gboard 動作鍵。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)
    execute {
        val method = GboardMethodTarget(
            "Lrno;", "a", listOf("Ljava/lang/String;"),
            "Landroid/content/res/ColorStateList;",
        ).resolve(this)
        val owner = mutableClass(method.definingClass)
        val helper = "morpheAccentStockA"
        if (owner.methods.none { it.name == helper }) {
            val renamed = ImmutableMethod(
                method.definingClass, helper, method.parameters, method.returnType,
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value, method.annotations,
                method.hiddenApiRestrictions, method.implementation,
            ).toMutable()
            check(owner.methods.remove(method) && owner.methods.add(renamed))
            owner.addHelperMethodIfMissing("a", listOf("Ljava/lang/String;"),
                "Landroid/content/res/ColorStateList;", method.accessFlags, 3, """
            invoke-direct {p0, p1}, ${method.definingClass}->$helper(Ljava/lang/String;)Landroid/content/res/ColorStateList;
            move-result-object v0
            ${RuntimeCallEmitter.invoke(
                RuntimeCallId.CUSTOM_THEME_RUNTIME_OVERRIDE_ACTION_KEY_COLOR,
                "p0, p1, v0",
            )}
            move-result-object v0
            return-object v0
            """.trimIndent())
        }
    }
}
