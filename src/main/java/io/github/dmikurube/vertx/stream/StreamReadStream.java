/*
 * Copyright 2024 Dai MIKURUBE
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

package io.github.dmikurube.vertx.stream;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import java.util.stream.Stream;

public final class StreamReadStream implements ReadStream<Buffer> {
    public StreamReadStream(final Stream<?> stream) {
        this.stream = stream;
        this.exceptionHandler = (e -> {});
        this.handler = (e -> {});
        this.endHandler = (e -> {});
        this.readInProgress = false;
        this.demand = Long.MAX_VALUE;
    }

    @Override
    public StreamReadStream exceptionHandler(final Handler<Throwable> handler) {
        synchronized (this) {
            this.exceptionHandler = handler;
        }
        return this;
    }

    @Override
    public StreamReadStream handler(final Handler<Buffer> handler) {
        synchronized (this) {
            this.handler = handler;
        }
        return this;
    }

    @Override
    public StreamReadStream pause() {
        return this;
    }

    @Override
    public StreamReadStream resume() {
        return this;
    }

    @Override
    public StreamReadStream fetch(final long amount) {
        return this;
    }

    @Override
    public StreamReadStream endHandler(final Handler<Void> endHandler) {
        synchronized (this) {
            this.endHandler = endHandler;
        }
        return this;
    }

    private final Stream<?> stream;

    private Handler<Throwable> exceptionHandler;

    private Handler<Buffer> handler;

    private Handler<Void> endHandler;

    private boolean readInProgress;

    private long demand;
}