/*
 * Copyright 2026 CleancutYt contributors
 * https://github.com/AS44git/CleancutYt
 *
 * Part of CleancutYt, a fork of Morphe's patches (GPL-3.0). Not affiliated with YouTube or Google.
 */
package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.BufferAsciiStrings;
import app.morphe.extension.shared.patches.components.ContextInterface;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.cleancut.Cleancut;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.PlayerType;

/**
 * CleancutYt's search page: the Home tab shows YouTube's toolbar and its search bar, and
 * nothing under them. Every element YouTube builds while the shown page is Home is dropped.
 *
 * <p>Pages opened from Home (search results, a channel, the watch page) aren't Home, and keep
 * their content.
 */
@SuppressWarnings("unused")
public final class CleancutHomeFilter extends Filter {

    public CleancutHomeFilter() {
        // Every element path has one of these; the decision is made by the page, below.
        StringFilterGroup everything = new StringFilterGroup(null, ".", "|");
        addPathCallbacks(everything);
    }

    @Override
    public boolean isFiltered(ContextInterface contextInterface, String identifier, String accessibility,
                              CharSequence path, byte[] buffer, BufferAsciiStrings asciiStrings,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!Cleancut.isHomeShown()) return false;
        // A video over Home (full screen or the watch page) keeps its comments and description.
        if (PlayerType.getCurrent().isMaximizedOrFullscreen()) return false;
        // Search results can open over Home.
        return !NavigationBar.isSearchBarActive();
    }
}
