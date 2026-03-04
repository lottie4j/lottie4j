package com.lottie4j.fxfileviewer.util;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Compact log formatter that produces concise, single-line log entries.
 * Output format: [Date Time] [Level] ClassName MethodName Message
 */
public class CompactFormatter extends Formatter {
    /**
     * Formats a log record into a compact single-line string.
     *
     * @param record the log record to format
     * @return formatted log string with timestamp, level, class, method, and message
     */
    @Override
    public String format(LogRecord record) {
        String className = record.getSourceClassName();
        // Get just the class name without package
        className = className.substring(className.lastIndexOf('.') + 1);

        String methodName = record.getSourceMethodName();
        // Trim method name if longer than 30 chars
        if (methodName.length() > 30) {
            methodName = methodName.substring(0, 27) + "...";
        }

        return String.format("[%1$tF %1$tT] [%2$-7s] %3$-20s %4$-30s %5$s%n",
                new Date(record.getMillis()),
                record.getLevel().getName(),
                className,
                methodName,
                formatMessage(record));
    }
}
