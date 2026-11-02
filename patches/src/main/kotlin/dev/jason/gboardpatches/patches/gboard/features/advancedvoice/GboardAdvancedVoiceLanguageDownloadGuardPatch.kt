package dev.jason.gboardpatches.patches.gboard.features.advancedvoice

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

/** Stops the 18.5.4 Advanced Voice language queue from tight-looping. */
internal val gboardAdvancedVoiceLanguageDownloadGuardPatch = bytecodePatch(
    description = "Rate-limit repeated Advanced Voice language model requests.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val original = GboardMethodTarget(
            "Lsxs;",
            "c",
            listOf("Ljava/lang/String;", "Lsyk;", "Ljava/util/function/Consumer;"),
            "V",
        ).resolve(this)
        val owner = mutableClass(original.definingClass)
        val helperName = "morpheStockLanguageDownloadC"
        if (owner.methods.none { it.name == helperName }) {
            val stock = ImmutableMethod(
                original.definingClass,
                helperName,
                original.parameters,
                original.returnType,
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                original.annotations,
                original.hiddenApiRestrictions,
                original.implementation,
            ).toMutable()
            check(owner.methods.remove(original) && owner.methods.add(stock))
            owner.addHelperMethodIfMissing(
                original.name,
                original.parameterTypes.map(CharSequence::toString),
                "V",
                original.accessFlags,
                5,
                """
                    ${RuntimeCallEmitter.invoke(
                        RuntimeCallId.ADVANCED_VOICE_RUNTIME_ALLOW_LANGUAGE_DOWNLOAD,
                        "p1",
                    )}
                    move-result v0
                    if-nez v0, :allowed
                    return-void
                    :allowed
                    invoke-direct {p0, p1, p2, p3}, ${original.definingClass}->$helperName(Ljava/lang/String;Lsyk;Ljava/util/function/Consumer;)V
                    return-void
                """.trimIndent(),
            )
        }
    }
}
