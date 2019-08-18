package com.alibaba.otter.canal.client.adapter.config.common;

import org.springframework.util.ObjectUtils;

/**
 * A {@link PropertySource} implementation capable of interrogating its
 * underlying source object to enumerate all possible property name/value pairs.
 * Exposes the {@link #getPropertyNames()} method to allow callers to introspect
 * available properties without having to access the underlying source object.
 * This also facilitates a more efficient implementation of
 * {@link #containsProperty(String)}, in that it can call
 * {@link #getPropertyNames()} and iterate through the returned array rather
 * than attempting a call to {@link #getProperty(String)} which may be more
 * expensive. Implementations may consider caching the result of
 * {@link #getPropertyNames()} to fully exploit this performance opportunity.
 * <p>
 * Most framework-provided {@code PropertySource} implementations are
 * enumerable; a counter-example would be {@code JndiPropertySource} where, due
 * to the nature of JNDI it is not possible to determine all possible property
 * names at any given time; rather it is only possible to try to access a
 * property (via {@link #getProperty(String)}) in order to evaluate whether it
 * is present or not.一种PropertySource实现，能够询问其底层源对象，以枚举所有可能的属性名称/值对。公开getPropertyNames()方法，以允许调用者在不访问底层源对象的情况下内省可用属性。这还促进了containsProperty(String)的更有效实现，因为它可以调用getPropertyNames()并遍历返回的数组，而不是尝试调用getProperty(String)，后者可能更昂贵。实现可以考虑缓存getPropertyNames()的结果，以充分利用这个性能机会。
 * 大多数框架提供的PropertySource实现都是可枚举的;一个反例是JndiPropertySource，由于JNDI的性质，不可能在任何给定时间确定所有可能的属性名称;相反，只有尝试访问一个属性(通过getProperty(String))才能评估它是否存在。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

    public EnumerablePropertySource(String name, T source){
        super(name, source);
    }

    protected EnumerablePropertySource(String name){
        super(name);
    }

    /**
     * Return whether this {@code PropertySource} contains a property with the given
     * name.
     * <p>
     * This implementation checks for the presence of the given name within the
     * {@link #getPropertyNames()} array.
     *
     * @param name the name of the property to find
     */
    @Override
    public boolean containsProperty(String name) {
        return ObjectUtils.containsElement(getPropertyNames(), name);
    }

    /**
     * Return the names of all properties contained by the {@linkplain #getSource()
     * source} object (never {@code null}).
     */
    public abstract String[] getPropertyNames();

}
