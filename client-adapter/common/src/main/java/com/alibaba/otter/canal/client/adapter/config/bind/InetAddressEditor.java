package com.alibaba.otter.canal.client.adapter.config.bind;

import java.beans.PropertyEditorSupport;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * {@link PropertyNamePatternsMatcher} that matches when a property name exactly
 * matches one of the given names, or starts with one of the given names
 * followed by a delimiter. This implementation is optimized for frequent calls.PropertyNamePatternsMatcher，当属性名与给定名称之一完全匹配时匹配，或者以给定名称之一开头，后跟分隔符时匹配。此实现针对频繁调用进行了优化。
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class InetAddressEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        return ((InetAddress) getValue()).getHostAddress();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(InetAddress.getByName(text));
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Cannot locate host", ex);
        }
    }

}
