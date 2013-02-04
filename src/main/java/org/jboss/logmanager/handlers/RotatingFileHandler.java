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
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.ErrorManager;

import org.jboss.logmanager.ExtLogRecord;

/**
 * A file handler that rotates based on it's configuration.
 * <p/>
 * Setting the {@link #setSuffix(String) suffix} rotates the file based on the date/time parsed from the suffix. The
 * suffix is then appended to the file and a new file is created. A {@code null} suffix indicates the periodic rotation
 * is disabled. This is the default value.
 * <p/>
 * Setting the {@link #setRotateSize(long) rotate size} rotates the file once the size of the file, in bytes, reaches
 * the rotate size. When the rotation occurs the file is renamed and a number is appended to indicate the backup index.
 * If the {@link #setMaxBackupIndex(int) maximum index} has been reached the previous maximum is deleted before the
 * file is renamed. A size of 0 or less indicates the size rotation is disabled. This is the default value.
 * <p/>
 * Setting the {@link #setRotateOnBoot(boolean)} to {@code true} rotates the file regardless of the other two options
 * when the first log record is written to the handler.
 * <p/>
 * The order the rotation check is as follows:
 * <ol>
 * <li>{@link #setRotateOnBoot(boolean) rotate on boot}</li>
 * <li>{@link #setSuffix(String) suffix}</li>
 * <li>{@link #setRotateSize(long) size}</li>
 * </ol>
 * <p/>
 * If both the {@link #setSuffix(String) suffix} and {@link #setRotateSize(long) size} are set the rotation index may
 * or may not be appended to the file name. The rotation index will be appended if the rotate size was reached before
 * the suffix rotation. If the suffix rotation happens before the size is reached, the index will not be appended.
 * <p/>
 * You can optionally set the {@link #setDaysToKeep(int) number of days} to keep files. Files that have a {@link
 * java.io.File#lastModified() last modified} before the current date minus the number of days to keep will be deleted
 * if the base file name matches. The base file name is the file name minus the suffix or rotation index. A value of 0
 * or less indicates that no files should be deleted based on the files {@link java.io.File#lastModified() last
 * modified} date.
 */
public class RotatingFileHandler extends FileHandler {

    private SimpleDateFormat format;
    private String nextSuffix;
    private Period period = Period.NEVER;
    private long nextRotationTime = Long.MAX_VALUE;
    private TimeZone timeZone = TimeZone.getDefault();

    private int daysToKeep = 0;
    private long rotateSize = 0;
    private int maxBackupIndex = 1;

    private CountingOutputStream outputStream;
    private boolean rotateOnBoot = false;
    private boolean firstWrite = true;

    /**
     * Construct a new instance with no formatter and no output file.
     */
    public RotatingFileHandler() {
    }

    /**
     * Construct a new instance with the given output file.
     *
     * @param fileName the file name
     *
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public RotatingFileHandler(final String fileName) throws FileNotFoundException {
        super(fileName);
    }

    /**
     * Construct a new instance with the given output file and append setting.
     *
     * @param fileName the file name
     * @param append   {@code true} to append, {@code false} to overwrite
     *
     * @throws java.io.FileNotFoundException if the file could not be found on open
     */
    public RotatingFileHandler(final String fileName, final boolean append) throws FileNotFoundException {
        super(fileName, append);
    }

    @Override
    public void setOutputStream(final OutputStream outputStream) {
        synchronized (outputLock) {
            this.outputStream = outputStream == null ? null : new CountingOutputStream(outputStream);
            super.setOutputStream(this.outputStream);
        }
    }

    @Override
    public void setFile(final File file) throws FileNotFoundException {
        synchronized (outputLock) {
            // Set the file
            super.setFile(file);
            if (outputStream != null)
                outputStream.currentSize = file == null ? 0L : file.length();
            // Calculate the next date rollover
            if (nextSuffix != null && format != null && file != null && file.lastModified() > 0) {
                calcNextRollover(file.lastModified());
            }
        }
    }

    /**
     * Returns the number of days to keep files with the same base name for. The base name is the default name of the
     * file minus any date suffixes or rotation indexes.
     *
     * @return the number of days to keep files
     */
    public int getDaysToKeep() {
        synchronized (outputLock) {
            return daysToKeep;
        }
    }

    /**
     * Sets the number of days to keep files.
     * <p/>
     * Files with a {@link java.io.File#lastModified() last modified} date less than the current date minus the number
     * of days to keep will be deleted if the base file name matches. A value of 0 or less indicates all files are
     * kept.
     * <p/>
     * The base name is the default name of the file minus any date suffixes or rotation indexes.
     *
     * @param daysToKeep the number of days to keep
     */
    public void setDaysToKeep(final int daysToKeep) {
        checkAccess(this);
        synchronized (outputLock) {
            this.daysToKeep = daysToKeep;
        }
    }

