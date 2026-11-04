/*
 * Copyright 2026 CleancutTube contributors
 * https://github.com/AS44git/CleancutTube
 *
 * Part of CleancutTube, a fork of Morphe's patches (GPL-3.0). Not affiliated with YouTube or Google.
 */
package app.morphe.extension.youtube.cleancut;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.youtube.patches.ChangeStartPagePatch.StartPage;
import app.morphe.extension.youtube.patches.OpenShortsInRegularPlayerPatch.ShortsPlayerType;
import app.morphe.extension.youtube.settings.Settings;

/**
 * CleancutTube: YouTube as search and your You page, with no feed to fall into.
 *
 * <ul>
 *   <li>The bottom bar holds Search and You. Home, Shorts, Subscriptions, Create and
 *       Notifications are gone, and the app opens on search.</li>
 *   <li>Home is not just hidden: the page YouTube falls back to (its
 *       "FEwhat_to_watch") is replaced with the You page wherever YouTube asks for it, so
 *       pressing back never lands on recommendations ({@link #homeBrowseId}).</li>
 *   <li>The watch page shows the video, its comments and its channel: no related videos, no
 *       autoplay, no end-screen suggestions, no mixes.</li>
 *   <li>Shorts open as a normal video with nothing to swipe to, and Shorts shelves are hidden
 *       everywhere.</li>
 * </ul>
 *
 * <p>All of it is locked in code ({@link app.morphe.extension.shared.settings.Setting#lockTo}):
 * no switch, import or reset in the app moves it. The way out is uninstalling.
 */
public final class Cleancut {
    /** The page that replaces Home: the You tab (YouTube's library). */
    public static final String HOME_REPLACEMENT_BROWSE_ID = "FElibrary";

    private Cleancut() {
    }

    private static final BooleanSetting[] LOCKED_ON = {
            // Bottom bar: Search and You only.
            Settings.HIDE_HOME_BUTTON,
            Settings.HIDE_SHORTS_BUTTON,
            Settings.HIDE_SUBSCRIPTIONS_BUTTON,
            Settings.HIDE_CREATE_BUTTON,
            Settings.HIDE_NOTIFICATIONS_BUTTON,
            Settings.SHOW_SEARCH_BUTTON,
            Settings.HIDE_EXPLORE_BUTTON,
            // Watch page: the video and nothing after it.
            Settings.HIDE_PLAYER_RELATED_VIDEOS,
            Settings.HIDE_PLAYER_RELATED_VIDEOS_OVERLAY,
            Settings.HIDE_END_SCREEN_CARDS,
            Settings.HIDE_END_SCREEN_SUGGESTED_VIDEO,
            Settings.HIDE_AUTOPLAY_PREVIEW,
            Settings.HIDE_AUTOPLAY_BUTTON,
            Settings.HIDE_MIX_PLAYLISTS,
            // Suggestion shelves in search and on pages.
            Settings.HIDE_HORIZONTAL_SHELVES,
            Settings.HIDE_YOU_MAY_LIKE_SECTION,
            // Shorts everywhere.
            Settings.HIDE_SHORTS_HOME,
            Settings.HIDE_SHORTS_SEARCH,
            Settings.HIDE_SHORTS_SUBSCRIPTIONS,
            Settings.HIDE_SHORTS_HISTORY,
            Settings.HIDE_SHORTS_CHANNEL,
    };

    /** Called once from {@link Settings}'s static initializer, after every setting exists. */
    public static void lockSettings() {
        try {
            for (BooleanSetting setting : LOCKED_ON) setting.lockTo(true);
            Settings.HIDE_NAVIGATION_BAR.lockTo(false);
            Settings.SHORTS_AUTOPLAY.lockTo(false);
            Settings.CHANGE_START_PAGE.lockTo(StartPage.SEARCH);
            Settings.SHORTS_PLAYER_TYPE.lockTo(ShortsPlayerType.REGULAR_PLAYER);
        } catch (Throwable failure) {
            Logger.printException(() -> "CleancutTube could not lock its settings", failure);
        }
    }

    /**
     * The browse id YouTube loads for Home, wherever it asks for it: the You page instead, so
     * neither a tap nor back nor a restart lands on recommendations.
     */
    public static String homeBrowseId(String original) {
        return HOME_REPLACEMENT_BROWSE_ID;
    }

    /** YouTube's "autoplay is on" check, answered off. */
    public static boolean autoplayEnabled(boolean original) {
        return false;
    }
}
