/*
 * Copyright 2026 CleancutYt contributors
 * https://github.com/AS44git/CleancutYt
 */
package app.morphe.extension.youtube.cleancut;

/** Everything that names this fork, in one place. */
public final class Brand {
    private Brand() {
    }

    public static final String APP_NAME = "CleancutYt";

    /** The repository whose releases the updater checks and downloads from. */
    public static final String GITHUB_REPO = "AS44git/CleancutYt";

    public static final String SOURCE_URL = "https://github.com/" + GITHUB_REPO;

    /**
     * CleancutYt has one build (no clone), so no install picks a "-clone.apk" asset; kept so the
     * updater reads the same as CleancutTok's.
     */
    public static final String CLONE_PACKAGE_SUFFIX = ".cleancutyt.clone";

    /**
     * This fork's own release number on top of YouTube's version: YouTube 21.30.216 at revision
     * 1 is released under the tag v21-30-216-1. Bump it for every release. The release workflow
     * reads it from here.
     */
    public static final int REVISION = 1;
}