    /**
     * Indicates whether or a not the handler should rotate the file before the first log record is written.
     *
     * @return {@code true} if file should rotate on boot, otherwise {@code false}/
     */
    public boolean isRotateOnBoot() {
        synchronized (outputLock) {
            return rotateOnBoot;
        }
    }

    /**
     * Set to a value of {@code true} on the first log record processed the file should be rotated if a file of the
     * same name already exists.
     *
     * @param rotateOnBoot {@code true} to rotate on boot, otherwise {@code false}
     */
    public void setRotateOnBoot(final boolean rotateOnBoot) {
        checkAccess(this);
        synchronized (outputLock) {
            this.rotateOnBoot = rotateOnBoot;
        }
    }

    /**
     * Get the rotation size in bytes.
     * <p/>
     * A value of 0 or less indicates the rotate size rotation check is disabled.
     *
     * @return the number of bytes before the log is rotated
     */
    public long getRotateSize() {
        synchronized (outputLock) {
            return rotateSize;
        }
    }

    /**
     * Set the rotation size, in bytes.
     * <p/>
     * A value of 0 or less indicates the rotate size rotation check is disabled.
     *
     * @param rotateSize the number of bytes before the log is rotated or 0 to disable
     */
    public void setRotateSize(final long rotateSize) {
        checkAccess(this);
        synchronized (outputLock) {
            this.rotateSize = rotateSize;
        }
    }

    /**
     * The maximum number of backups to keep. This is only used if the {@link #setRotateSize(long) rotation size} is
     * specified.
     *
     * @return the number of backups to keep.
     */
    public int getMaxBackupIndex() {
        synchronized (outputLock) {
            return maxBackupIndex;
        }
    }

    /**
     * Set the maximum backup index (the number of log files to keep around). This is only used if the {@link
     * #setRotateSize(long) rotation size} is specified.
     *
     * @param maxBackupIndex the maximum backup index
     */
    public void setMaxBackupIndex(final int maxBackupIndex) {
        checkAccess(this);
        synchronized (outputLock) {
            this.maxBackupIndex = maxBackupIndex;
        }
    }

    /**
     * Get the configured time zone for this handler.
     *
     * @return the configured time zone
     */
    public TimeZone getTimeZone() {
        synchronized (outputLock) {
            return timeZone;
        }
    }

    /**
     * Set the configured time zone for this handler.
     *
     * @param timeZone the configured time zone
     */
    public void setTimeZone(final TimeZone timeZone) {
        checkAccess(this);
        if (timeZone == null) {
            throw new IllegalArgumentException("timeZone is null");
        }
        synchronized (outputLock) {
            this.timeZone = timeZone;
        }
    }

    /**
     * Get the suffix pattern used to rotate based on the date.
     *
     * @return the suffix pattern
     */
    public String getSuffix() {
        synchronized (outputLock) {
            return format.toPattern();
        }
    }

    /**
     * Set the suffix string.  The string is in a format which can be understood by {@link java.text.SimpleDateFormat}.
     * The period of the rotation is automatically calculated based on the suffix.
     * <p/>
     * If the suffix is {@code null}, rotation based on date is disabled.
     *
     * @param suffix the suffix
     *
     * @throws IllegalArgumentException if the suffix is not valid
     */
    public void setSuffix(String suffix) throws IllegalArgumentException {
        if (suffix == null) {
            nextRotationTime = Long.MAX_VALUE;
            nextSuffix = null;
        } else {
            final SimpleDateFormat format = new SimpleDateFormat(suffix);
            format.setTimeZone(timeZone);
            final int len = suffix.length();
            Period period = Period.NEVER;
            for (int i = 0; i < len; i++) {
                switch (suffix.charAt(i)) {
                    case 'y':
                        period = min(period, Period.YEAR);
                        break;
                    case 'M':
                        period = min(period, Period.MONTH);
                        break;
                    case 'w':
                    case 'W':
                        period = min(period, Period.WEEK);
                        break;
                    case 'D':
                    case 'd':
                    case 'F':
                    case 'E':
                        period = min(period, Period.DAY);
                        break;
                    case 'a':
                        period = min(period, Period.HALF_DAY);
                        break;
                    case 'H':
                    case 'k':
                    case 'K':
                    case 'h':
                        period = min(period, Period.HOUR);
                        break;
                    case 'm':
                        period = min(period, Period.MINUTE);
                        break;
                    case '\'':
                        while (suffix.charAt(++i) != '\'') ;
                        break;
                    case 's':
                    case 'S':
                        throw new IllegalArgumentException("Rotating by second or millisecond is not supported");
                }
            }
            synchronized (outputLock) {
                this.format = format;
                this.period = period;
                final long now;
                final File file = getFile();
                if (file != null && file.lastModified() > 0) {
                    now = file.lastModified();
                } else {
                    now = System.currentTimeMillis();
                }
                calcNextRollover(now);
            }
        }
    }

