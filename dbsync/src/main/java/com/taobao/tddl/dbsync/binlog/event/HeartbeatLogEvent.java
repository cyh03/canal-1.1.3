package com.taobao.tddl.dbsync.binlog.event;

import com.taobao.tddl.dbsync.binlog.LogBuffer;
import com.taobao.tddl.dbsync.binlog.LogEvent;

/**
 * <pre>
 * Replication event to ensure to slave that master is alive.
 *   The event is originated by master's dump thread and sent straight to
 *   slave without being logged. Slave itself does not store it in relay log
 *   but rather uses a data for immediate checks and throws away the event.
 * 
 *   Two members of the class log_ident and Log_event::log_pos comprise 
 *   @see the event_coordinates instance. The coordinates that a heartbeat
 *   instance carries correspond to the last event master has sent from
 *   its binlog.复制事件，以确保从服务器的主服务器是活动的。
 * 事件由主转储线程发起，并直接发送到
 * 没有被记录的奴隶。从服务器本身并不将其存储在中继日志中
 * 而是使用数据进行即时检查并丢弃事件。
 *
 * log_ident和Log_event::log_pos类的两个成员组成
 * </pre>
 * 
 * @author jianghang 2013-4-8 上午12:36:29
 * @version 1.0.3
 * @since mysql 5.6
 */
public class HeartbeatLogEvent extends LogEvent {

    public static final int FN_REFLEN = 512; /* Max length of full path-name */
    private int             identLen;
    private String          logIdent;

    public HeartbeatLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent){
        super(header);

        final int commonHeaderLen = descriptionEvent.commonHeaderLen;
        identLen = buffer.limit() - commonHeaderLen;
        if (identLen > FN_REFLEN - 1) {
            identLen = FN_REFLEN - 1;
        }

        logIdent = buffer.getFullString(commonHeaderLen, identLen, LogBuffer.ISO_8859_1);
    }

    public int getIdentLen() {
        return identLen;
    }

    public String getLogIdent() {
        return logIdent;
    }

}
