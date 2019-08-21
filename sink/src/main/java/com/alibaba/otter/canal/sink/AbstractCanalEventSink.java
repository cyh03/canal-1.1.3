package com.alibaba.otter.canal.sink;

import com.alibaba.otter.canal.common.AbstractCanalLifeCycle;
import com.alibaba.otter.canal.filter.CanalEventFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jianghang 2012-7-23 下午01:02:45
 */
//
public abstract class AbstractCanalEventSink<T> extends AbstractCanalLifeCycle implements CanalEventSink<T> {

    protected CanalEventFilter                  filter;
    protected List<CanalEventDownStreamHandler> handlers = new ArrayList<CanalEventDownStreamHandler>();

    public void setFilter(CanalEventFilter filter) {
        this.filter = filter;
    }

    public void addHandler(CanalEventDownStreamHandler handler) {
        this.handlers.add(handler);
    }

    public CanalEventDownStreamHandler getHandler(int index) {
        return this.handlers.get(index);
    }

    public void addHandler(CanalEventDownStreamHandler handler, int index) {
        this.handlers.add(index, handler);
    }

    public void removeHandler(int index) {
        this.handlers.remove(index);
    }

    public void removeHandler(CanalEventDownStreamHandler handler) {
        this.handlers.remove(handler);
    }

    public CanalEventFilter getFilter() {
        return filter;
    }

    public List<CanalEventDownStreamHandler> getHandlers() {
        return handlers;
    }

    public void interrupt() {
        // do nothing
    }

}
