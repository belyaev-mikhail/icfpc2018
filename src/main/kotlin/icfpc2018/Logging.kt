package icfpc2018

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

// mmmagic!
// caller-dependent global property: hows that, Elon Musk?
val log: Logger
    inline get() = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

inline fun Logger.debug(message: () -> String) =
        if(isDebugEnabled) debug(message()) else {}
inline fun Logger.trace(message: () -> String) =
        if(isTraceEnabled) trace(message()) else {}
inline fun Logger.info(message: () -> String) =
        if(isInfoEnabled) info(message()) else {}
inline fun Logger.warn(message: () -> String) =
        if(isWarnEnabled) warn(message()) else {}
inline fun Logger.error(message: () -> String) =
        if(isErrorEnabled) error(message()) else {}
