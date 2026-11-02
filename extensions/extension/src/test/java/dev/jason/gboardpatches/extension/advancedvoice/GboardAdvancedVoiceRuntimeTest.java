package dev.jason.gboardpatches.extension.advancedvoice;

import java.util.Locale;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class GboardAdvancedVoiceRuntimeTest {
    @After public void clean() {
        GboardAdvancedVoiceRuntimeSettings.clearEnabledOverrideForTest();
        GboardAdvancedVoiceRuntime.resetMddRequest();
        GboardAdvancedVoiceRuntime.resetLanguageDownloadGuardForTest();
    }
    @Test public void repeatedLanguageDownloadIsSuppressedForThirtyMinutes() {
        long minute = 60L * 1_000_000_000L;
        assertTrue(GboardAdvancedVoiceRuntime.allowLanguageDownloadAt("en-AU", minute));
        assertFalse(GboardAdvancedVoiceRuntime.allowLanguageDownloadAt("en-AU", 2L * minute));
        assertTrue(GboardAdvancedVoiceRuntime.allowLanguageDownloadAt("fr-FR", 2L * minute));
        assertTrue(GboardAdvancedVoiceRuntime.allowLanguageDownloadAt("en-AU", 31L * minute));
    }
    @Test public void downloadGuardDoesNotAlterStockWhenAdvancedVoiceIsOff() {
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(false);
        assertTrue(GboardAdvancedVoiceRuntime.allowLanguageDownload("en-AU"));
        assertTrue(GboardAdvancedVoiceRuntime.allowLanguageDownload("en-AU"));
    }
    @Test public void nativeLoaderIsConsultedOnlyWhenMasterOnAndStockNotReady() {
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(false);
        assertFalse(GboardAdvancedVoiceRuntime.shouldCheckNative(false));
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(true);
        assertTrue(GboardAdvancedVoiceRuntime.shouldCheckNative(false));
        assertFalse(GboardAdvancedVoiceRuntime.shouldCheckNative(true));
    }
    @Test public void punctuationRequiresBothSwitchesAndRequestCanRetryAfterFailure() {
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(true);
        GboardAdvancedVoiceRuntimeSettings.setZhTwPunctuationEnabledOverrideForTest(false);
        assertFalse(GboardAdvancedVoiceRuntime.claimMddRequest());
        GboardAdvancedVoiceRuntimeSettings.setZhTwPunctuationEnabledOverrideForTest(true);
        assertTrue(GboardAdvancedVoiceRuntime.claimMddRequest());
        assertFalse(GboardAdvancedVoiceRuntime.claimMddRequest());
        GboardAdvancedVoiceRuntime.resetMddRequest();
        assertTrue(GboardAdvancedVoiceRuntime.claimMddRequest());
    }
    @Test public void formatterMatchesExactLocaleAndPreservesStockConfigurationBounds() {
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(true);
        GboardAdvancedVoiceRuntimeSettings.setZhTwPunctuationEnabledOverrideForTest(true);
        assertFalse(GboardAdvancedVoiceRuntime.formatterGate(Locale.forLanguageTag("zh-TW"), false, true));
        assertTrue(GboardAdvancedVoiceRuntime.formatterGate(Locale.US, false, true));
        assertTrue(GboardAdvancedVoiceRuntime.formatterGate(Locale.forLanguageTag("zh-TW"), true, true));
        assertFalse(GboardAdvancedVoiceRuntime.formatterGate(Locale.forLanguageTag("zh-TW"), false, false));
        GboardAdvancedVoiceRuntimeSettings.setEnabledOverrideForTest(false);
        assertTrue(GboardAdvancedVoiceRuntime.formatterGate(Locale.forLanguageTag("zh-TW"), false, true));
    }
}
