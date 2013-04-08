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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RotatingFileHandlerTests extends AbstractHandlerTest {
    private final static String FILENAME = "rotating-file-handler.log";

    private final File logFile = new File(BASE_LOG_DIR, FILENAME);

    @Test
    public void testCompressedRotate() throws Exception {
        RotatingFileHandler handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        handler.setCompressOnRotate(true);
        handler.setRotateSize(1024L);
        handler.setMaxBackupIndex(2);
        handler.setFile(logFile);

        // Allow a few rotates
        for (int i = 0; i < 100; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        // We should end up with 3 files, 2 zips and an uncompressed log
        final File compressedFile1 = new File(BASE_LOG_DIR, FILENAME + ".1.zip");
        final File compressedFile2 = new File(BASE_LOG_DIR, FILENAME + ".2.zip");
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(compressedFile1.exists());
        Assert.assertTrue(compressedFile2.exists());

        // Clean up files
        compressedFile1.delete();
        compressedFile2.delete();

        // Test compression with periodic rotating
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        final String extension = "." + fmt.format(cal.getTimeInMillis()) + ".zip";
        handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Set the rotate pattern
        handler.setSuffix("." + fmt.toPattern());
        handler.setCompressOnRotate(true);
        handler.setFile(logFile);

        // Write a record
        handler.publish(createLogRecord("Test message 1"));

        // Increase the day
        cal.add(Calendar.DAY_OF_MONTH, 1);

        // Write a record
        ExtLogRecord record = createLogRecord("Test message 2");
        record.setMillis(cal.getTimeInMillis());
        handler.publish(record);
        File rotatedFile = new File(BASE_LOG_DIR, FILENAME + extension);

        handler.close();

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile.exists());

        // Neither file should be empty
        Assert.assertTrue(logFile.length() > 0L);
        Assert.assertTrue(rotatedFile.length() > 0L);

        // Clean up the files
        rotatedFile.delete();

    }

    @Test
    public void testSizeRotate() throws Exception {
        final RotatingFileHandler handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        handler.setRotateSize(1024L);
        handler.setMaxBackupIndex(2);
        handler.setFile(logFile);

        // Allow a few rotates
        for (int i = 0; i < 100; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        handler.close();

        // We should end up with 3 files, 2 rotated and the default log
        final File file1 = new File(BASE_LOG_DIR, FILENAME + ".1");
        final File file2 = new File(BASE_LOG_DIR, FILENAME + ".2");
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(file1.exists());
        Assert.assertTrue(file2.exists());

        // Clean up files
        file1.delete();
        file2.delete();
    }

    @Test
    public void testBootRotate() throws Exception {
        RotatingFileHandler handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Enough to not rotate
        handler.setRotateSize(5000L);
        handler.setMaxBackupIndex(1);
        handler.setFile(logFile);
        final File rotatedFile = new File(BASE_LOG_DIR, FILENAME + ".1");

        // Log a few records
        for (int i = 0; i < 10; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        // Close the handler and create a new one
        handler.close();
        handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);

        // Log a few records
        for (int i = 0; i < 10; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        // First file should have been cleared and not rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertFalse(rotatedFile.exists());

        // Close the handler and create a new one
        handler.close();
        handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        handler.setRotateSize(5000L);
        handler.setMaxBackupIndex(1);
        handler.setRotateOnBoot(true);
        handler.setFile(logFile);

        // Log a few records
        for (int i = 0; i < 10; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        handler.close();

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile.exists());

        // Neither file should be empty
        Assert.assertTrue(logFile.length() > 0L);
        Assert.assertTrue(rotatedFile.length() > 0L);

        // Clean up files
        rotatedFile.delete();
    }

    @Test
    public void testPeriodicRotate() throws Exception {
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        final String extension = "." + fmt.format(cal.getTimeInMillis());

        RotatingFileHandler handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Set the rotate pattern
        handler.setSuffix("." + fmt.toPattern());
        handler.setFile(logFile);

        // Write a record
        handler.publish(createLogRecord("Test message 1"));

        // Test rotating the file on boot
        handler.close();
        handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Set the rotate pattern
        handler.setSuffix("." + fmt.toPattern());
        handler.setRotateOnBoot(true);
        handler.setFile(logFile);

        // Write a record
        ExtLogRecord record = createLogRecord("Test message 2");
        handler.publish(record);

        File rotatedFile = new File(BASE_LOG_DIR, FILENAME + extension);

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile.exists());

        // Neither file should be empty
        Assert.assertTrue(logFile.length() > 0L);
        Assert.assertTrue(rotatedFile.length() > 0L);

        // Delete the rotated file
        rotatedFile.delete();

        // Increase the calender to force a rotation
        cal.add(Calendar.DAY_OF_MONTH, 1);

        // Write a new record which should result in a rotation
        record = createLogRecord("Test message 3");
        record.setMillis(cal.getTimeInMillis());
        handler.publish(record);

        handler.close();

        rotatedFile = new File(BASE_LOG_DIR, FILENAME + extension);

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile.exists());

        // Neither file should be empty
        Assert.assertTrue(logFile.length() > 0L);
        Assert.assertTrue(rotatedFile.length() > 0L);

        // Clean up files
        rotatedFile.delete();
    }

    @Test
    public void testPeriodicAndSizeRotate() throws Exception {
        final int logCount = 100;
        final long rotateSize = 1024L;
        final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        String extension = "." + fmt.format(cal.getTimeInMillis());

        RotatingFileHandler handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Enough to not rotate
        handler.setRotateSize(rotateSize);
        handler.setMaxBackupIndex(2);
        handler.setRotateOnBoot(true);
        handler.setSuffix("." + fmt.toPattern());
        handler.setFile(logFile);

        // Write a record
        for (int i = 0; i < logCount; i++) {
            handler.publish(createLogRecord("Test message: %d", i));
        }

        // Test rotating the file on boot
        handler.close();
        handler = new RotatingFileHandler();
        configureHandlerDefaults(handler);
        // Enough to not rotate
        handler.setRotateSize(rotateSize);
        handler.setMaxBackupIndex(2);
        handler.setRotateOnBoot(true);
        handler.setSuffix("." + fmt.toPattern());
        // Capture the logFile size before the change
        final long currentSize = logFile.length();
        handler.setFile(logFile);

        File rotatedFile1 = new File(BASE_LOG_DIR, FILENAME + extension + ".1");
        File rotatedFile2 = new File(BASE_LOG_DIR, FILENAME + extension + ".2");

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile1.exists());
        Assert.assertTrue(rotatedFile2.exists());

        // The default file should be empty, the rotated file should not be
        Assert.assertEquals(0L, logFile.length());
        Assert.assertEquals(currentSize, rotatedFile1.length());
        Assert.assertTrue(rotatedFile2.length() > 0L);

        // Increase the calender to force a rotation
        cal.add(Calendar.DAY_OF_MONTH, 1);

        // Write a new record which should result in a rotation
        for (int i = 0; i < logCount; i++) {
            ExtLogRecord record = createLogRecord("Test message: %d", i);
            record.setMillis(cal.getTimeInMillis());
            handler.publish(record);
        }

        handler.close();

        extension = "." + fmt.format(cal.getTimeInMillis());
        rotatedFile1 = new File(BASE_LOG_DIR, FILENAME + extension + ".1");
        rotatedFile2 = new File(BASE_LOG_DIR, FILENAME + extension + ".2");

        // File should have been rotated
        Assert.assertTrue(logFile.exists());
        Assert.assertTrue(rotatedFile1.exists());
        Assert.assertTrue(rotatedFile2.exists());

        // Neither file should be empty
        Assert.assertTrue(logFile.length() > 0L);
        Assert.assertTrue(rotatedFile1.length() > 0L);
        Assert.assertTrue(rotatedFile2.length() > 0L);

        // Clean up files
        rotatedFile1.delete();
        rotatedFile2.delete();
    }
}
