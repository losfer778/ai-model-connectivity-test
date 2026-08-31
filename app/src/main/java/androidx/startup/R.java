package androidx.startup;

/**
 * Compatibility shim for AIDE's resource packager.
 * The packaged androidx.startup runtime references R.string.androidx_startup
 * but AIDE occasionally omits this generated class from dex.
 */
public final class R {
    private R() { }

    public static final class string {
        public static final int androidx_startup = 0x7f120000;
        private string() { }
    }
}
