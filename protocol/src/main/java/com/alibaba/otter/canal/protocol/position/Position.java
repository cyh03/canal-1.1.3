package com.alibaba.otter.canal.protocol.position;

import com.alibaba.otter.canal.common.utils.CanalToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 事件唯一标示
 */
//
public abstract class Position implements Serializable {

    private static final long serialVersionUID = 2332798099928474975L;

    public String toString() {
        return ToStringBuilder.reflectionToString(this, CanalToStringStyle.DEFAULT_STYLE);
    }

}
