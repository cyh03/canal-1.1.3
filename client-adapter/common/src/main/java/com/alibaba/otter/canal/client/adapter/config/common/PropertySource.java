package com.alibaba.otter.canal.client.adapter.config.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class representing a source of name/value property pairs. The
 * underlying {@linkplain #getSource() source object} may be of any type
 * {@code T} that encapsulates properties. Examples include
 * {@link java.util.Properties} objects, {@link java.util.Map} objects,
 * {@code ServletContext} and {@code ServletConfig} objects (for access to init
 * parameters). Explore the {@code PropertySource} type hierarchy to see
 * provided implementations.
 * <p>
 * {@code PropertySource} objects are not typically used in isolation, but
 * rather through a {@link PropertySources} object, which aggregates property
 * sources and in conjunction with a {@link PropertyResolver} implementation
 * that can perform precedence-based searches across the set of
 * {@code PropertySources}.
 * <p>
 * {@code PropertySource} identity is determined not based on the content of
 * encapsulated properties, but rather based on the {@link #getName() name} of
 * the {@code PropertySource} alone. This is useful for manipulating
 * {@code PropertySource} objects when in collection contexts. See operations in
 * {@link MutablePropertySources} as well as the {@link #named(String)} and
 * {@link #toString()} methods for details.
 * <p>
 * Note that when working
 * with @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes that the @{@link PropertySource PropertySource}
 * annotation provides a convenient and declarative way of adding property
 * sources to the enclosing {@code Environment}.表示名称/值属性对的源的抽象基类。底层源对象可以是封装属性的任何类型的T。例子包括java.util。属性对象,java.util。映射对象、ServletContext和ServletConfig对象(用于访问初始化参数)。研究PropertySource类型层次结构，查看提供的实现。
 * PropertySource对象通常不是单独使用的，而是通过PropertySources对象使用的，该对象聚合属性源，并与PropertyResolver实现结合使用，后者可以跨一组PropertySources执行基于先例的搜索。
 * PropertySource标识不是根据封装属性的内容确定的，而是仅根据PropertySource的名称确定的。这对于在集合上下文中操作PropertySource对象非常有用。有关详细信息，请参见MutablePropertySources中的操作以及named(String)和toString()方法。
 * 注意，当使用@Configuration类时，@PropertySource注释提供了一种方便且声明性的方法，可以将属性源添加到封闭的环境中。
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySources
 * @see MutablePropertySources
 * @see PropertySource
 */
public abstract class PropertySource<T> {

    protected final Log    logger = LogFactory.getLog(getClass());

    protected final String name;

    protected final T      source;

    /**
     * Create a new {@code PropertySource} with the given name and source object.
     */
    public PropertySource(String name, T source){
        Assert.hasText(name, "Property source name must contain at least one character");
        Assert.notNull(source, "Property source must not be null");
        this.name = name;
        this.source = source;
    }

    /**
     * Create a new {@code PropertySource} with the given name and with a new
     * {@code Object} instance as the underlying source.
     * <p>
     * Often useful in testing scenarios when creating anonymous implementations
     * that never query an actual source but rather return hard-coded values.创建一个新的PropertySource，使用给定的名称和一个新的对象实例作为基础源。
     * 在创建从不查询实际源而是返回硬编码值的匿名实现时，在测试场景中通常很有用。
     */
    @SuppressWarnings("unchecked")
    public PropertySource(String name){
        this(name, (T) new Object());
    }

    /**
     * Return the name of this {@code PropertySource}
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the underlying source object for this {@code PropertySource}.返回此属性源的基础源对象。
     */
    public T getSource() {
        return this.source;
    }

    /**
     * Return whether this {@code PropertySource} contains the given name.
     * <p>
     * This implementation simply checks for a {@code null} return value from
     * {@link #getProperty(String)}. Subclasses may wish to implement a more
     * efficient algorithm if possible.返回此PropertySource是否包含给定名称。
     * 这个实现只是检查getProperty(String)的空返回值。子类可能希望实现更有效的算法。
     *
     * @param name the property name to find
     */
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * Return the value associated with the given name, or {@code null} if not
     * found.返回与给定名称关联的值，如果没有找到，则返回null。
     *
     * @param name the property to find
     */
    public abstract Object getProperty(String name);

    /**
     * This {@code PropertySource} object is equal to the given object if:
     * <ul>
     * <li>they are the same instance
     * <li>the {@code name} properties for both objects are equal
     * </ul>
     * <p>
     * No properties other than {@code name} are evaluated.
     */
    @Override
    public boolean equals(Object obj) {
        return (this == obj || (obj instanceof PropertySource
                                && ObjectUtils.nullSafeEquals(this.name, ((PropertySource<?>) obj).name)));
    }

    /**
     * Return a hash code derived from the {@code name} property of this
     * {@code PropertySource} object.
     */
    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.name);
    }

    /**
     * Produce concise output (type and name) if the current log level does not
     * include debug. If debug is enabled, produce verbose output including the hash
     * code of the PropertySource instance and every name/value property pair.
     * <p>
     * This variable verbosity is useful as a property source such as system
     * properties or environment variables may contain an arbitrary number of
     * property pairs, potentially leading to difficult to read exception and log
     * messages.
     *
     * @see Log#isDebugEnabled()
     */
    @Override
    public String toString() {
        if (logger.isDebugEnabled()) {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this) + " {name='" + this.name
                   + "', properties=" + this.source + "}";
        } else {
            return getClass().getSimpleName() + " {name='" + this.name + "'}";
        }
    }

    /**
     * Return a {@code PropertySource} implementation intended for collection
     * comparison purposes only.
     * <p>
     * Primarily for internal use, but given a collection of {@code PropertySource}
     * objects, may be used as follows:
     *
     * <pre class="code">
     *
     * {
     *     &#64;code
     *     List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
     *     sources.add(new MapPropertySource("sourceA", mapA));
     *     sources.add(new MapPropertySource("sourceB", mapB));
     *     assert sources.contains(PropertySource.named("sourceA"));
     *     assert sources.contains(PropertySource.named("sourceB"));
     *     assert !sources.contains(PropertySource.named("sourceC"));
     * }
     * </pre>
     *
     * The returned {@code PropertySource} will throw
     * {@code UnsupportedOperationException} if any methods other than
     * {@code equals(Object)}, {@code hashCode()}, and {@code toString()} are
     * called.Return a PropertySource implementation intended for collection comparison purposes only.
     * The returned PropertySource will throw UnsupportedOperationException if any methods other than equals(Object), hashCode(), and toString() are called.
     * 返回仅用于集合比较目的的PropertySource实现。
     * 如果调用了除equals(Object)、hashCode()和toString()之外的任何方法，返回的PropertySource将抛出UnsupportedOperationException。返回仅用于集合比较目的的PropertySource实现。如果调用了除equals(Object)、hashCode()和toString()之外的任何方法，返回的PropertySource将抛出UnsupportedOperationException。
     *
     * @param name the name of the comparison {@code PropertySource} to be created
     *     and returned.
     */
    public static PropertySource<?> named(String name) {
        return new ComparisonPropertySource(name);
    }

    /**
     * {@code PropertySource} to be used as a placeholder in cases where an actual
     * property source cannot be eagerly initialized at application context creation
     * time. For example, a {@code ServletContext}-based property source must wait
     * until the {@code ServletContext} object is available to its enclosing
     * {@code ApplicationContext}. In such cases, a stub should be used to hold the
     * intended default position/order of the property source, then be replaced
     * during context refresh.在实际属性源不能在应用程序上下文创建时立即初始化的情况下用作占位符。例如，基于ServletContext的属性源必须等待，直到ServletContext对象对其封装的ApplicationContext可用。在这种情况下，应该使用存根来保存属性源的默认位置/顺序，然后在上下文刷新期间替换存根。
     *
     * @see org.springframework.web.context.support.StandardServletEnvironment
     * @see org.springframework.web.context.support.ServletContextPropertySource
     */
    public static class StubPropertySource extends PropertySource<Object> {

        public StubPropertySource(String name){
            super(name, new Object());
        }

        /**
         * Always returns {@code null}.
         */
        @Override
        public String getProperty(String name) {
            return null;
        }
    }

    /**
     * @see PropertySource#named(String)
     */
    static class ComparisonPropertySource extends StubPropertySource {

        private static final String USAGE_ERROR = "ComparisonPropertySource instances are for use with collection comparison only";

        public ComparisonPropertySource(String name){
            super(name);
        }

        @Override
        public Object getSource() {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public boolean containsProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        @Override
        public String getProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }
    }

}
