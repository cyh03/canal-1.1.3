package com.taobao.tddl.dbsync.binlog.event;

import com.taobao.tddl.dbsync.binlog.LogBuffer;
import com.taobao.tddl.dbsync.binlog.LogContext;
import com.taobao.tddl.dbsync.binlog.LogEvent;
import com.taobao.tddl.dbsync.binlog.event.TableMapLogEvent.ColumnInfo;

import java.util.BitSet;

/**
 * Common base class for all row-containing log events.所有包含行的日志事件的公共基类。
 * 
 * @author <a href="mailto:changyuan.lh@taobao.com">Changyuan.lh</a>
 * @version 1.0
 */
public abstract class RowsLogEvent extends LogEvent {

    /**
     * Fixed data part:
     * <ul>
     * <li>6 bytes. The table ID.</li>
     * <li>2 bytes. Reserved for future use.</li>
     * </ul>
     * <p>
     * Variable data part:
     * <ul>
     * <li>Packed integer. The number of columns in the table.</li>
     * <li>Variable-sized. Bit-field indicating whether each column is used, one
     * bit per column. For this field, the amount of storage required for N
     * columns is INT((N+7)/8) bytes.</li>
     * <li>Variable-sized (for UPDATE_ROWS_LOG_EVENT only). Bit-field indicating
     * whether each column is used in the UPDATE_ROWS_LOG_EVENT after-image; one
     * bit per column. For this field, the amount of storage required for N
     * columns is INT((N+7)/8) bytes.</li>
     * <li>Variable-sized. A sequence of zero or more rows. The end is
     * determined by the size of the event. Each row has the following format:
     * <ul>
     * <li>Variable-sized. Bit-field indicating whether each field in the row is
     * NULL. Only columns that are "used" according to the second field in the
     * variable data part are listed here. If the second field in the variable
     * data part has N one-bits, the amount of storage required for this field
     * is INT((N+7)/8) bytes.</li>
     * <li>Variable-sized. The row-image, containing values of all table fields.
     * This only lists table fields that are used (according to the second field
     * of the variable data part) and non-NULL (according to the previous
     * field). In other words, the number of values listed here is equal to the
     * number of zero bits in the previous field (not counting padding bits in
     * the last byte). The format of each value is described in the
     * log_event_print_value() function in log_event.cc.</li>
     * <li>(for UPDATE_ROWS_EVENT only) the previous two fields are repeated,
     * representing a second table row.</li>固定的数据部分:
     * 6字节。表的ID。
     * 2字节。保留以备将来使用。
     * 可变数据部分:
     * 拥挤的整数。表中的列数。
     * 大小可变的。位字段，表示是否使用每一列，每一列一个位。对于这个字段，N列所需的存储量是INT((N+7)/8)字节。
     * 可变大小(仅适用于UPDATE_ROWS_LOG_EVENT)。位字段，指示UPDATE_ROWS_LOG_EVENT后像中是否使用每一列;每列一个位。对于这个字段，N列所需的存储量是INT((N+7)/8)字节。
     * 大小可变的。零行或多行序列。结束由事件的大小决定。每一行的格式如下:
     * 大小可变的。位字段，指示行中的每个字段是否为空。这里只列出根据变量数据部分中的第二个字段“使用”的列。如果变量数据部分中的第二个字段有N个1位，则该字段所需的存储量为INT((N+7)/8)字节。
     * 大小可变的。行映像，包含所有表字段的值。这只列出使用的表字段(根据变量数据部分的第二个字段)和非null(根据前面的字段)。换句话说，这里列出的值的数量等于前一个字段中的零位的数量(不包括最后一个字节中的填充位)。每个值的格式在log_event.cc中的log_event_print_value()函数中描述。
     * (仅对UPDATE_ROWS_EVENT)重复前面的两个字段，表示第二个表行。
     * </ul>
     * </ul>
     * Source : http://forge.mysql.com/wiki/MySQL_Internals_Binary_Log
     */
    private final long       tableId;                           /* Table ID */
    private TableMapLogEvent table;                             /*
                                                                  * The table
                                                                  * the rows
                                                                  * belong to
                                                                  */

    /** Bitmap denoting columns available */
    protected final int      columnLen;
    protected final boolean  partial;
    protected final BitSet   columns;

    /**
     * Bitmap for columns available in the after image, if present. These fields
     * are only available for Update_rows events. Observe that the width of both
     * the before image COLS vector and the after image COLS vector is the same:
     * the number of columns of the table on the master.在after图像中可用列的位图(如果存在)。这些字段只对Update_rows事件可用。注意，前图像COLS向量和后图像COLS向量的宽度相同:主表上的列数。
     */
    protected final BitSet   changeColumns;

    protected int            jsonColumnCount         = 0;

    /** XXX: Don't handle buffer in another thread. */
    private final LogBuffer  rowsBuf;                           /*
                                                                  * The rows in
                                                                  * packed
                                                                  * format
                                                                  */

