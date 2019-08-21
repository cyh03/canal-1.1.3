package com.taobao.tddl.dbsync.binlog.event;

import com.taobao.tddl.dbsync.binlog.LogBuffer;
import com.taobao.tddl.dbsync.binlog.LogEvent;

/**
 * <pre>
 *   Base class for ignorable log events. Events deriving from
 *   this class can be safely ignored by slaves that cannot
 *   recognize them. Newer slaves, will be able to read and
 *   handle them. This has been designed to be an open-ended
 *   architecture, so adding new derived events shall not harm
 *   the old slaves that support ignorable log event mechanism
 *   (they will just ignore unrecognized ignorable events).
 * 
 *   @note The only thing that makes an event ignorable is that it has
 *   the LOG_EVENT_IGNORABLE_F flag set.  It is not strictly necessary
 *   that ignorable event types derive from Ignorable_log_event; they may
 *   just as well derive from Log_event and pass LOG_EVENT_IGNORABLE_F as
 *   argument to the Log_event constructor.可忽略日志事件的基类。事件源于
 * 这个类可以被不能忽略的奴隶安全地忽略
 * 认识他们。较新的奴隶，将能够阅读和
 * 处理它们。这是一个开放式的设计
 * 因此，添加新的派生事件不会造成损害
 * 支持可忽略日志事件机制的旧从属项
 * (他们只会忽略无法识别的、可忽略的事件)。
 * </pre>
 * 
 * @author jianghang 2013-4-8 上午12:36:29
 * @version 1.0.3
 * @since mysql 5.6
 */
public class IgnorableLogEvent extends LogEvent {

    public IgnorableLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent){
        super(header);

        // do nothing , just ignore log event
    }

}
