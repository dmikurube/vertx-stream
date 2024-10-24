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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestHandlerVerticle extends AbstractVerticle {
    private HttpRequestHandlerVerticle() {
    }

    private static class VerticleSupplier implements Supplier<Verticle> {
        @Override
        public HttpRequestHandlerVerticle get() {
            return new HttpRequestHandlerVerticle();
        }
    }

    public static Supplier<Verticle> supplier() {
        return new VerticleSupplier();
    }

    @Override
    public void start(final Promise<Void> startPromise) {
        final Vertx vertx = this.getVertx();
        final EventBus eventBus = vertx.eventBus();

        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(LoggerHandler.create());
        router.getWithRegex("\\/(?<repoPath>(?:[0-9a-zA-Z_]+/)*[0-9a-zA-Z_]+)")
                .handler(context -> {
                    final HttpServerRequest request = context.request();
                    final HttpServerResponse response = context.response();

                    final String path = context.pathParam("repoPath");
                    final MultiMap queryParams = context.queryParams(StandardCharsets.UTF_8);

                    response.send(new StreamReadStream(vertx, Stream.of("foo", path, "bar"), "head", "foot"), asyncResult -> {
                        if (asyncResult.failed()) {
                            response.setStatusCode(400);
                            response.end();
                            return;
                        }
                        response.setStatusCode(200);
                        response.end();
                    });
                });

        final HttpServer server = vertx.createHttpServer();
        server.requestHandler(router);

        server.listen(8080, http -> {
            if (http.succeeded()) {
                logger.info("Started the HTTP server successfully at port 8080.");
                startPromise.complete();
            } else {
                logger.error("Failed to start the HTTP server.", http.cause());
                startPromise.fail(http.cause());
            }
        });
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandlerVerticle.class);
}
