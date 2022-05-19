/*
 * Copyright 2018 Johns Hopkins University
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
package org.dataconservancy.fcrepo.jms;

import java.util.Enumeration;
import java.util.stream.Collectors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.fcrepo.jms.DefaultMessageFactory;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides JMS headers that can be used as {@link Message} selectors.
 * <h4>Limitations</h4>
 * <ul>
 *     <li>Only property names containing a period (0x2e) are considered for transformation</li>
 *     <li>Technically, a property name cannot be used as a selector if the name does not start with a valid
 *         {@link Character#isJavaIdentifierStart(char) Java identifier}, or if the property contains any character that
 *         is not a {@link Character#isJavaIdentifierPart(char) Java identifier part}</li>
 *     <li>Only properties with {@code String} values are considered</li>
 * </ul>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 * @see <a href="https://docs.oracle.com/javaee/7/api/javax/jms/Message.html">Message Selectors heading</a>
 */
public class HeaderTransformingMessageFactory extends DefaultMessageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HeaderTransformingMessageFactory.class);

    /**
     * Examines the properties set on the {@code Message} produced by the super class, and transforms any property names
     * that cannot be used as {@code Message} selectors.  The transformed property name is added to the {@code Message},
     * with a copy of the original value.  Receivers, if they are aware of the transformation algorithm, can use the
     * transformed version of the property name as a {@code Message} selector.
     *
     * @param event      the Fedora event
     * @param jmsSession the current JMS Session
     * @return a {@code Message} that may carry additional properties viable for {@code Message} selection
     * @throws JMSException
     */
    @Override
    public Message getMessage(FedoraEvent event, Session jmsSession) throws JMSException {
        LOG.debug(">>>> Generating JMS for resource {}", event.getPath());
        Message message = super.getMessage(event, jmsSession);

        Enumeration propertyNames = message.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            Object propObj = propertyNames.nextElement();
            if (!(propObj instanceof String)) {
                continue;
            }

            String propStr = (String) propObj;

            // TODO fully validate property names
            if (propStr.contains(".")) {
                // TODO consider other property types, right now only string properties are considered
                try {
                    String transformedProp = transform(propStr);
                    String value = message.getStringProperty((String) propObj);
                    LOG.debug(">>>> Adding JMS header '{}', with value '{}' to message {}",
                              transformedProp, value, message);
                    message.setStringProperty(transformedProp, value);
                } catch (Exception e) {
                    LOG.error("Error transforming property name {} to a String value: {}", propObj, e.getMessage(), e);
                }
            }

        }

        return message;
    }

    /**
     * Transforms the supplied property name by examining each character of the name in sequence, and:
     * <ol>
     *     <li>If the first character is not a {@link Character#isJavaIdentifierStart(char)}, drop characters until
     *     one is found.</li>
     *     <li>If subsequent characters are not a {@link Character#isJavaIdentifierPart(char)}, drop them.</li>
     *     <li>If a character is dropped, and a valid first character has been found, uppercase the next valid
     *     character</li>
     * </ol>
     *
     * @param propertyName the JMS property name that may not viable for use as a {@code Message} selector
     * @return the transformed property name, which may be unchanged if the supplied name was viable as a {@code
     * Message} selector
     */
    static String transform(String propertyName) {
        final byte[] first = {0x01};
        final byte[] flag = {0x00};

        return propertyName.chars().sequential().mapToObj((c) -> {
            if (first[0] == 0x01) {
                if (!Character.isJavaIdentifierStart(c)) {
                    return String.valueOf("");
                } else {
                    first[0] = 0x00;
                    return Character.toString((char) c);
                }
            }

            if (Character.isJavaIdentifierPart(c) && flag[0] == 0x00) {
                return Character.toString((char) c);
            }

            if (Character.isJavaIdentifierPart(c) && flag[0] != 0x00) {
                flag[0] = 0x00;
                return Character.toString(Character.toUpperCase((char) c));
            }

            // Character is illegal, so we drop it and camel case the next legal character.
            flag[0] = 0x01;
            return String.valueOf("");
        }).collect(Collectors.joining());
    }

}
