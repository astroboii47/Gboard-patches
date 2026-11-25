import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

extension {
    name = "extensions/gboard-calculator.rve"
}

val generatedShortcodeDataDir = layout.buildDirectory.dir(
    "generated/source/shortcodeData/java"
)

android.sourceSets.named("main") {
    java.directories.add(generatedShortcodeDataDir.get().asFile.absolutePath)
}

val generateShortcodeData = tasks.register("generateShortcodeData") {
    val emojiFile = layout.projectDirectory.file("src/main/assets/emoji_shortcodes.json")
    val symbolFile = layout.projectDirectory.file("src/main/assets/symbol_shortcodes.json")
    val outputFile = generatedShortcodeDataDir.map { directory ->
        directory.file(
            "dev/jason/gboardpatches/extension/calculator/GboardShortcodeData.java"
        )
    }
    inputs.files(emojiFile, symbolFile)
    outputs.file(outputFile)
    doLast {
        fun compressedChunks(file: java.io.File): String {
            val compressed = ByteArrayOutputStream()
            GZIPOutputStream(compressed).use { it.write(file.readBytes()) }
            val base64 = Base64.getEncoder().encodeToString(compressed.toByteArray())
            return base64.chunked(8000).joinToString(",\n") { "            \"$it\"" }
        }
        val emojiChunks = compressedChunks(emojiFile.asFile)
        val symbolChunks = compressedChunks(symbolFile.asFile)
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            package dev.jason.gboardpatches.extension.calculator;

            import android.util.Base64;
            import java.io.ByteArrayInputStream;
            import java.io.ByteArrayOutputStream;
            import java.nio.charset.StandardCharsets;
            import java.util.zip.GZIPInputStream;

            final class GboardShortcodeData {
                private static final String[] EMOJI = new String[] {
$emojiChunks
                };
                private static final String[] SYMBOL = new String[] {
$symbolChunks
                };
                private GboardShortcodeData() { }
                static String emojiJson() throws Exception { return decode(EMOJI); }
                static String symbolJson() throws Exception { return decode(SYMBOL); }
                private static String decode(String[] chunks) throws Exception {
                    StringBuilder encoded = new StringBuilder();
                    for (String chunk : chunks) encoded.append(chunk);
                    byte[] compressed = Base64.decode(encoded.toString(), Base64.DEFAULT);
                    ByteArrayOutputStream plain = new ByteArrayOutputStream();
                    try (GZIPInputStream input = new GZIPInputStream(
                            new ByteArrayInputStream(compressed))) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) >= 0) plain.write(buffer, 0, count);
                    }
                    return new String(plain.toByteArray(), StandardCharsets.UTF_8);
                }
            }
            """.trimIndent()
        )
    }
}

android {
    namespace = "dev.jason.gboardpatches.calculator"

    defaultConfig {
        minSdk = 26
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        checkOnly += "NewApi"
    }
}

tasks.named("syncExtension") {
    dependsOn("lintDebug")
}

tasks.named("preBuild") {
    dependsOn(generateShortcodeData)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
