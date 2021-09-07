/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author roland
 * @since 07/10/16
 */
public class AnsiLoggerTest {
    @BeforeClass
    public static void installAnsi() {
        AnsiConsole.systemInstall();
    }

    @Before
    public void forceAnsiPassthrough() {
        // Because the AnsiConsole keeps a per-VM counter of calls to systemInstall, it is
        // difficult to force it to pass through escapes to stdout during test.
        // Additionally, running the test in a suite (e.g. with mvn test) means other
        // code may have already initialized or manipulated the AnsiConsole.
        // Hence we just reset the stdout/stderr references to those captured by AnsiConsole
        // during its static initialization and restore them after tests.
        System.setOut(AnsiConsole.system_out);
        System.setErr(AnsiConsole.system_err);
    }

    @AfterClass
    public static void restoreAnsiPassthrough() {
        AnsiConsole.systemUninstall();
        System.setOut(AnsiConsole.out);
        System.setErr(AnsiConsole.err);
    }

    @Test
    public void emphasizeDebug() {
        TestLog testLog = new TestLog() {
            @Override
            public boolean isDebugEnabled() {
                return true;
            }
        };

        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        logger.debug("Debug messages do not interpret [[*]]%s[[*]]", "emphasis");
        assertEquals("T>Debug messages do not interpret [[*]]emphasis[[*]]",
                testLog.getMessage());
    }

    @Test
    public void emphasizeInfoWithDebugEnabled() {
        TestLog testLog = new TestLog() {
            @Override
            public boolean isDebugEnabled() {
                return true;
            }
        };

        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        logger.info("Info messages do not apply [[*]]%s[[*]] when debug is enabled", "color codes");
        assertEquals("T>Info messages do not apply color codes when debug is enabled",
                testLog.getMessage());
    }

    @Test
    public void verboseEnabled() {
        String[] data = {
                "build", "Test",
                "api", null,
                "bla", "log: Unknown verbosity group bla. Ignoring...",
                "all", "Test",
                "", "Test",
                "true", "Test",
                "false", null
        };
        for (int i = 0; i < data.length; i += 2) {
            TestLog testLog = new TestLog();
            AnsiLogger logger = new AnsiLogger(testLog, false, data[i], false, "");
            logger.verbose( KitLogger.LogVerboseCategory.BUILD, "Test");
            assertEquals(data[i+1], testLog.getMessage());
        }
    }
    @Test

    public void emphasizeInfo() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = Ansi.ansi();
        logger.info("Yet another [[*]]Test[[*]] %s", "emphasis");
        assertEquals(ansi.fg(AnsiLogger.INFO)
                        .a("T>")
                        .a("Yet another ")
                        .fgBright(AnsiLogger.EMPHASIS)
                        .a("Test")
                        .fg(AnsiLogger.INFO)
                        .a(" emphasis")
                        .reset().toString(),
                testLog.getMessage());
    }

    @Test
    public void emphasizeInfoSpecificColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.info("Specific [[C]]color[[C]] %s","is possible");
        assertEquals(ansi.fg(AnsiLogger.INFO)
                        .a("T>")
                        .a("Specific ")
                        .fg(Ansi.Color.CYAN)
                        .a("color")
                        .fg(AnsiLogger.INFO)
                        .a(" is possible")
                        .reset().toString(),
                testLog.getMessage());
    }

    @Test
    public void emphasizeInfoIgnoringEmpties() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        // Note that the closing part of the emphasis does not need to match the opening.
        // E.g. [[b]]Blue[[*]] works just like [[b]]Blue[[b]]
        logger.info("[[b]][[*]]Skip[[*]][[*]]ping [[m]]empty strings[[/]] %s[[*]][[c]][[c]][[*]]","is possible");
        assertEquals(ansi.fg(AnsiLogger.INFO)
                        .a("T>")
                        .a("Skipping ")
                        .fgBright(Ansi.Color.MAGENTA)
                        .a("empty strings")
                        .fg(AnsiLogger.INFO)
                        .a(" is possible")
                        .reset().toString(),
                testLog.getMessage());
    }

    @Test
    public void emphasizeInfoSpecificBrightColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.info("Lowercase enables [[c]]bright version[[c]] of %d colors",Ansi.Color.values().length - 1);
        assertEquals(ansi.fg(AnsiLogger.INFO)
                        .a("T>")
                        .a("Lowercase enables ")
                        .fgBright(Ansi.Color.CYAN)
                        .a("bright version")
                        .fg(AnsiLogger.INFO)
                        .a(" of 8 colors")
                        .reset().toString(),
                testLog.getMessage());
    }

    @Test
    public void emphasizeInfoWithoutColor() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, false, null, false, "T>");
        logger.info("Disabling color causes logger to [[*]]interpret and remove[[*]] %s","emphasis");
        assertEquals("T>Disabling color causes logger to interpret and remove emphasis",
                testLog.getMessage());
    }

    @Test
    public void emphasizeWarning() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.warn("%s messages support [[*]]emphasis[[*]] too","Warning");
        assertEquals(ansi.fg(AnsiLogger.WARNING)
                        .a("T>")
                        .a("Warning messages support ")
                        .fgBright(AnsiLogger.EMPHASIS)
                        .a("emphasis")
                        .fg(AnsiLogger.WARNING)
                        .a(" too")
                        .reset().toString(),
                testLog.getMessage());
    }

    @Test
    public void emphasizeError() {
        TestLog testLog = new TestLog();
        AnsiLogger logger = new AnsiLogger(testLog, true, null, false, "T>");
        Ansi ansi = new Ansi();
        logger.error("Error [[*]]messages[[*]] could emphasise [[*]]%s[[*]]","many things");
        assertEquals(ansi.fg(AnsiLogger.ERROR)
                        .a("T>")
                        .a("Error ")
                        .fgBright(AnsiLogger.EMPHASIS)
                        .a("messages")
                        .fg(AnsiLogger.ERROR)
                        .a(" could emphasise ")
                        .fgBright(AnsiLogger.EMPHASIS)
                        .a("many things")
                        .reset()
                        .toString(),
                testLog.getMessage());
    }


    private class TestLog extends DefaultLog {
        private String message;

        public TestLog() {
            super(new ConsoleLogger(1, "console"));
        }

        @Override
        public void debug(CharSequence content) {
            this.message = content.toString();
            super.debug(content);
        }

        @Override
        public void info(CharSequence content) {
            this.message = content.toString();
            super.info(content);
        }

        @Override
        public void warn(CharSequence content) {
            this.message = content.toString();
            super.warn(content);
        }

        @Override
        public void error(CharSequence content) {
            this.message = content.toString();
            super.error(content);
        }

        void reset() {
            message = null;
        }

        public String getMessage() {
            return message;
        }
    }

}

