/*
 * Copyright 2026 CleancutYt contributors
 * https://github.com/AS44git/CleancutYt
 *
 * Part of CleancutTube, a fork of Morphe's patches (GPL-3.0). Not affiliated with YouTube or Google.
 */
package app.morphe.patches.youtube.cleancut

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.misc.litho.filter.addLithoFilter
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequestUrl
import app.morphe.patches.youtube.layout.hide.endscreensuggestedvideo.hideEndScreenSuggestedVideoPatch
import app.morphe.patches.youtube.layout.hide.relatedvideos.hideRelatedVideosPatch
import app.morphe.patches.youtube.layout.startpage.changeStartPagePatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.morphe.patches.youtube.misc.navigation.addBrowseIdHook
import app.morphe.patches.youtube.misc.navigation.addSearchResultsHook
import app.morphe.patches.youtube.misc.navigation.browseIdHookPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION = "Lapp/morphe/extension/youtube/cleancut/Cleancut;"
private const val UPDATER = "Lapp/morphe/extension/youtube/cleancut/Updater;"
private const val INSTALL_PERMISSION = "android.permission.REQUEST_INSTALL_PACKAGES"

/** YouTube's main activity being created: where the updater's quiet check starts. */
private object CleancutMainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

/** Lets the updater hand a downloaded release to Android's package installer. */
private val cleancutInstallPermissionPatch = resourcePatch {
    execute {
        // Updates: a quiet check of this repository's latest release a few seconds after the
        // main screen opens.
        CleancutMainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $UPDATER->onMainActivity(Landroid/app/Activity;)V",
        )

        document("AndroidManifest.xml").use { xml ->
            val manifest = xml.documentElement
            val permissions = xml.getElementsByTagName("uses-permission")
            val declared = (0 until permissions.length)
                .any { (permissions.item(it) as Element).getAttribute("android:name") == INSTALL_PERMISSION }
            if (!declared) {
                val element = xml.createElement("uses-permission")
                element.setAttribute("android:name", INSTALL_PERMISSION)
                manifest.insertBefore(element, manifest.firstChild)
            }
        }
    }
}

private const val HOME_FILTER = "Lapp/morphe/extension/youtube/patches/components/CleancutHomeFilter;"

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

private const val SWIPE_REFRESH_LAYOUT = "Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;"

/**
 * The pull-to-refresh circle's "start or stop refreshing" (androidx setRefreshing, renamed by R8;
 * the only public final (boolean, boolean) method of the class, private one aside).
 */
private object CleancutSetRefreshingFingerprint : Fingerprint(
    definingClass = SWIPE_REFRESH_LAYOUT,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "Z"),
)

/** Its touch handling, which starts a pull. */
private object CleancutRefreshInterceptTouchFingerprint : Fingerprint(
    definingClass = SWIPE_REFRESH_LAYOUT,
    name = "onInterceptTouchEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
)

/**
 * The "loading more" row YouTube puts at the end of a list while the next page loads: its
 * constructor inflates the loading_status_view layout into a FrameLayout.
 */
private object CleancutLoadingRowFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "loading_status_view"),
        methodCall(smali = "Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;"),
        checkCast(type = "Landroid/widget/FrameLayout;", location = MatchAfterWithin(3)),
    ),
)

@Suppress("unused")
val cleancutTubePatch = bytecodePatch(
    name = "CleancutTube",
    description = "Turns YouTube into a search page and your You page: no Home feed, Shorts, " +
        "Subscriptions, related videos, autoplay or end-screen suggestions. " +
        "Locked on: nothing in the app turns it off.",
) {
    // The settings it locks come with these; the lock itself is in the extension.
    dependsOn(
        sharedExtensionPatch,
        changeStartPagePatch,
        hideEndScreenSuggestedVideoPatch,
        hideRelatedVideosPatch,
        lithoFilterPatch,
        browseIdHookPatch,
        buildRequestPatch,
        cleancutInstallPermissionPatch,
    )
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        // The search page: Home's feed dropped element by element, so only the toolbar and its
        // search bar are left. The filter knows the page shown from the browse id hook.
        addBrowseIdHook("$EXTENSION->setBrowseId(Ljava/lang/String;)V")
        addSearchResultsHook("$EXTENSION->clearBrowseId()V")
        addLithoFilter(HOME_FILTER)
        // With nothing to show, the feed would keep asking for its next page; after the first
        // one, Home's page requests fail before they leave the phone.
        hookBuildRequestUrl("$EXTENSION->requestUrl")
        // ...the "loading more" row stays hidden on Home (Home's list is empty, so the row would
        // sit under the search bar spinning for a page that isn't coming)...
        CleancutLoadingRowFingerprint.let {
            it.method.apply {
                val castIndex = it.instructionMatches.last().index
                val row = getInstruction<OneRegisterInstruction>(castIndex).registerA
                addInstruction(castIndex + 1, "invoke-static { v$row }, $EXTENSION->onLoadingRow(Landroid/view/View;)V")
            }
        }

        // ...and the pull-to-refresh circle under the search bar never spins on Home: it would
        // wait for a page that isn't coming.
        CleancutSetRefreshingFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION->refreshing(Z)Z
                move-result p1
            """,
        )
        CleancutRefreshInterceptTouchFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION->refreshBlocked()Z
                move-result v0
                if-eqz v0, :cleancut_pull
                const/4 v0, 0x0
                return v0
            """,
            ExternalLabel("cleancut_pull", CleancutRefreshInterceptTouchFingerprint.method.getInstruction(0)),
        )

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
