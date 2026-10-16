package com.zlatanstajic.einsteinintelligencetest;

import java.util.Arrays;

/** Checks OS dispatch without starting a browser or email client during tests. */
public final class AuthorWebsiteTest {
    public static void main(String[] args) {
        for (String url : new String[] {AuthorWebsite.URL, AuthorWebsite.EMAIL_URL}) {
            check("Windows 11", url, "rundll32.exe", "url.dll,FileProtocolHandler", url);
            check("Windows 10", url, "rundll32.exe", "url.dll,FileProtocolHandler", url);
            check("Linux", url, "xdg-open", url);
            check("Mac OS X", url, "open", url);
        }
        try {
            AuthorWebsite.commandFor("Windows 11", "https://example.com/&calc.exe");
            throw new AssertionError("Unexpected contact link accepted");
        } catch (IllegalArgumentException expected) {
            // Restrict OS handlers to the application's own contact links.
        }
        System.out.println("AuthorWebsiteTest passed");
    }

    private static void check(String os, String url, String... expected) {
        GameSessionTest.require(Arrays.equals(expected, AuthorWebsite.commandFor(os, url)),
                "contact link command for " + os + ": " + url);
    }
}
