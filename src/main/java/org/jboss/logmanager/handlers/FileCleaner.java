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
import java.io.FileFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class FileCleaner implements Runnable {

    private static final Object LOCK = new Object();
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\.\\d+$");

    private final File baseFile;
    private final int daysToKeep;
    private final SimpleDateFormat format;
    private final TimeZone timeZone;

    protected FileCleaner(final File baseFile, final int daysToKeep, final String suffixPattern, final TimeZone timeZone) {
        this.baseFile = baseFile;
        this.daysToKeep = daysToKeep;
        this.timeZone = timeZone;
        if (suffixPattern == null) {
            format = null;
        } else {
            format = new SimpleDateFormat(suffixPattern);
        }
    }

    public static void execute(final File baseFile, final int daysToKeep, final String suffixPattern, final TimeZone timeZone) {
        final FileCleaner cleaner = new FileCleaner(baseFile, daysToKeep, suffixPattern, timeZone);
        final Thread t = new Thread(cleaner);
        t.setName("logmanager-file-cleaner");
        t.setDaemon(true);
        t.start();
    }


    @Override
    public void run() {
        final Calendar cal = Calendar.getInstance(timeZone);
        cal.add(Calendar.DAY_OF_MONTH, daysToKeep * -1);
        synchronized (LOCK) {
            final File[] files = baseFile.getParentFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    final String basePath = baseFile.getAbsolutePath();
                    final String path = pathname.getAbsolutePath();
                    if (basePath.equals(path) || !path.startsWith(basePath)) {
                        return false;
                    }

                    // Get the possible suffix
                    final String possibleSuffix = path.substring(basePath.length());

                    // Attempt to parse the date
                    if (format != null) {
                        try {
                            format.parse(possibleSuffix);
                            return true;
                        } catch (ParseException ignore) {
                            // Attempt to remove a possible index
                            final Matcher matcher = INDEX_PATTERN.matcher(possibleSuffix);
                            final String rpl = matcher.replaceAll("");
                            try {
                                format.parse(rpl);
                                return true;
                            } catch (ParseException ignored) {
                            }
                        }
                    }

                    return INDEX_PATTERN.matcher(possibleSuffix).matches();
                }
            });
            for (File file : (files != null ? files : new File[0])) {
                if (file.isFile()) {
                    // Check the last modified date
                    if (file.lastModified() < cal.getTimeInMillis()) {
                        file.delete();
                    }
                }
            }
        }
    }
}
