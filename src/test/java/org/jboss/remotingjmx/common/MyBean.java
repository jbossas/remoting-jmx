/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.remotingjmx.common;

/**
 * A simple MBean used for testing.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class MyBean implements MyBeanMBean {

    private String someValue;
    private String anotherValue;

    // The no-arg constructor for the most basic create.
    public MyBean() {
    }

    public MyBean(String someValue, String anotherValue) {
        this.someValue = someValue;
        this.anotherValue = anotherValue;
    }

    public String getSomeValue() {
        return someValue;
    }

    public void setSomeValue(String someValue) {
        this.someValue = someValue;
    }

    public String getAnotherValue() {
        return anotherValue;
    }

    public void setAnotherValue(String anotherValue) {
        this.anotherValue = anotherValue;
    }

    public String transpose(final String message) {
        StringBuilder sb = new StringBuilder(message.length());
        for (int i = message.length() - 1; i >= 0; i--) {
            sb.append(message.charAt(i));
        }

        return sb.toString();
    }

    @Override
    public String extractCustomValue(final CustomValueClass value) {
        return value.getValue();
    }

}
