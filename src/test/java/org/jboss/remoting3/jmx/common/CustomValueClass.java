package org.jboss.remoting3.jmx.common;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class CustomValueClass implements Serializable {

    private final String value;

    public CustomValueClass(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
