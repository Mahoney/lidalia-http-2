package uk.org.lidalia
package exampleapp.system
package logging

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.spi.{ContextAware, LifeCycle}
import ch.qos.logback.core.status.{OnErrorConsoleStatusListener, OnConsoleStatusListener}
import ch.qos.logback.core.{AsyncAppenderBase, ConsoleAppender, UnsynchronizedAppenderBase}
import org.slf4j.{Logger, LoggerFactory}
import uk.org.lidalia.exampleapp.system.logging.JulConfigurer.sendJulToSlf4j

object LogbackConfigurer {

  private val logFactory = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  def configureLogback(loggerLevels: (String, Level)*) = {

    sendJulToSlf4j()
    logFactory.getStatusManager.add(started(new OnErrorConsoleStatusListener))
    logFactory.addListener(started(new LevelChangePropagator))

    val root = logFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    root.setLevel(Level.WARN)

    val asyncConsoleAppender = new AsyncAppenderBase[ILoggingEvent] {
      override def preprocess(eventObject: ILoggingEvent): Unit = eventObject.prepareForDeferredProcessing()
    }
    asyncConsoleAppender.setName("root_async")
    asyncConsoleAppender.setContext(logFactory)

    val sysOutConsoleAppender = consoleAppender("System.out")
    val sysErrConsoleAppender = consoleAppender("System.err")

    val sysConsoleAppender = new UnsynchronizedAppenderBase[ILoggingEvent] {
      override def append(eventObject: ILoggingEvent): Unit = {
        if (eventObject.getLevel.isGreaterOrEqual(Level.WARN)) {
          sysErrConsoleAppender.doAppend(eventObject)
        } else {
          sysOutConsoleAppender.doAppend(eventObject)
        }
      }
    }
    sysConsoleAppender.setName("console")
    asyncConsoleAppender.addAppender(started(sysConsoleAppender))

    root.detachAndStopAllAppenders()
    root.addAppender(started(asyncConsoleAppender))

    loggerLevels.foreach {
      case (loggerName, level) => logFactory.getLogger(loggerName).setLevel(level)
    }
  }

  private def consoleAppender(target: String) = {
    val ca: ConsoleAppender[ILoggingEvent] = new ConsoleAppender[ILoggingEvent]
    ca.setContext(logFactory)
    ca.setName(target)
    ca.setTarget(target)
    val pl: PatternLayoutEncoder = new PatternLayoutEncoder
    pl.setContext(logFactory)
    pl.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n")
    pl.start()
    ca.setEncoder(pl)
    ca.start()
    ca
  }

  private def started[T <: LifeCycle with ContextAware](thing: T): T = {
    thing.setContext(logFactory)
    thing.start()
    thing
  }
}
