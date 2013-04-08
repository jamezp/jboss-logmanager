/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.logmanager.handlers;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FileCleanerTests extends AbstractHandlerTest {
    private final int defaultFileCount = 5;
    private final String filename = "purge";

    @BeforeClass
    public static void clearBaseDirectory() {
        deleteChildrenRecursively(BASE_LOG_DIR);
    }

    @Before
    public void createDefaultFiles() throws Exception {
        // Create some generic files that should not be deleted
        for (int i = 0; i < defaultFileCount; i++) {
            createFile("purge-%d", i);
        }
    }

    @Test
    public void purgeWithDateSuffix() throws Exception {

        // Create a base file
        final File baseFile = createFile(filename);
        final SimpleDateFormat fmt = new SimpleDateFormat(".yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();

        // Create files appending a the date suffix
        for (int i = 0; i < 10; i++) {
            final File file = createFile(baseFile.getName() + fmt.format(cal.getTime()));
            Assert.assertTrue("Could not set the last modified time", file.setLastModified(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Clean up the files
        final FileCleaner cleaner = new FileCleaner(baseFile, 1, fmt.toPattern(), cal.getTimeZone());
        cleaner.run();
        // Check the total number of files
        Assert.assertEquals("Files not deleted", defaultFileCount + 2, BASE_LOG_DIR.list().length);
    }

    @Test
    public void purgeWithIndexSuffix() throws Exception {

        // Create a base file
        final File baseFile = createFile(filename);
        final Calendar cal = Calendar.getInstance();

        // Create files with a index suffix only
        for (int i = 0; i < 10; i++) {
            final File file = createFile(baseFile.getName() + "." + i);
            Assert.assertTrue("Could not set the last modified time", file.setLastModified(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Clean up the files
        final FileCleaner cleaner = new FileCleaner(baseFile, 1, null, cal.getTimeZone());
        cleaner.run();
        // Check the total number of files
        Assert.assertEquals("Files not deleted", defaultFileCount + 2, BASE_LOG_DIR.list().length);
    }

    @Test
    public void purgeWithDateAndIndexSuffix() throws Exception {

        // Create a base file
        final File baseFile = createFile(filename);
        final SimpleDateFormat fmt = new SimpleDateFormat(".yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();

        // Create files with a index suffix only
        for (int i = 0; i < 10; i++) {
            final File file = createFile(baseFile.getName() + fmt.format(cal.getTime()) + "." + i);
            Assert.assertTrue("Could not set the last modified time", file.setLastModified(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Clean up the files
        final FileCleaner cleaner = new FileCleaner(baseFile, 1, fmt.toPattern(), cal.getTimeZone());
        cleaner.run();
        // Check the total number of files
        Assert.assertEquals("Files not deleted", defaultFileCount + 2, BASE_LOG_DIR.list().length);
    }

    static File createFile(final String filename) throws IOException {
        final File result = new File(BASE_LOG_DIR, filename);
        Assert.assertTrue("Could not create file: " + filename, result.createNewFile());
        return result;
    }

    static File createFile(final String format, final Object... args) throws IOException {
        return createFile(String.format(format, args));
    }
}
