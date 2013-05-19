import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.classic.filter.ThresholdFilter
import java.text.SimpleDateFormat
import java.util.Date

import static ch.qos.logback.classic.Level.INFO

appender("CONSOLE_APPENDER", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss:SSS} %-5level %msg %xEx%n"
    }
}

appender("STREAM2ES_APPENDER", FileAppender) {
    append = false
    def calculateFilename = {
    if (System.getProperty("s2es") != null) {
        return System.getProperty("s2es");
    } else {
        return "log/es-"+(new SimpleDateFormat("yyyyMMddahh").format(new Date())) +".txt";
    }
    }
    file = calculateFilename()
    encoder(PatternLayoutEncoder) {
        pattern = "%msg"
    }
}

appender("TEE_APPENDER", FileAppender) {
    append = false
  filter(ThresholdFilter) {
    level = INFO
  }
    file = "log/info.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy:MMM:dd-HH:mm:ss:SSS} %-5level %msg %xEx%n"
    }
}

root(INFO, ["CONSOLE_APPENDER", "TEE_APPENDER"])
logger("d", INFO, [])
logger("log.to", INFO, [], false)
logger("log.to.file", INFO, ["STREAM2ES_APPENDER"])