    @Override
    protected void preWrite(final ExtLogRecord record) {
        super.preWrite(record);
        boolean doRollOver = false;
        final File file = getFile();
        // If rotate on boot, rotate if the file already exists
        if (firstWrite && rotateOnBoot && file != null && file.exists() && file.length() > 0L) {
            doRollOver = true;
        }
        if (nextSuffix != null) {
            final long recordMillis = record.getMillis();
            if (recordMillis >= nextRotationTime) {
                doRollOver = true;
                calcNextRollover(recordMillis);
            }
        } else if (rotateSize > 0) {
            final int maxBackupIndex = this.maxBackupIndex;
            final long currentSize = (outputStream == null ? Long.MIN_VALUE : outputStream.currentSize);
            if (currentSize > rotateSize && maxBackupIndex > 0) {
                doRollOver = true;
            }
        }
        if (doRollOver) {
            rotate();
        }
        firstWrite = false;
    }

    private void rotate() {
        try {
            final File file = getFile();
            if (file == null) {
                // no file is set; a direct output stream or writer was specified
                return;
            }
            // first, close the original file (some OSes won't let you move/rename a file that is open)
            setFile(null);
            final boolean rename;
            final String filename;
            if (nextSuffix != null) {
                filename = file.getAbsolutePath() + nextSuffix;
                rename = true;
            } else {
                filename = file.getAbsolutePath();
                rename = false;
            }

            // If we're rotating based on size we need to check the rotation index
            if (rotateSize > 0L) {
                final int maxBackupIndex = this.maxBackupIndex;
                final long currentSize = (outputStream == null ? Long.MIN_VALUE : outputStream.currentSize);
                if (currentSize > rotateSize && maxBackupIndex > 0) {
                    // rotate.  First, drop the max file (if any), then move each file to the next higher slot.
                    new File(filename + "." + maxBackupIndex).delete();
                    for (int i = maxBackupIndex - 1; i >= 1; i--) {
                        new File(filename + "." + i).renameTo(new File(filename + "." + (i + 1)));
                    }
                    file.renameTo(new File(filename + ".1"));
                }
            }
            // Only rotate on the date if the nextSuffix is set
            if (rename) {
                file.renameTo(new File(filename));
            }
            if (daysToKeep > 0) {
                FileCleaner.execute(file, daysToKeep, timeZone);
            }
            // start new file
            setFile(file);
        } catch (FileNotFoundException e) {
            reportError("Unable to rotate log file", e, ErrorManager.OPEN_FAILURE);
        }
    }

    private void calcNextRollover(final long fromTime) {
        if (period == Period.NEVER) {
            nextRotationTime = Long.MAX_VALUE;
            nextSuffix = null;
            return;
        }
        nextSuffix = format.format(new Date(fromTime));
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(fromTime);
        final Period period = this.period;
        // clear out less-significant fields
        switch (period) {
            default:
            case YEAR:
                calendar.set(Calendar.MONTH, 0);
            case MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 0);
                calendar.clear(Calendar.WEEK_OF_MONTH);
            case WEEK:
                if (period == Period.WEEK) {
                    calendar.set(Calendar.DAY_OF_WEEK, 0);
                } else {
                    calendar.clear(Calendar.DAY_OF_WEEK);
                }
                calendar.clear(Calendar.DAY_OF_WEEK_IN_MONTH);
            case DAY:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            case HALF_DAY:
                calendar.set(Calendar.HOUR, 0);
            case HOUR:
                calendar.set(Calendar.MINUTE, 0);
            case MINUTE:
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
        }
        // increment the relevant field
        switch (period) {
            case YEAR:
                calendar.add(Calendar.YEAR, 1);
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, 1);
                break;
            case WEEK:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case DAY:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case HALF_DAY:
                calendar.add(Calendar.AM_PM, 1);
                break;
            case HOUR:
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case MINUTE:
                calendar.add(Calendar.MINUTE, 1);
                break;
        }
        nextRotationTime = calendar.getTimeInMillis();
    }

    private static <T extends Comparable<? super T>> T min(T a, T b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * Possible period values.  Keep in strictly ascending order of magnitude.
     */
    public enum Period {
        MINUTE,
        HOUR,
        HALF_DAY,
        DAY,
        WEEK,
        MONTH,
        YEAR,
        NEVER,
    }
}
