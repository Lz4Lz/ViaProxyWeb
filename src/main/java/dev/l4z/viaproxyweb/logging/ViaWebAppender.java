package dev.l4z.viaproxyweb.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public final class ViaWebAppender extends AbstractAppender {

    public static final String NAME = "ViaWeb";
    private static ViaWebAppender installedAppender;

    private ViaWebAppender(
            String name,
            Filter filter,
            Layout<? extends Serializable> layout
    ) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
    }

    public static ViaWebAppender create(Configuration config) {
        PatternLayout layout = PatternLayout.newBuilder()
                .setPattern(
                        "%style{[%d{HH:mm:ss}]}{blue} " +
                                "%highlight{[%t/%level]}{" +
                                "FATAL=bg_red, ERROR=red, WARN=yellow, " +
                                "INFO=green, DEBUG=green, TRACE=blue} " +
                                "%style{(%logger{1})}{cyan} " +
                                "%msg"
                )
                .setDisableAnsi(false)
                .setCharset(StandardCharsets.UTF_8)
                .setConfiguration(config)
                .build();

        return new ViaWebAppender(NAME, null, layout);
    }
    
    public static void install(
            org.apache.logging.log4j.core.Logger viaLogger
    ) {
        if (installedAppender != null) {
            return;
        }

        LoggerContext ctx = viaLogger.getContext();
        Configuration config = ctx.getConfiguration();

        ViaWebAppender appender = create(config);
        appender.start();

        config.addAppender(appender);

        config.getRootLogger().addAppender(
                appender,
                Level.INFO,
                null
        );

        ctx.updateLoggers();

        installedAppender = appender;
    }

    public static void uninstall(
            org.apache.logging.log4j.core.Logger viaLogger
    ) {
        if (installedAppender == null) {
            return;
        }

        LoggerContext ctx = viaLogger.getContext();
        Configuration config = ctx.getConfiguration();

        config.getRootLogger().removeAppender(NAME);

        if (config instanceof org.apache.logging.log4j.core.config.AbstractConfiguration abstractConfig) {
            abstractConfig.removeAppender(NAME);
        }

        ctx.updateLoggers();

        installedAppender = null;
    }

    @Override
    public void append(LogEvent event) {
        Layout<? extends Serializable> layout = getLayout();

        if (layout == null) {
            return;
        }

        byte[] bytes = layout.toByteArray(event);

        String line = new String(bytes, StandardCharsets.UTF_8);

        LogBroadcast.publish(line);
    }
}