/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.spi.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomConsumerExceptionHandlerTest extends ContextTestSupport {

    private static final CountDownLatch LATCH = new CountDownLatch(1);

    @Override
    protected Registry createCamelRegistry() throws Exception {
        Registry jndi = super.createCamelRegistry();
        jndi.bind("myHandler", new MyExceptionHandler());
        return jndi;
    }

    @Test
    public void testDeadLetterChannelAlwaysHandled() throws Exception {
        getMockEndpoint("mock:foo").expectedMessageCount(1);
        getMockEndpoint("mock:bar").expectedMessageCount(1);
        getMockEndpoint("mock:result").expectedMessageCount(0);

        template.sendBody("seda:foo", "Hello World");

        assertMockEndpointsSatisfied();

        assertTrue(LATCH.await(5, TimeUnit.SECONDS), "Should have been called");
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:foo?exceptionHandler=#myHandler").routeId("foo").to("mock:foo").to("direct:bar")
                        .to("mock:result");

                from("direct:bar").routeId("bar").onException(IllegalArgumentException.class).maximumRedeliveries(3)
                        .redeliveryDelay(0).end().to("mock:bar")
                        .throwException(new IllegalArgumentException("Forced"));
            }
        };
    }

    private static final class MyExceptionHandler implements ExceptionHandler {

        @Override
        public void handleException(Throwable exception) {
            LATCH.countDown();
        }

        @Override
        public void handleException(String message, Throwable exception) {
            LATCH.countDown();
        }

        @Override
        public void handleException(String message, Exchange exchange, Throwable exception) {
            LATCH.countDown();
        }
    }
}