    /**
     * enum enum_flag These definitions allow you to combine the flags into an
     * appropriate flag set using the normal bitwise operators. The implicit
     * conversion from an enum-constant to an integer is accepted by the
     * compiler, which is then used to set the real set of flags.enum enum_flag这些定义允许您使用普通的位操作符将标志组合到适当的标志集中。编译器接受从枚举常量到整数的隐式转换，然后用它设置真正的标志集。
     */
    private final int        flags;

    /** Last event of a statement */
    public static final int  STMT_END_F              = 1;

    /** Value of the OPTION_NO_FOREIGN_KEY_CHECKS flag in thd->options */
    public static final int  NO_FOREIGN_KEY_CHECKS_F = (1 << 1);

    /** Value of the OPTION_RELAXED_UNIQUE_CHECKS flag in thd->options */
    public static final int  RELAXED_UNIQUE_CHECKS_F = (1 << 2);

    /**
     * Indicates that rows in this event are complete, that is contain values
     * for all columns of the table.指示此事件中的行已完成，即包含表中所有列的值。
     */
    public static final int  COMPLETE_ROWS_F         = (1 << 3);

    /* RW = "RoWs" */
    public static final int  RW_MAPID_OFFSET         = 0;
    public static final int  RW_FLAGS_OFFSET         = 6;
    public static final int  RW_VHLEN_OFFSET         = 8;
    public static final int  RW_V_TAG_LEN            = 1;
    public static final int  RW_V_EXTRAINFO_TAG      = 0;

    public RowsLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent){
        this(header, buffer, descriptionEvent, false);
    }

    public RowsLogEvent(LogHeader header, LogBuffer buffer, FormatDescriptionLogEvent descriptionEvent, boolean partial){
        super(header);

        final int commonHeaderLen = descriptionEvent.commonHeaderLen;
        final int postHeaderLen = descriptionEvent.postHeaderLen[header.type - 1];
        int headerLen = 0;
        buffer.position(commonHeaderLen + RW_MAPID_OFFSET);
        if (postHeaderLen == 6) {
            /*
             * Master is of an intermediate source tree before 5.1.4. Id is 4
             * bytes
             */
            tableId = buffer.getUint32();
        } else {
            tableId = buffer.getUlong48(); // RW_FLAGS_OFFSET
        }
        flags = buffer.getUint16();

        if (postHeaderLen == FormatDescriptionLogEvent.ROWS_HEADER_LEN_V2) {
            headerLen = buffer.getUint16();
            headerLen -= 2;
            int start = buffer.position();
            int end = start + headerLen;
            for (int i = start; i < end;) {
                switch (buffer.getUint8(i++)) {
                    case RW_V_EXTRAINFO_TAG:
                        // int infoLen = buffer.getUint8();
                        buffer.position(i + EXTRA_ROW_INFO_LEN_OFFSET);
                        int checkLen = buffer.getUint8(); // EXTRA_ROW_INFO_LEN_OFFSET
                        int val = checkLen - EXTRA_ROW_INFO_HDR_BYTES;
                        assert (buffer.getUint8() == val); // EXTRA_ROW_INFO_FORMAT_OFFSET
                        for (int j = 0; j < val; j++) {
                            assert (buffer.getUint8() == val); // EXTRA_ROW_INFO_HDR_BYTES
                                                               // + i
                        }
                        break;
                    default:
                        i = end;
                        break;
                }
            }
        }

        buffer.position(commonHeaderLen + postHeaderLen + headerLen);
        columnLen = (int) buffer.getPackedLong();
        this.partial = partial;
        columns = buffer.getBitmap(columnLen);

        if (header.type == UPDATE_ROWS_EVENT_V1 || header.type == UPDATE_ROWS_EVENT
            || header.type == PARTIAL_UPDATE_ROWS_EVENT) {
            changeColumns = buffer.getBitmap(columnLen);
        } else {
            changeColumns = columns;
        }

        // XXX: Don't handle buffer in another thread.
        int dataSize = buffer.limit() - buffer.position();
        rowsBuf = buffer.duplicate(dataSize);
    }

    public final void fillTable(LogContext context) {
        table = context.getTable(tableId);

        // end of statement check:
        if ((flags & RowsLogEvent.STMT_END_F) != 0) {
            // Now is safe to clear ignored map (clear_tables will also
            // delete original table map events stored in the map).
            context.clearAllTables();
        }

        int jsonColumnCount = 0;
        int columnCnt = table.getColumnCnt();
        ColumnInfo[] columnInfo = table.getColumnInfo();
        for (int i = 0; i < columnCnt; i++) {
            ColumnInfo info = columnInfo[i];
            if (info.type == LogEvent.MYSQL_TYPE_JSON) {
                jsonColumnCount++;
            }
        }
        this.jsonColumnCount = jsonColumnCount;
    }

    public final long getTableId() {
        return tableId;
    }

    public final TableMapLogEvent getTable() {
        return table;
    }

    public final BitSet getColumns() {
        return columns;
    }

    public final BitSet getChangeColumns() {
        return changeColumns;
    }

    public final RowsLogBuffer getRowsBuf(String charsetName) {
        return new RowsLogBuffer(rowsBuf, columnLen, charsetName, jsonColumnCount, partial);
    }

    public final int getFlags(final int flags) {
        return this.flags & flags;
    }
}
