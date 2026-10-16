PACKAGE := com.zlatanstajic.einsteinintelligencetest
PACKAGE_DIR := $(subst .,/,$(PACKAGE))
SOURCES := $(wildcard src/$(PACKAGE_DIR)/*.java)
TEST_SOURCES := $(wildcard test/$(PACKAGE_DIR)/*.java)
MAIN_CLASS := build/$(PACKAGE_DIR)/MainWindow.class
SQLITE_JDBC := lib/sqlite-jdbc-3.50.3.0.jar

.PHONY: build run test compile-tests test-ui jar check install-hooks clean
build: $(MAIN_CLASS)
$(MAIN_CLASS): $(SOURCES)
	javac --release 11 -encoding UTF-8 -d build $(SOURCES)
run: build
	java -cp build:src:$(SQLITE_JDBC) $(PACKAGE).MainWindow
compile-tests: build
	javac --release 11 -encoding UTF-8 -cp build -d build/test $(TEST_SOURCES)
test: compile-tests
	java -Djava.awt.headless=true -cp build:build/test $(PACKAGE).AuthorWebsiteTest
	java -Djava.awt.headless=true -cp build:build/test $(PACKAGE).GameSessionTest
	java -Djava.awt.headless=true -cp build:build/test $(PACKAGE).LanguageTest
	java -Djava.awt.headless=true -cp build:build/test:$(SQLITE_JDBC) $(PACKAGE).WinnerStoreTest
test-ui: compile-tests
	java -cp build:build/test:src:$(SQLITE_JDBC) $(PACKAGE).SwingSmokeTest
jar: build
	rm -rf build/sqlite-jdbc
	mkdir -p build/sqlite-jdbc dist
	cd build/sqlite-jdbc && jar xf ../../$(SQLITE_JDBC)
	cd build/sqlite-jdbc && rm -rf META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA META-INF/*.EC \
		META-INF/versions META-INF/native-image module-info.class
	jar cfm dist/einstein-intelligence-test.jar manifest.mf -C build $(PACKAGE_DIR) -C src $(PACKAGE_DIR)/images \
		-C build/sqlite-jdbc .
check: test jar
	java -Djava.awt.headless=true -cp build:build/test $(PACKAGE).JarVerificationTest dist/einstein-intelligence-test.jar
install-hooks:
	install -D -m 755 hooks/pre-commit "$$(git rev-parse --git-path hooks/pre-commit)"
clean:
	rm -rf build dist
