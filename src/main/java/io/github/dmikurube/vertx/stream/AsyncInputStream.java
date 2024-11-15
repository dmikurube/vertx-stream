/*
 * This file is based on a copy from Stephan H. Wissel's AsyncInputStream with modifications.
 *
 * https://gist.github.com/Stwissel/a7f8ce79785afd49eb2ced69b56335de
 */

// ==========================================================================
// Copyright (C) 2017-2024 NotesSensei ( https://www.wissel.net/ )
//                            All rights reserved.
// ==========================================================================
// Licensed under the  Apache License, Version 2.0  (the "License").  You may
// not use this file except in compliance with the License.  You may obtain a
// copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
//
// Unless  required  by applicable  law or  agreed  to  in writing,  software
// distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT
// WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the
// License for the  specific language  governing permissions  and limitations
// under the License.
// ==========================================================================

package io.github.dmikurube.vertx.stream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author stw, antimist
 */
public class AsyncInputStream implements ReadStream<Buffer> {
    /**
     * Create a new Async InputStream that can we used with a Pump
     */
    public AsyncInputStream(final Vertx vertx, final Context savedContext, final InputStream in) {
        this.vertx = vertx;
        this.savedContext = savedContext;
        this.ch = Channels.newChannel(in);
        this.queue = new InboundBuffer<>(savedContext, 0);
        queue.handler(buff -> {
            if (buff.length() > 0) {
                this.doHandle(buff);
            } else {
                this.doHandleEnd();
            }
        });
        queue.drainHandler(v -> {
            doRead();
        });
    }

