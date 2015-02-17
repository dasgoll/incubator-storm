/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package backtype.storm.serialization;

import backtype.storm.generated.ErrorInfo;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;


public class ThriftBridgeSerializationDelegateTest {

    SerializationDelegate testDelegate;

    @Before
    public void setUp() throws Exception {
        testDelegate = new ThriftSerializationDelegateBridge();
        testDelegate.prepare(null);
    }

    @Test
    public void testNonThriftInstance() throws Exception {
        TestPojo pojo = new TestPojo();
        pojo.name = "foo";
        pojo.age = 100;

        byte[] serialized = new DefaultSerializationDelegate().serialize(pojo);

        TestPojo pojo2 = (TestPojo)testDelegate.deserialize(serialized, TestPojo.class);

        assertEquals(pojo2.name, pojo.name);
        assertEquals(pojo2.age, pojo.age);

        serialized = testDelegate.serialize(pojo);
        pojo2 = (TestPojo) new DefaultSerializationDelegate().deserialize(serialized, Serializable.class);
        assertEquals(pojo2.name, pojo.name);
        assertEquals(pojo2.age, pojo.age);
    }

    @Test
    public void testThriftInstance() throws Exception {
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.set_error("error");
        errorInfo.set_error_time_secs(1);
        errorInfo.set_host("host");
        errorInfo.set_port(1);

        byte[] serialized = new ThriftSerializationDelegate().serialize(errorInfo);
        ErrorInfo errorInfo2 = testDelegate.deserialize(serialized, ErrorInfo.class);
        assertEquals(errorInfo, errorInfo2);

        serialized = testDelegate.serialize(errorInfo);
        errorInfo2 = new ThriftSerializationDelegate().deserialize(serialized, ErrorInfo.class);
        assertEquals(errorInfo, errorInfo2);
    }

    static class TestPojo implements Serializable {
        String name;
        int age;
    }
}