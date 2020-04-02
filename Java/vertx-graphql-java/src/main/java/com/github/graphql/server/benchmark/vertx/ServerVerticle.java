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

package com.github.graphql.server.benchmark.vertx;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.dataloader.VertxMappedBatchLoader;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;

import static java.util.stream.Collectors.*;

public class ServerVerticle extends AbstractVerticle {

  static GraphQL graphQL;

  private ContextInternal context;
  private WebClient webClient;
  private PgPool pgClient;

  @Override
  public void start() {
    context = (ContextInternal) super.context;

    JsonObject config = config();
    int port = config.getInteger("port", 8080);

    setupWebClient(config);
    setupPgClient(config);

    GraphQLHandler graphQLHandler = GraphQLHandler.create(graphQL, new GraphQLHandlerOptions())
      .dataLoaderRegistry(rc -> {
        DataLoader<Integer, JsonArray> commentDataLoader = DataLoader.newMappedDataLoader(
          VertxMappedBatchLoader.create(this::findComments, env -> context)
        );
        DataLoader<Integer, JsonObject> postDataLoader = DataLoader.newMappedDataLoader(
          VertxMappedBatchLoader.create(this::findPosts, env -> context)
        );
        return new DataLoaderRegistry()
          .register("comment", commentDataLoader)
          .register("post", postDataLoader);
      });

    Router router = Router.router(vertx);
    router.route("/graphql").handler(this::bindVerticleInstance).handler(graphQLHandler);
    router.get("/graphiql/*").handler(GraphiQLHandler.create());

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port);
  }

  private void bindVerticleInstance(RoutingContext rc) {
    rc.put(ServerVerticle.class.getName(), this);
    rc.next();
  }

  private void setupWebClient(JsonObject config) {
    JsonObject backend = config.getJsonObject("backend", new JsonObject());
    String backendHost = System.getenv().getOrDefault("BACKEND_HOST", backend.getString("host", "localhost"));
    int backendPort = backend.getInteger("port", 8181);
    int maxSize = backend.getInteger("poolSize", 4);

    WebClientOptions webClientOptions = new WebClientOptions()
      .setDefaultHost(backendHost)
      .setDefaultPort(backendPort)
      .setMaxPoolSize(maxSize)
      .setPipelining(true);
    webClient = WebClient.create(vertx, webClientOptions);
  }

  private void setupPgClient(JsonObject config) {
    JsonObject postgres = config.getJsonObject("postgres", new JsonObject());
    String postgresHost = System.getenv().getOrDefault("POSTGRES_HOST", postgres.getString("host", "localhost"));
    int postgresPort = postgres.getInteger("port", 5432);
    int maxSize = postgres.getInteger("poolSize", 4);

    PgConnectOptions pgConnectOptions = new PgConnectOptions()
      .setHost(postgresHost)
      .setPort(postgresPort)
      .setUser("graphql")
      .setPassword("graphql")
      .setDatabase("blogdb")
      .setCachePreparedStatements(true);
    PoolOptions pgPoolOptions = new PoolOptions()
      .setMaxSize(maxSize);
    pgClient = PgPool.pool(vertx, pgConnectOptions, pgPoolOptions);
  }

  Future<JsonObject> findAuthor(Integer authorId, DataFetchingEnvironment env) {
    AsyncLoadingCache<Integer, JsonObject> authorCache = getAuthorCache(env);
    return Future.fromCompletionStage(authorCache.get(authorId), context);
  }

  private AsyncLoadingCache<Integer, JsonObject> getAuthorCache(DataFetchingEnvironment env) {
    RoutingContext rc = env.getContext();
    AsyncLoadingCache<Integer, JsonObject> authorCache = rc.get("authorCache");
    if (authorCache == null) {
      authorCache = Caffeine.newBuilder()
        .executor(context)
        .buildAsync((key, executor) -> loadAuthor(key));
      rc.put("authorCache", authorCache);
    }
    return authorCache;
  }

  private CompletableFuture<JsonObject> loadAuthor(Integer authorId) {
    return webClient.get("/author/" + authorId)
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_OK)
      .send()
      .map(HttpResponse::body)
      .toCompletionStage()
      .toCompletableFuture();
  }

  Future<JsonArray> findPosts(Integer authorId, DataFetchingEnvironment env) {
    Collector<Row, ?, JsonArray> collector = mapping(this::toPost, collectingAndThen(toList(), JsonArray::new));
    Future<SqlResult<JsonArray>> future;
    if (authorId == null) {
      future = pgClient.preparedQuery("select * from posts")
        .collecting(collector)
        .execute();
    } else {
      future = pgClient.preparedQuery("select * from posts where author_id = $1")
        .collecting(collector)
        .execute(Tuple.of(authorId));
    }
    return future.map(SqlResult::value);
  }

  private Future<Map<Integer, JsonObject>> findPosts(Set<Integer> ids, BatchLoaderEnvironment env) {
    Collector<Row, ?, Map<Integer, JsonObject>> collector = toMap(row -> row.getInteger("id"), this::toPost);
    return pgClient.preparedQuery("select * from posts where id = any($1)")
      .collecting(collector)
      .execute(Tuple.of(ids.toArray(new Integer[0])))
      .map(SqlResult::value);
  }

  private JsonObject toPost(Row row) {
    return new JsonObject()
      .put("id", row.getInteger("id"))
      .put("author_id", row.getInteger("author_id"))
      .put("title", row.getString("title"))
      .put("content", row.getString("content"));
  }

  private Future<Map<Integer, JsonArray>> findComments(Set<Integer> postIds, BatchLoaderEnvironment env) {
    Collector<Row, ?, Map<Integer, JsonArray>> collector = groupingBy(
      row -> row.getInteger("post_id"),
      mapping(this::toComment, collectingAndThen(toList(), JsonArray::new))
    );
    return pgClient.preparedQuery("select * from comments where post_id = any($1)")
      .collecting(collector)
      .execute(Tuple.of(postIds.toArray(new Integer[0])))
      .map(SqlResult::value);
  }

  Future<JsonArray> findComments(Integer authorId, DataFetchingEnvironment env) {
    Collector<Row, ?, JsonArray> collector = mapping(this::toComment, collectingAndThen(toList(), JsonArray::new));
    return pgClient.preparedQuery("select * from comments where author_id = $1")
      .collecting(collector)
      .execute(Tuple.of(authorId))
      .map(SqlResult::value);
  }

  private JsonObject toComment(Row row) {
    return new JsonObject()
      .put("post_id", row.getInteger("post_id"))
      .put("author_id", row.getInteger("author_id"))
      .put("content", row.getString("content"));
  }

  ContextInternal getContext() {
    return context;
  }
}
