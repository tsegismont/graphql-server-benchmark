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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.GraphQL;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import io.reactiverse.pgclient.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
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
import io.vertx.ext.web.handler.graphql.GraphiQLOptions;
import io.vertx.ext.web.handler.graphql.VertxPropertyDataFetcher;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collector;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static java.util.stream.Collectors.*;

public class ServerVerticle extends AbstractVerticle {

  private Executor contextExecutor;
  private WebClient webClient;
  private PgPool pgClient;

  @Override
  public void start() {
    Context context = vertx.getOrCreateContext();
    contextExecutor = cmd -> context.runOnContext(v -> cmd.run());

    JsonObject config = config();
    int port = config.getInteger("port", 8080);

    setupWebClient(config);
    setupPgClient(config);

    GraphQL graphQL = setupGraphQL();
    GraphQLHandlerOptions options = new GraphQLHandlerOptions()
      .setGraphiQLOptions(new GraphiQLOptions().setEnabled(true));
    GraphQLHandler graphQLHandler = GraphQLHandler.create(graphQL, options)
      .dataLoaderRegistry(rc -> {
        DataLoader<Integer, JsonArray> commentDataLoader = DataLoader.newMappedDataLoader((keys, env) -> {
          return toCompletableFuture(findComments(keys, env));
        });
        DataLoader<Integer, JsonObject> postDataLoader = DataLoader.newMappedDataLoader((keys, env) -> {
          return toCompletableFuture(findPosts(keys, env));
        });
        return new DataLoaderRegistry()
          .register("comment", commentDataLoader)
          .register("post", postDataLoader);
      });

    Router router = Router.router(vertx);
    router.route("/graphql").handler(graphQLHandler);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port);
  }

  private void setupWebClient(JsonObject config) {
    JsonObject backend = config.getJsonObject("backend", new JsonObject());
    String backendHost = backend.getString("host", "localhost");
    int backendPort = backend.getInteger("port", 8181);

    WebClientOptions webClientOptions = new WebClientOptions()
      .setDefaultHost(backendHost)
      .setDefaultPort(backendPort);
    webClient = WebClient.create(vertx, webClientOptions);
  }

  private void setupPgClient(JsonObject config) {
    JsonObject postgres = config.getJsonObject("postgres", new JsonObject());
    String postgresHost = postgres.getString("host", "localhost");
    int postgresPort = postgres.getInteger("port", 5432);

    PgPoolOptions pgPoolOptions = new PgPoolOptions()
      .setHost(postgresHost)
      .setPort(postgresPort)
      .setUser("graphql")
      .setPassword("graphql")
      .setDatabase("blogdb")
      .setCachePreparedStatements(true);
    pgClient = PgClient.pool(vertx, pgPoolOptions);
  }

  private GraphQL setupGraphQL() {
    String schema = vertx.fileSystem().readFileBlocking("blog.graphqls").toString();

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = newRuntimeWiring()
      .wiringFactory(new WiringFactory() {
        @Override
        public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
          return new VertxPropertyDataFetcher(environment.getFieldDefinition().getName());
        }
      })
      .type("Query", builder -> {
        return builder
          .dataFetcher("posts", env -> toCompletableFuture(findPosts(null, env)))
          .dataFetcher("author", env -> toCompletableFuture(findAuthor(env.getArgument("id"), env)));
      }).type("Post", builder -> {
        return builder
          .dataFetcher("author", env -> {
            JsonObject post = env.getSource();
            return toCompletableFuture(findAuthor(post.getInteger("author_id"), env));
          }).dataFetcher("comments", env -> {
            JsonObject post = env.getSource();
            DataLoader<Integer, JsonArray> comment = env.getDataLoader("comment");
            return comment.load(post.getInteger("id"), env);
          });
      }).type("Author", builder -> {
        return builder
          .dataFetcher("posts", env -> {
            JsonObject author = env.getSource();
            return toCompletableFuture(findPosts(author.getInteger("id"), env));
          }).dataFetcher("comments", env -> {
            JsonObject author = env.getSource();
            return toCompletableFuture(findComments(author.getInteger("id"), env));
          });
      }).type("Comment", builder -> {
        return builder
          .dataFetcher("author", env -> {
            JsonObject comment = env.getSource();
            return toCompletableFuture(findAuthor(comment.getInteger("author_id"), env));
          }).dataFetcher("post", env -> {
            JsonObject comment = env.getSource();
            DataLoader<Integer, JsonObject> post = env.getDataLoader("post");
            return post.load(comment.getInteger("post_id"), env);
          });
      })
      .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    Cache<String, PreparsedDocumentEntry> queryCache = Caffeine.newBuilder().build();

    return GraphQL.newGraphQL(graphQLSchema).preparsedDocumentProvider(queryCache::get)
      .instrumentation(new DataLoaderDispatcherInstrumentation())
      .build();
  }

  private Future<JsonObject> findAuthor(Integer authorId, DataFetchingEnvironment env) {
    Future<JsonObject> future = Future.future();

    AsyncLoadingCache<Integer, JsonObject> authorCache = getAuthorCache(env);
    authorCache.get(authorId).whenComplete((jsonObject, throwable) -> {
      if (throwable == null) {
        future.complete(jsonObject);
      } else {
        future.fail(throwable);
      }
    });

    return future;
  }

  private AsyncLoadingCache<Integer, JsonObject> getAuthorCache(DataFetchingEnvironment env) {
    RoutingContext rc = env.getContext();
    AsyncLoadingCache<Integer, JsonObject> authorCache = rc.get("authorCache");
    if (authorCache == null) {
      authorCache = Caffeine.newBuilder()
        .executor(contextExecutor)
        .buildAsync((key, executor) -> toCompletableFuture(loadAuthor(key)));
      rc.put("authorCache", authorCache);
    }
    return authorCache;
  }

  private Future<JsonObject> loadAuthor(Integer authorId) {
    Future<HttpResponse<JsonObject>> future = Future.future();

    webClient.get("/author/" + authorId)
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_OK)
      .send(future);

    return future.map(HttpResponse::body);
  }

  private Future<JsonArray> findPosts(Integer postId, DataFetchingEnvironment env) {
    Future<PgResult<JsonArray>> future = Future.future();
    Collector<Row, ?, JsonArray> collector = mapping(this::toPost, collectingAndThen(toList(), JsonArray::new));
    if (postId == null) {
      pgClient.preparedQuery("select * from posts", collector, future);
    } else {
      pgClient.preparedQuery("select * from posts where author_id = $1", Tuple.of(postId), collector, future);
    }
    return future.map(PgResult::value);
  }

  private Future<Map<Integer, JsonObject>> findPosts(Set<Integer> ids, BatchLoaderEnvironment env) {
    Future<PgResult<Map<Integer, JsonObject>>> future = Future.future();
    Collector<Row, ?, Map<Integer, JsonObject>> collector = toMap(row -> row.getInteger("id"), this::toPost);
    pgClient.preparedQuery("select * from posts where id = any($1)", Tuple.of(ids.toArray(new Integer[0])), collector, future);
    return future.map(PgResult::value);
  }

  private JsonObject toPost(Row row) {
    return new JsonObject()
      .put("id", row.getInteger("id"))
      .put("author_id", row.getInteger("author_id"))
      .put("title", row.getString("title"))
      .put("content", row.getString("content"));
  }

  private Future<Map<Integer, JsonArray>> findComments(Set<Integer> postIds, BatchLoaderEnvironment env) {
    Future<PgResult<Map<Integer, JsonArray>>> future = Future.future();
    Collector<Row, ?, Map<Integer, JsonArray>> collector = groupingBy(
      row -> row.getInteger("post_id"),
      mapping(this::toComment, collectingAndThen(toList(), JsonArray::new))
    );
    pgClient.preparedQuery("select * from comments where post_id = any($1)", Tuple.of(postIds.toArray(new Integer[0])), collector, future);
    return future.map(PgResult::value);
  }

  private Future<JsonArray> findComments(Integer authorId, DataFetchingEnvironment env) {
    Future<PgResult<JsonArray>> future = Future.future();
    Collector<Row, ?, JsonArray> collector = mapping(this::toComment, collectingAndThen(toList(), JsonArray::new));
    pgClient.preparedQuery("select * from comments where author_id = $1", Tuple.of(authorId), collector, future);
    return future.map(PgResult::value);
  }

  private JsonObject toComment(Row row) {
    return new JsonObject()
      .put("post_id", row.getInteger("post_id"))
      .put("author_id", row.getInteger("author_id"))
      .put("content", row.getString("content"));
  }

  private <T> CompletableFuture<T> toCompletableFuture(Future<T> future) {
    CompletableFuture<T> cf = new CompletableFuture<>();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        cf.complete(ar.result());
      } else {
        cf.completeExceptionally(ar.cause());
      }
    });
    return cf;
  }
}
