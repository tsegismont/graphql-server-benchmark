/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.graphql.server.benchmark.backend;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class BackendVerticle extends AbstractVerticle {

  private LocalMap<Integer, Buffer> authors;
  private long delay;
  private int burnCpuMicros;
  private long iterationsForOneMilli;

  @Override
  public void start() throws Exception {
    authors = vertx.sharedData().getLocalMap("authors");

    JsonObject config = config();
    int port = config.getInteger("port", 8181);
    delay = config.getLong("delay", 2L);
    burnCpuMicros = config.getInteger("burnCpuMicros", 500);
    if (burnCpuMicros > 0) {
      iterationsForOneMilli = Utils.calibrateBlackhole();
    }

    loadData();

    Router router = Router.router(vertx);

    Route route = router.get("/author/:id").produces("application/json");
    route.handler(ResponseContentTypeHandler.create());
    route.handler(this::delayResponse);
    route.handler(this::getAuthor);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port);
  }

  private void loadData() {
    Buffer csv = vertx.fileSystem().readFileBlocking("authors.data");
    RecordParser parser = RecordParser.newDelimited("\n");
    parser.handler(line -> {
      String[] split = line.toString().split("\\|");
      JsonObject author = new JsonObject()
        .put("id", Integer.valueOf(split[0].trim()))
        .put("firstName", split[1].trim())
        .put("lastName", split[2].trim())
        .put("bio", split[3].trim());
      authors.put(author.getInteger("id"), author.toBuffer());
    });
    parser.handle(csv);
  }

  private void delayResponse(RoutingContext rc) {
    vertx.setTimer(delay, l -> rc.next());
    if (burnCpuMicros > 0) {
      final long targetDelay = Utils.ONE_MICRO_IN_NANO * burnCpuMicros;
      long numIters = Math.round(targetDelay * 1.0 * iterationsForOneMilli / Utils.ONE_MILLI_IN_NANO);
      Utils.blackholeCpu(numIters);
    }
  }

  private void getAuthor(RoutingContext rc) {
    Integer authorId = Integer.valueOf(rc.pathParam("id"));
    Buffer author = authors.get(authorId);
    if (author == null) {
      rc.response().setStatusCode(404).end();
    } else {
      rc.response().end(author);
    }
  }
}
