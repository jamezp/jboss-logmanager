/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager;

import java.lang.management.ManagementFactory;
import java.lang.management.PlatformLoggingMXBean;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PlatformLoggingMXBeanTests {
    static {
        // TODO (jrp) this should be removed once the tests are complete
        //System.clearProperty("java.util.logging.manager");
    }

    private static final String LOGGER_NAME = PlatformLoggingMXBeanTests.class.getName();
    private static final String PARENT_LOGGER_NAME = PlatformLoggingMXBeanTests.class.getPackage().getName();
    private java.util.logging.Logger logger;

    @Before
    public void setup() throws Exception {
        final LogContext logContext = LogContext.create();
        LogContext.setLogContextSelector(() -> logContext);
        // Create the loggers
        logger = java.util.logging.Logger.getLogger(LOGGER_NAME);
        java.util.logging.Logger.getLogger(PARENT_LOGGER_NAME);
    }

    @Test
    public void testCorrectClass() throws Exception {
        final PlatformLoggingMXBean bean = ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class);
        System.out.println(bean);
    }

    @Test
    public void testLoggerNames() {
        final List<String> names = getPlatformLoggingMXBean().getLoggerNames();
        Assert.assertTrue(names.contains(LOGGER_NAME));
    }

    @Test
    public void testGetLevel() {
        final PlatformLoggingMXBean bean = getPlatformLoggingMXBean();
        // A non-existent logger should return a null level
        // TODO (jrp) our LogManager.getLogger() seems to break the contract and should return null if it doesn't exist
        Assert.assertNull(bean.getLoggerLevel("non.existent.logger"));
        // No level has been set so this should be null
        Assert.assertEquals("", bean.getLoggerLevel(LOGGER_NAME));
        // Set a level on the logger, the returned value should now not be null
        logger.setLevel(Level.INFO);
        Assert.assertEquals(Level.INFO.getName(), bean.getLoggerLevel(LOGGER_NAME));
    }

    @Test
    public void testGetParentLoggerName() {
        final PlatformLoggingMXBean bean = getPlatformLoggingMXBean();
        // A non-existent logger should return a null parent
        Assert.assertNull(bean.getParentLoggerName("non.existent.logger"));
        // This should return the parent logger name
        Assert.assertEquals(PARENT_LOGGER_NAME, bean.getParentLoggerName(LOGGER_NAME));
        // The parent logger should return an empty string since no real parent was created. This is effectively the
        // root logger
        Assert.assertEquals("", bean.getParentLoggerName(PARENT_LOGGER_NAME));
    }

    @Test
    public void testSetLevel() {
        final PlatformLoggingMXBean bean = getPlatformLoggingMXBean();
        bean.setLoggerLevel(LOGGER_NAME, "INFO");
        Assert.assertEquals("INFO", bean.getLoggerLevel(LOGGER_NAME));
        Assert.assertEquals(java.util.logging.Level.INFO, logger.getLevel());
        bean.setLoggerLevel(LOGGER_NAME, null);
        // Setting the level to null should result in a MXBean value of "", but a level of null
        Assert.assertEquals("", bean.getLoggerLevel(LOGGER_NAME));
        Assert.assertNull(logger.getLevel());
    }

    private static PlatformLoggingMXBean getPlatformLoggingMXBean() {
        final List<?> l = ManagementFactory.getPlatformMXBeans(PlatformLoggingMXBean.class);
        l.forEach(System.out::println);
        return ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class);
    }
}
