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
package org.apache.camel.util.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;

import org.apache.camel.Exchange;
import org.apache.camel.StreamCache;
import org.apache.camel.util.IOHelper;

/**
 * {@link org.apache.camel.StreamCache} implementation for {@link StringSource}s.
 * <p/>
 * <b>Important:</b> All the classes from the Camel release that implements {@link StreamCache} is NOT intended for end
 * users to create as instances, but they are part of Camels
 * <a href="https://camel.apache.org/manual/stream-caching.html">stream-caching</a> functionality.
 */
public final class SourceCache extends StringSource implements StreamCache {

    private static final @Serial long serialVersionUID = 1L;
    private final int length;

    public SourceCache(String data) {
        super(data);
        this.length = data.length();
    }

    public SourceCache() {
        throw new IllegalStateException();
    }

    @Override
    public void reset() {
        // do nothing here
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        IOHelper.copy(getInputStream(), os);
    }

    @Override
    public StreamCache copy(Exchange exchange) throws IOException {
        return new SourceCache(getText());
    }

    @Override
    public boolean inMemory() {
        return true;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long position() {
        return -1;
    }
}
