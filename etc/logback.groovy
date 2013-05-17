import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import java.text.SimpleDateFormat
import java.util.Date

import static ch.qos.logback.classic.Level.INFO

appender("CONSOLE_APPENDER", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} %-5level %logger{24} - %msg %xEx%n"
    }
}

def logfile() {
    if (System.getProperty("s2es") != null) {
        return System.getProperty("s2es");
    } else {
        return "log/es-"+(new SimpleDateFormat("yyyyMMddhh").format(new Date())) +".txt";
    }
}

appender("STREAM2ES_APPENDER", FileAppender) {
    append = false
    file = logfile()
    encoder(PatternLayoutEncoder) {
        pattern = "%msg%n"
    }
}

root(INFO, ["CONSOLE_APPENDER"])
logger("d", INFO, [])
logger("log.to", INFO, [], false)
logger("log.to.file", INFO, ["STREAM2ES_APPENDER"])
