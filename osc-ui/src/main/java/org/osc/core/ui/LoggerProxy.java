/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.ui;

import static org.osc.core.ui.LogProvider.getLoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Wrapper around the properly initialized {@link Logger}, once available from {@link LogProvider}.
 * Delegates all the calls to that logger or to a standin logger in the meanwhile.
 *
 */
class LoggerProxy implements Logger {

    private Logger properLogger;
    private final Logger FALLBACK_IMPL;
    private final String className;

    public LoggerProxy(String className) {
        this.className = className;
        this.FALLBACK_IMPL = LoggerFactory.getLogger(className);
    }

    @Override
    public String getName() {
        return this.className;
    }

    @Override
    public boolean isTraceEnabled() {
        return findImplToUse().isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return findImplToUse().isTraceEnabled(marker);
    }

    @Override
    public void trace(String msg) {
        findImplToUse().trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        findImplToUse().trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        findImplToUse().trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        findImplToUse().trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        findImplToUse().trace(msg, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        findImplToUse().trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        findImplToUse().trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        findImplToUse().trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        findImplToUse().trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        findImplToUse().trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return findImplToUse().isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return findImplToUse().isDebugEnabled(marker);
    }

    @Override
    public void debug(String msg) {
        findImplToUse().debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        findImplToUse().debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        findImplToUse().debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        findImplToUse().debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        findImplToUse().debug(msg, t);
    }

    @Override
    public void debug(Marker marker, String msg) {
        findImplToUse().debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        findImplToUse().debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        findImplToUse().debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        findImplToUse().debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        findImplToUse().debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return findImplToUse().isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return findImplToUse().isInfoEnabled(marker);
    }

    @Override
    public void info(String msg) {
        findImplToUse().info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        findImplToUse().info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        findImplToUse().info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        findImplToUse().info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        findImplToUse().info(msg, t);
    }

    @Override
    public void info(Marker marker, String msg) {
        findImplToUse().info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        findImplToUse().info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        findImplToUse().info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        findImplToUse().info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        findImplToUse().info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return findImplToUse().isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return findImplToUse().isWarnEnabled(marker);
    }

    @Override
    public void warn(String msg) {
        findImplToUse().warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        findImplToUse().warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        findImplToUse().warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        findImplToUse().warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        findImplToUse().warn(msg, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        findImplToUse().warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        findImplToUse().warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        findImplToUse().warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        findImplToUse().warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        findImplToUse().warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return findImplToUse().isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return findImplToUse().isErrorEnabled(marker);
    }

    @Override
    public void error(String msg) {
        findImplToUse().error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        findImplToUse().error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        findImplToUse().error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        findImplToUse().error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        findImplToUse().error(msg, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        findImplToUse().error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        findImplToUse().error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        findImplToUse().error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        findImplToUse().error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        findImplToUse().error(marker, msg, t);
    }

    private Logger findImplToUse() {
        if (this.properLogger != null) {
            return this.properLogger;
        }

        if (getLoggerFactory() != null) {
            Logger implToUse = getLoggerFactory().getLogger(this.className);
            synchronized (this) {
                if (this.properLogger == null) {
                    this.properLogger = implToUse;
                }
            }
            return this.properLogger;
        } else {
            return this.FALLBACK_IMPL;
        }
    }
}
