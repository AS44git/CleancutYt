/*
 * Copyright 2026 CleancutYt contributors
 * https://github.com/AS44git/CleancutYt
 *
 * Part of CleancutTube, a fork of Morphe's patches (GPL-3.0). Not affiliated with YouTube or Google.
 */
package app.morphe.patches.youtube.cleancut

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.layout.hide.endscreensuggestedvideo.hideEndScreenSuggestedVideoPatch
import app.morphe.patches.youtube.layout.hide.relatedvideos.hideRelatedVideosPatch
import app.morphe.patches.youtube.layout.startpage.changeStartPagePatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.AccessFlags

private const val EXTENSION = "Lapp/morphe/extension/youtube/cleancut/Cleancut;"

/** The class behind YouTube's Autoplay switch: its constructor reads the "main_app_autonav" pref. */
private object CleancutAutoNavConstructorFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf("main_app_autonav"),
)

/** Its "is Autoplay on" getter, which the player asks as a video ends. */
private object CleancutAutoNavStatusFingerprint : Fingerprint(
    classFingerprint = CleancutAutoNavConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
)

@Suppress("unused")
val cleancutTubePatch = bytecodePatch(
    name = "CleancutTube",
    description = "Turns YouTube into search and your You page: no Home feed, Shorts, " +
        "Subscriptions, related videos, autoplay or end-screen suggestions. Opens on search. " +
        "Locked on: nothing in the app turns it off.",
) {
    // The settings it locks come with these; the lock itself is in the extension.
    dependsOn(
        sharedExtensionPatch,
        changeStartPagePatch,
        hideEndScreenSuggestedVideoPatch,
        hideRelatedVideosPatch,
    )
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        // Autoplay never moves on to another video: the getter the player asks answers off.
        // The getter is tiny (no locals of its own), so the answer goes in p0, which nothing
        // reads again once the method returns.
        CleancutAutoNavStatusFingerprint.method.addInstructions(
            0,
            """
                const/4 p0, 0x0
                invoke-static { p0 }, $EXTENSION->autoplayEnabled(Z)Z
                move-result p0
                return p0
            """,
        )
    }
}
