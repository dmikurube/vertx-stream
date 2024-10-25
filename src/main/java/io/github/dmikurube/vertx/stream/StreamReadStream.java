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

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public final class StreamReadStream implements ReadStream<Buffer> {
    public StreamReadStream(final Vertx vertx, final Stream<?> stream, final String head, final String foot) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(stream);

        this.vertx = vertx;
        this.context = vertx.getOrCreateContext();

        this.stream = stream;
        this.iter = stream.iterator();

        this.head = head;
        this.foot = foot;

        this.buffer = Buffer.buffer(65536);
        this.buffer.appendString(head);

        this.exceptionHandler = (e -> {});
        this.handler = (e -> {});
        this.endHandler = (e -> {});
        this.readInProgress = false;
        this.isStreamEnded = false;
        this.demand = Long.MAX_VALUE;
    }

    @Override
    public StreamReadStream exceptionHandler(final Handler<Throwable> handler) {
        Objects.requireNonNull(handler);
        synchronized (this) {
            this.exceptionHandler = handler;
        }
        return this;
    }

    @Override
    public StreamReadStream handler(final Handler<Buffer> handler) {
        Objects.requireNonNull(handler);
        synchronized (this) {
            this.handler = handler;
            if (handler != null && this.demand > 0) {
                this.readFromStreamAndPushToBufferAndDoHandleIfRequired();
            }
        }
        return this;
    }

    @Override
    public StreamReadStream pause() {
        synchronized (this) {
            this.demand = 0L;
        }
        return this;
    }

    @Override
    public StreamReadStream resume() {
        synchronized (this) {
            this.fetch(Long.MAX_VALUE);
        }
        return this;
    }

    @Override
    public StreamReadStream fetch(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative.");
        }
        synchronized (this) {
            if ((Long.MAX_VALUE - amount) >= this.demand) {  // Avoid overflow.
                this.demand = this.demand + amount;
            } else {
                this.demand = Long.MAX_VALUE;
            }
            this.readFromStreamAndPushToBufferAndDoHandleIfRequired();
        }
        return this;
    }

    @Override
    public StreamReadStream endHandler(final Handler<Void> endHandler) {
        Objects.requireNonNull(endHandler);
        synchronized (this) {
            this.endHandler = endHandler;
        }
        return this;
    }

    private static class ReadResult {
        public ReadResult(final String string, final boolean hasNext) {
            this.string = string;
            this.hasNext = hasNext;
        }

        @Override
        public String toString() {
            return this.string;
        }

        public boolean hasNext() {
            return this.hasNext;
        }

        private final String string;
        private final boolean hasNext;
    }

    private void readFromStreamAndPushToBufferAndDoHandleIfRequired() {
        System.out.println("reading...");
        if (this.readInProgress) {
            System.out.println("reading in progress");
            // Schedule next read.
            if (!this.isStreamEnded && this.demand > 0) {
                this.context.runOnContext(v -> this.readFromStreamAndPushToBufferAndDoHandleIfRequired());
            }
            return;
        }

        if (this.demand <= 0) {
            return;
        }
        this.demand--;

        this.readInProgress = true;

        this.vertx.<ReadResult>executeBlocking(() -> {
            if (this.iter.hasNext()) {
                final Object next = this.iter.next();
                if (next == null) {
                    throw new NullPointerException("Stream contains null.");
                }
                return new ReadResult(next.toString(), true);
            } else {
                return new ReadResult(this.foot, false);
            }
        }, true /* ordered */, asyncResult -> {
            if (asyncResult.failed()) {
                this.doHandleException(asyncResult.cause());
                return;
            }
            final ReadResult result = asyncResult.result();
            this.buffer.appendString(result.toString());
            if ((!result.hasNext()) || this.buffer.length() > 60000) {
                this.doHandle();
                if (!result.hasNext()) {
                    this.doHandleEnd();
                    this.isStreamEnded = true;
                    return;
                }
            }
            this.scheduleNextRead();
        });
    }

    private void scheduleNextRead() {
        synchronized (this) {
            if (this.demand > 0) {
                this.context.runOnContext(nothing -> this.readFromStreamAndPushToBufferAndDoHandleIfRequired());
            }
        }
    }

    private void doHandle() {
        synchronized (this) {
            if (this.handler != null) {
                this.requireRunningInEqualVertxContext();
                this.handler.handle(this.buffer);
                this.buffer = Buffer.buffer(65536);
            }
            this.readInProgress = false;
        }
    }

    private void doHandleEnd() {
        synchronized (this) {
            if (this.endHandler != null) {
                this.requireRunningInEqualVertxContext();
                this.endHandler.handle(null);
            }
        }
    }

    private void doHandleException(final Throwable ex) {
        if (this.exceptionHandler != null) {
            this.exceptionHandler.handle(ex);
        } else {
            ex.printStackTrace();
        }
    }

    private void handleExceptionLazily(final Throwable ex) {
        this.context.runOnContext(nothing -> this.doHandleException(ex));
    }

    private void requireRunningInEqualVertxContext() {
        final Context currentContext = this.vertx.getOrCreateContext();
        if (!this.context.equals(currentContext)) {
            throw new IllegalStateException(
                    "StreamReadStream must run in the same Vert.x context in which it was created. "
                    + this.context + " is expected, but " + currentContext);
        }
    }

    private final Vertx vertx;
    private final Context context;

    private final Stream<?> stream;
    private final Iterator<?> iter;

    private final String head;
    private final String foot;

    private Buffer buffer;

    private Handler<Throwable> exceptionHandler;

    private Handler<Buffer> handler;

    private Handler<Void> endHandler;

    private boolean readInProgress;
    private boolean isStreamEnded;

    private long demand;
}