    /*
     * (non-Javadoc)
     * @see
     * io.vertx.core.streams.ReadStream#exceptionHandler(io.vertx.core.Handler)
     */
    @Override
    public AsyncInputStream exceptionHandler(final Handler<Throwable> handler) {
        synchronized (this) {
            this.requireStreamIsOpen();
            this.exceptionHandler = handler;
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.streams.ReadStream#handler(io.vertx.core.Handler)
     */
    @Override
    public AsyncInputStream handler(final Handler<Buffer> handler) {
        synchronized (this) {
            this.requireStreamIsOpen();
            this.handler = handler;
            if (this.handler != null && !this.closed) {
                this.doRead();
            } else {
                this.queue.clear();
            }
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.streams.ReadStream#pause()
     */
    @Override
    public AsyncInputStream pause() {
        synchronized (this) {
            this.requireStreamIsOpen();
            this.queue.pause();
        }
        return this;
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.streams.ReadStream#resume()
     */
    @Override
    public AsyncInputStream resume() {
        synchronized (this) {
            this.requireStreamIsOpen();
            if (!this.closed) {
                this.queue.resume();
            }
        }
        return this;
    }

    @Override
    public AsyncInputStream fetch(final long amount) {
        this.queue.fetch(amount);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.streams.ReadStream#endHandler(io.vertx.core.Handler)
     */
    @Override
    public AsyncInputStream endHandler(final Handler<Void> endHandler) {
        synchronized (this) {
            this.requireStreamIsOpen();
            this.endHandler = endHandler;
        }
        return this;
    }

    private void requireStreamIsOpen() {
        if (this.closed) {
            throw new IllegalStateException("Inputstream is closed");
        }
    }

    private void requireRunningInEqualVertxContext() {
        final Context currentContext = this.vertx.getOrCreateContext();
        if (!this.savedContext.equals(currentContext)) {
            throw new IllegalStateException(
                    "AsyncInputStream must run in the same Vert.x context in which it was created. "
                    + this.savedContext + " is expected, but " + currentContext);
        }
    }

    private void doRead() {
        this.requireStreamIsOpen();
        doRead(ByteBuffer.allocate(readBufferSize));
    }

    private synchronized void doRead(ByteBuffer bb) {
        if (!readInProgress) {
            readInProgress = true;
            Buffer buff = Buffer.buffer(readBufferSize);
            doRead(buff, 0, bb, readPos, ar -> {
                if (ar.succeeded()) {
                    readInProgress = false;
                    Buffer buffer = ar.result();
                    readPos += buffer.length();
                    // Empty buffer represents end of file
                    if (queue.write(buffer) && buffer.length() > 0) {
                        doRead(bb);
                    }
                } else {
                    this.doHandleException(ar.cause());
                }
            });
        }
    }

    private void doRead(
            final Buffer destinationBuffer,
            final int offsetInDestinationBuffer,
            final ByteBuffer sourceByteBuffer,
            final long position,
            final Handler<AsyncResult<Buffer>> resultHandler) {
        // ReadableByteChannel doesn't have a completion handler, so we wrap it into
        // an executeBlocking and use the future there
        this.vertx.<Integer>executeBlocking(() -> {
            try {
                final Integer bytesRead = this.ch.read(sourceByteBuffer);
                return bytesRead;
            } catch (final Exception ex) {
                logger.error("Failure in reading.", ex);
                throw new RuntimeException(ex);
            }
        }, true /* ordered */, asyncResult -> {
            if (asyncResult.failed()) {
                this.savedContext.runOnContext(ignored -> resultHandler.handle(Future.failedFuture(asyncResult.cause())));
            } else {
                final Integer bytesRead = (Integer) asyncResult.result();
                if (bytesRead == -1) {  // End of the source stream
                    this.savedContext.runOnContext(ignored -> {
                        sourceByteBuffer.flip();
                        destinationBuffer.setBytes(offsetInDestinationBuffer, sourceByteBuffer);
                        sourceByteBuffer.compact();
                        resultHandler.handle(Future.succeededFuture(destinationBuffer));
                    });
                } else {
                    if (sourceByteBuffer.hasRemaining()) {
                        long pos = position;
                        pos += bytesRead;
                        // resubmit
                        doRead(destinationBuffer, offsetInDestinationBuffer, sourceByteBuffer, pos, resultHandler);
                    } else {
                        // It's been fully written
                        this.savedContext.runOnContext(ignored -> {
                            sourceByteBuffer.flip();
                            destinationBuffer.setBytes(offsetInDestinationBuffer, sourceByteBuffer);
                            sourceByteBuffer.compact();
                            resultHandler.handle(Future.succeededFuture(destinationBuffer));
                        });
                    }
                }
            }
        });
    }

    private void doHandle(final Buffer buffer) {
        final Handler<Buffer> handler;
        synchronized (this) {
            handler = this.handler;
        }
        if (handler != null) {
            this.requireRunningInEqualVertxContext();
            handler.handle(buffer);
        }
    }

    private void doHandleEnd() {
        final Handler<Void> endHandler;
        synchronized (this) {
            this.handler = null;
            endHandler = this.endHandler;
        }
        if (endHandler != null) {
            this.requireRunningInEqualVertxContext();
            endHandler.handle(null);
        }
    }

    private void doHandleException(final Throwable throwable) {
        if (this.exceptionHandler != null && throwable instanceof Exception) {
            this.exceptionHandler.handle(throwable);
        } else {
            this.logger.error("Unhandled exception.", throwable);
        }
    }

    /*
    public synchronized AsyncInputStream read(Buffer buffer, int offset, long position, int length,
                                              Handler<AsyncResult<Buffer>> handler) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(handler, "handler");
        Arguments.require(offset >= 0, "offset must be >= 0");
        Arguments.require(position >= 0, "position must be >= 0");
        Arguments.require(length >= 0, "length must be >= 0");
        this.requireStreamIsOpen();
        ByteBuffer bb = ByteBuffer.allocate(length);
        doRead(buffer, offset, bb, position, handler);
        return this;
    }
    */

    /*
    public void close() {
        closeInternal(null);
    }
    */

    /*
    public void close(Handler<AsyncResult<Void>> handler) {
        closeInternal(handler);
    }
    */

    /*
    private synchronized void closeInternal(Handler<AsyncResult<Void>> handler) {
        this.requireStreamIsOpen();
        closed = true;
        doClose(handler);
    }
    */

    /*
    private void doClose(Handler<AsyncResult<Void>> handler) {
        try {
            ch.close();
            if (handler != null) {
                this.vertx.runOnContext(v -> handler.handle(Future.succeededFuture()));
            }
        } catch (IOException e) {
            if (handler != null) {
                this.vertx.runOnContext(v -> handler.handle(Future.failedFuture(e)));
            }
        }
    }
    */

    private static final Logger logger = LoggerFactory.getLogger(AsyncInputStream.class);

    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;

    // Based on the inputStream with the real data
    private final ReadableByteChannel ch;
    private final Vertx vertx;
    private final Context savedContext;

    private final InboundBuffer<Buffer> queue;

    private boolean closed;
    private boolean readInProgress;

    private Handler<Buffer> handler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;

    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
    private long readPos;
}
