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

import static org.dataconservancy.fcrepo.jms.HeaderTransformingMessageFactory.transform;
import static org.junit.Assert.assertEquals;

import org.fcrepo.jms.DefaultMessageFactory;
import org.junit.Test;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class HeaderTransformingMessageFactoryTest {

    @Test
    public void testTransformFcrepoHeader() throws Exception {
        assertEquals("orgFcrepoJmsEventType", transform(DefaultMessageFactory.EVENT_TYPE_HEADER_NAME));
    }

    @Test
    public void testTransformOkHeader() throws Exception {
        assertEquals("JMSCorrelationID", transform("JMSCorrelationID"));
    }

    @Test
    public void testTransformHeaderEndingInPeriod() throws Exception {
        assertEquals("orgFcrepoJmsEventType", transform(DefaultMessageFactory.EVENT_TYPE_HEADER_NAME + "."));
    }

    @Test
    public void testTransformHeaderStartingWithPeriod() throws Exception {
        assertEquals("orgFcrepoJmsEventType", transform("." + DefaultMessageFactory.EVENT_TYPE_HEADER_NAME));
    }

    @Test
    public void testTransformWithOtherIllegalChars() throws Exception {
        assertEquals("ABCD", transform("-*A/B#C\\D"));
    }

    @Test
    public void testTransformWithJavaIdentifierStart() throws Exception {
        assertEquals("$1234", transform("$1234"));
    }

    @Test
    public void testTransformWithIllegalStart() throws Exception {
        assertEquals("$1234", transform(".$1234"));
    }

    @Test
    public void testTransformWithJavaIdentifierStartAndContains() throws Exception {
        assertEquals("$12$34", transform(".$12$34"));
    }

    @Test
    public void testTransformIllegalChars() throws Exception {
        assertEquals("", transform("...."));
    }

    @Test
    public void testTransformIllegalCharsWithOkStart() throws Exception {
        assertEquals("$", transform("$...."));
    }

    @Test
    public void testTransformIllegalCharsToOkStart() throws Exception {
        assertEquals("$abc", transform("...$abc"));
    }
}