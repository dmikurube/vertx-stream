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

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.Arguments;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author stw, antimist
 */
public class AsyncInputStream implements ReadStream<Buffer> {
    /**
     * Create a new Async InputStream that can we used with a Pump
     *
     * @param in
     */
    public AsyncInputStream(Vertx vertx, Context context, InputStream in) {
        this.vertx = vertx;
        this.context = context;
        this.ch = Channels.newChannel(in);
        this.queue = new InboundBuffer<>(context, 0);
        queue.handler(buff -> {
            if (buff.length() > 0) {
                handleData(buff);
            } else {
                handleEnd();
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
        Objects.requireNonNull(handler);
        synchronized (this) {
            this.requireOpen();
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
        Objects.requireNonNull(handler);
        synchronized (this) {
            this.requireOpen();
            this.handler = handler;
            if (this.handler != null && !this.isClosed) {
                this.doRead();
            } else {
                queue.clear();
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
            this.requireOpen();
            queue.pause();
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
            this.requireOpen();
            if (!this.isClosed) {
                queue.resume();
            }
        }
        return this;
    }

    @Override
    public ReadStream<Buffer> fetch(long amount) {
        queue.fetch(amount);
        return this;
    }

    /*
     * (non-Javadoc)
     * @see io.vertx.core.streams.ReadStream#endHandler(io.vertx.core.Handler)
     */
    @Override
    public AsyncInputStream endHandler(final Handler<Void> endHandler) {
        Objects.requireNonNull(endHandler);
        synchronized (this) {
            this.requireOpen();
            this.endHandler = endHandler;
        }
        return this;
    }

    private void doRead() {
        this.requireOpen();
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
                    handleException(ar.cause());
                }
            });
        }
    }

    private void doRead(Buffer writeBuff, int offset, ByteBuffer buff, long position, Handler<AsyncResult<Buffer>> handler) {

        // ReadableByteChannel doesn't have a completion handler, so we wrap it into
        // an executeBlocking and use the future there
        vertx.executeBlocking(future -> {
            try {
                Integer bytesRead = ch.read(buff);
                future.complete(bytesRead);
            } catch (Exception e) {
                log.error("", e);
                future.fail(e);
            }

        }, res -> {

            if (res.failed()) {
                context.runOnContext((v) -> handler.handle(Future.failedFuture(res.cause())));
            } else {
                // Do the completed check
                Integer bytesRead = (Integer) res.result();
                if (bytesRead == -1) {
                    //End of file
                    context.runOnContext((v) -> {
                        buff.flip();
                        writeBuff.setBytes(offset, buff);
                        buff.compact();
                        handler.handle(Future.succeededFuture(writeBuff));
                    });
                } else if (buff.hasRemaining()) {
                    long pos = position;
                    pos += bytesRead;
                    // resubmit
                    doRead(writeBuff, offset, buff, pos, handler);
                } else {
                    // It's been fully written

                    context.runOnContext((v) -> {
                        buff.flip();
                        writeBuff.setBytes(offset, buff);
                        buff.compact();
                        handler.handle(Future.succeededFuture(writeBuff));
                    });
                }
            }
        });
    }

    private void handleData(Buffer buff) {
        Handler<Buffer> handler;
        synchronized (this) {
            handler = this.handler;
        }
        if (handler != null) {
            checkContext();
            handler.handle(buff);
        }
    }

    private synchronized void handleEnd() {
        Handler<Void> endHandler;
        synchronized (this) {
            handler = null;
            endHandler = this.endHandler;
        }
        if (endHandler != null) {
            checkContext();
            endHandler.handle(null);
        }
    }

    private void handleException(Throwable t) {
        if (exceptionHandler != null && t instanceof Exception) {
            exceptionHandler.handle(t);
        } else {
            log.error("Unhandled exception", t);

        }
    }

    private void checkContext() {
        if (!vertx.getOrCreateContext().equals(context)) {
            throw new IllegalStateException("AsyncInputStream must only be used in the context that created it, expected: " + this.context
                + " actual " + vertx.getOrCreateContext());
        }
    }

    private void requireOpen() {
        if (this.isClosed) {
            throw new IllegalStateException("Inputstream is closed");
        }
    }

    public void close(Handler<AsyncResult<Void>> handler) {
        closeInternal(handler);
    }

    private void close() {
        closeInternal(null);
    }

    private synchronized void closeInternal(Handler<AsyncResult<Void>> handler) {
        this.requireOpen();
        this.isClosed = true;
        doClose(handler);
    }

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

    public static final int DEFAULT_READ_BUFFER_SIZE = 8192;

    private static final Logger log = LoggerFactory.getLogger(AsyncInputStream.class);

    // Based on the inputStream with the real data
    private final ReadableByteChannel ch;
    private final Vertx vertx;
    private final Context context;

    private boolean isClosed;
    private boolean readInProgress;

    private Handler<Buffer> handler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;
    private final InboundBuffer<Buffer> queue;

    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
    private long readPos;
}
