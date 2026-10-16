package com.zlatanstajic.einsteinintelligencetest;

import java.io.IOException;
import java.util.Locale;

/** Opens the fixed author contact links using the operating system's URL handler. */
public final class AuthorWebsite {

    public static final String URL = "https://zlatanstajic.com";
    public static final String EMAIL_URL = "mailto:contact@zlatanstajic.com";

    private AuthorWebsite() {
    }

    /** Opens the author's website in the default browser. */
    public static void open() throws IOException {
        openLink(URL);
    }

    /** Opens a new message in the configured email application. */
    public static void openEmail() throws IOException {
        openLink(EMAIL_URL);
    }

    private static void openLink(String url) throws IOException {
        new ProcessBuilder(commandFor(System.getProperty("os.name", ""), url)).inheritIO().start();
    }

    // Only fixed application-owned links reach the OS handler; no shell is involved.
    static String[] commandFor(String osName, String url) {
        if (!URL.equals(url) && !EMAIL_URL.equals(url)) {
            throw new IllegalArgumentException("Unsupported contact link");
        }
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            return new String[] {"rundll32.exe", "url.dll,FileProtocolHandler", url};
        }
        if (os.startsWith("mac")) {
            return new String[] {"open", url};
        }
        // Desktop.browse/mail can report unsupported when GTK fails in snap terminals.
        return new String[] {"xdg-open", url};
    }
}
