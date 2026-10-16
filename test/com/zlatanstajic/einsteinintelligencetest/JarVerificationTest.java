package com.zlatanstajic.einsteinintelligencetest;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Checks the artifact independently of the development resource classpath. */
public final class JarVerificationTest {
    public static void main(String[] args) throws Exception {
        Path artifact = Path.of(args[0]);
        String packagePath = "com/zlatanstajic/einsteinintelligencetest/";
        // The platform loader supplies java.sql while build/, src/ and lib/ stay invisible.
        try (JarFile jar = new JarFile(artifact.toFile());
                URLClassLoader loader = new URLClassLoader(new URL[] {artifact.toUri().toURL()},
                        ClassLoader.getPlatformClassLoader())) {
            GameSessionTest.require((packagePath.replace('/', '.') + "MainWindow").equals(
                    jar.getManifest().getMainAttributes().getValue("Main-Class")), "JAR entry point");
            GameSessionTest.require(jar.getManifest().getMainAttributes().getValue("Class-Path") == null,
                    "no Class-Path manifest attribute");
            for (String name : new String[] {"MainWindow", "AboutDialog", "Language", "AuthorWebsite", "GameSession",
                    "ModernTheme", "WinnerStore", "RankListDialog"}) {
                Class.forName(packagePath.replace('/', '.') + name, false, loader);
            }
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                GameSessionTest.require(!name.endsWith(".java") && !name.endsWith(".form") && !name.startsWith("test/")
                        && !(name.startsWith(packagePath) && name.contains("Test")), "production-only entry: " + name);
                GameSessionTest.require(!name.equals("module-info.class"), "no root module-info.class");
                GameSessionTest.require(!name.startsWith("META-INF/versions/") && !name.startsWith("META-INF/native-image/"),
                        "no multi-release or native-image entry: " + name);
                GameSessionTest.require(!name.toUpperCase(Locale.ROOT).matches("META-INF/.*\\.(SF|RSA|DSA|EC)"),
                        "no signature file: " + name);
                if (name.endsWith(".class")) {
                    try (DataInputStream input = new DataInputStream(jar.getInputStream(entry))) {
                        input.readInt();
                        input.readUnsignedShort();
                        GameSessionTest.require(input.readUnsignedShort() <= 55, "Java 11 class file: " + name);
                    }
                }
            }
            for (String name : new String[] {"org/sqlite/JDBC.class", "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so"}) {
                GameSessionTest.require(jar.getEntry(name) != null, "driver entry: " + name);
            }
            JarEntry service = jar.getJarEntry("META-INF/services/java.sql.Driver");
            GameSessionTest.require(service != null, "JDBC driver service entry");
            try (InputStream input = jar.getInputStream(service)) {
                GameSessionTest.require(new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .anyMatch(line -> line.strip().equals("org.sqlite.JDBC")), "service names org.sqlite.JDBC");
            }
            requireSqliteConnection(loader);
            Pattern reference = Pattern.compile("getResource\\(\"([^\"]+)\"\\)");
            int references = 0;
            try (java.util.stream.Stream<Path> sources = Files.list(Path.of("src", packagePath))) {
                for (Path source : (Iterable<Path>) sources.filter(path -> path.toString().endsWith(".java"))::iterator) {
                    Matcher matcher = reference.matcher(Files.readString(source));
                    while (matcher.find()) {
                        String name = matcher.group(1);
                        String resource = name.startsWith("/") ? name.substring(1) : packagePath + name;
                        URL url = loader.getResource(resource);
                        GameSessionTest.require(url != null && url.getProtocol().equals("jar"), "packaged resource: " + resource);
                        GameSessionTest.require(ImageIO.read(url) != null, "readable packaged image: " + resource);
                        references++;
                    }
                }
            }
            GameSessionTest.require(references > 0, "production resource references discovered");
            System.out.println("JarVerificationTest passed (" + references + " image references)");
        }
    }

    /** Opens a real SQLite database with the packaged driver only. */
    private static void requireSqliteConnection(ClassLoader loader) throws Exception {
        Driver driver = (Driver) Class.forName("org.sqlite.JDBC", true, loader).getDeclaredConstructor().newInstance();
        Path directory = Files.createTempDirectory("jar-sqlite-test");
        try (Connection connection = driver.connect("jdbc:sqlite:" + directory.resolve("check.db"), new Properties());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select sqlite_version()")) {
            GameSessionTest.require(result.next() && !result.getString(1).isEmpty(), "packaged SQLite driver opens a database");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(path);
                }
            }
        }
    }
}
