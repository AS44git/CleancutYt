/*
 * Copyright 2026 CleancutYt contributors
 * https://github.com/AS44git/CleancutYt
 *
 * Part of CleancutTube, a fork of Morphe's patches (GPL-3.0). Not affiliated with YouTube or Google.
 */
package app.morphe.extension.youtube.cleancut;

import android.view.View;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.patches.CustomBrandingPatch.BrandingTheme;
import app.morphe.extension.shared.patches.SplashAnimationPatch.SplashScreenAnimationStyle;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.youtube.patches.ChangeStartPagePatch.StartPage;
import app.morphe.extension.youtube.patches.WideSearchBarPatch.SearchbarType;
import app.morphe.extension.youtube.patches.OpenShortsInRegularPlayerPatch.ShortsPlayerType;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.PlayerType;

/**
 * CleancutTube: YouTube as search and your You page, with no feed to fall into.
 *
 * <ul>
 *   <li>The bottom bar holds Home and You. Shorts, Subscriptions, Create and Notifications
 *       are gone.</li>
 *   <li>Home is the search page: the toolbar's wide search bar and nothing under it. Every
 *       element of the recommendations feed is dropped as it's built
 *       ({@code CleancutHomeFilter}, {@link #isHomeShown}).</li>
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
    private Cleancut() {
    }

    private static final BooleanSetting[] LOCKED_ON = {
            // Bottom bar: Home (the search page) and You only.
            Settings.HIDE_SHORTS_BUTTON,
            Settings.HIDE_SUBSCRIPTIONS_BUTTON,
            Settings.HIDE_CREATE_BUTTON,
            Settings.HIDE_NOTIFICATIONS_BUTTON,
            Settings.HIDE_FILTER_BAR_IN_FEED,
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
            Settings.HIDE_HOME_BUTTON.lockTo(false);
            Settings.SHOW_SEARCH_BUTTON.lockTo(false);
            Settings.SHORTS_AUTOPLAY.lockTo(false);
            // Opens on Home, which is the search page: the toolbar's wide search bar over nothing.
            Settings.CHANGE_START_PAGE.lockTo(StartPage.DEFAULT);
            Settings.SEARCHBAR_TYPE.lockTo(SearchbarType.WIDE);
            // YouTube's own look: its icon, and no startup animation (Morphe's is the default).
            SharedYouTubeSettings.SPLASH_SCREEN_STYLE.lockTo(SplashScreenAnimationStyle.DISABLED);
            SharedYouTubeSettings.CUSTOM_BRANDING_ICON.lockTo(BrandingTheme.ORIGINAL);
            Settings.SHORTS_PLAYER_TYPE.lockTo(ShortsPlayerType.REGULAR_PLAYER);
        } catch (Throwable failure) {
            Logger.printException(() -> "CleancutTube could not lock its settings", failure);
        }
    }

    /** YouTube's browse id for Home, its recommendations feed. */
    static final String HOME_BROWSE_ID = "FEwhat_to_watch";

    /** The browse id of the page shown, or empty for search results. */
    private static volatile String shownBrowseId = "";

    /** Injection point: a browse page is shown. */
    public static void setBrowseId(String browseId) {
        String id = browseId == null ? "" : browseId;
        if (HOME_BROWSE_ID.equals(id) && !HOME_BROWSE_ID.equals(shownBrowseId)) {
            // Arriving on Home: its next page request may go out once.
            homePageRequests = 0;
        }
        shownBrowseId = id;
    }

    /** Where a blocked request goes: nowhere, so it fails at once without leaving the phone. */
    static final String BLOCKED_URL = "https://127.0.0.1:9/cleancutyt-blocked";

    /** Page requests made while Home is shown, since arriving on it. */
    private static volatile int homePageRequests;

    /**
     * Injection point: every request's URL, as it's built. Home shows nothing, so its feed
     * sees the end of an empty list and asks for the next page, again and again. Once Home is
     * shown, its first page request goes out (the page loads) and later ones are refused.
     * Search, channels, the watch page and the You page are never touched.
     */
    public static String requestUrl(String url) {
        try {
            if (url == null || !isHomeShown() || !url.contains("/youtubei/v1/browse")) return url;
            if (NavigationBar.isSearchBarActive() || PlayerType.getCurrent().isMaximizedOrFullscreen()) return url;
            if (++homePageRequests <= 1) return url;
            Logger.printDebug(() -> "CleancutYt: refused Home's next page");
            return BLOCKED_URL;
        } catch (Throwable failure) {
            Logger.printException(() -> "CleancutYt request check failed", failure);
            return url;
        }
    }

    /** Injection point: search results are shown (not a browse page). */
    public static void clearBrowseId() {
        shownBrowseId = "";
    }

    /** Whether the page shown is Home, which CleancutYt empties into the search page. */
    public static boolean isHomeShown() {
        return HOME_BROWSE_ID.equals(shownBrowseId);
    }

    /** Whether Home (the search page) is in front: not under search results or a video. */
    public static boolean refreshBlocked() {
        try {
            return isHomeShown() && !NavigationBar.isSearchBarActive()
                    && !PlayerType.getCurrent().isMaximizedOrFullscreen();
        } catch (Throwable failure) {
            return false;
        }
    }

    /**
     * Injection point: YouTube built a "loading more" row. On Home (empty, so the row would sit
     * under the search bar spinning) it's hidden each time it's shown; anywhere else it shows.
     */
    public static void onLoadingRow(View row) {
        if (row == null) return;
        try {
            row.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    updateLoadingRow(view);
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                }
            });
            row.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> updateLoadingRow(view));
        } catch (Throwable failure) {
            Logger.printException(() -> "CleancutYt could not watch a loading row", failure);
        }
    }

    private static void updateLoadingRow(View row) {
        int visibility = refreshBlocked() ? View.GONE : View.VISIBLE;
        if (row.getVisibility() != visibility) row.setVisibility(visibility);
    }

    /** Injection point: the pull-to-refresh circle starting or stopping. On Home it stays stopped. */
    public static boolean refreshing(boolean refreshing) {
        return refreshing && !refreshBlocked();
    }

    /** YouTube's "autoplay is on" check, answered off. */
    public static boolean autoplayEnabled(boolean original) {
        return false;
    }
}
