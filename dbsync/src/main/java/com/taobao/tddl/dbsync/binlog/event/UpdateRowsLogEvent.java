package com.taobao.tddl.dbsync.binlog.event;

import com.taobao.tddl.dbsync.binlog.LogBuffer;

/**
 * Log row updates with a before image. The event contain several update rows
 * for a table. Note that each event contains only rows for one table. Also note
 * that the row data consists of pairs of row data: one row for the old data and
 * one row for the new data.使用before映像更新日志行。该事件包含表的多个更新行。注意，每个事件只包含一个表的行。还要注意，行数据由行数据对组成:一行用于旧数据，一行用于新数据。
 * 
 * @author <a href="mailto:changyuan.lh@taobao.com">Changyuan.lh</a>
 * @version 1.0
 */
public final class UpdateRowsLogEvent extends RowsLogEvent {

    public UpdateRowsLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent){
        super(header, buffer, descriptionEvent, false);
    }

    public UpdateRowsLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent,
                              boolean partial){
        super(header, buffer, descriptionEvent, partial);
    }
}
