/*
 * Copyright 2020 Red Hat, Inc.
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

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import io.vertx.core.Context;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import io.vertx.ext.web.handler.graphql.schema.VertxPropertyDataFetcher;
import org.dataloader.DataLoader;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class AppLauncher extends Launcher {

  public static void main(String[] args) {
    new AppLauncher().dispatch(args);
  }

  @Override
  public void afterStartingVertx(Vertx vertx) {
    String schema = vertx.fileSystem().readFileBlocking("blog.graphqls").toString();

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = newRuntimeWiring()
      .wiringFactory(new WiringFactory() {
        @Override
        public DataFetcher<Object> getDefaultDataFetcher(FieldWiringEnvironment environment) {
          return VertxPropertyDataFetcher.create(environment.getFieldDefinition().getName());
        }
      })
      .type("Query", builder -> {
        return builder
          .dataFetcher("posts", VertxDataFetcher.create(env -> getServerVerticle(env).findPosts(null, env), this::contextForEnv))
          .dataFetcher("author", VertxDataFetcher.create(env -> getServerVerticle(env).findAuthor(env.getArgument("id"), env), this::contextForEnv));
      }).type("Post", builder -> {
        return builder
          .dataFetcher("author", VertxDataFetcher.create(env -> {
            JsonObject post = env.getSource();
            return getServerVerticle(env).findAuthor(post.getInteger("author_id"), env);
          }, this::contextForEnv))
          .dataFetcher("comments", env -> {
            JsonObject post = env.getSource();
            DataLoader<Integer, JsonArray> comment = env.getDataLoader("comment");
            return comment.load(post.getInteger("id"), env);
          });
      }).type("Author", builder -> {
        return builder
          .dataFetcher("posts", VertxDataFetcher.create(env -> {
            JsonObject author = env.getSource();
            return getServerVerticle(env).findPosts(author.getInteger("id"), env);
          }, this::contextForEnv))
          .dataFetcher("comments", VertxDataFetcher.create(env -> {
            JsonObject author = env.getSource();
            return getServerVerticle(env).findComments(author.getInteger("id"), env);
          }, this::contextForEnv));
      }).type("Comment", builder -> {
        return builder
          .dataFetcher("author", VertxDataFetcher.create(env -> {
            JsonObject comment = env.getSource();
            return getServerVerticle(env).findAuthor(comment.getInteger("author_id"), env);
          }, this::contextForEnv))
          .dataFetcher("post", env -> {
            JsonObject comment = env.getSource();
            DataLoader<Integer, JsonObject> post = env.getDataLoader("post");
            return post.load(comment.getInteger("post_id"), env);
          });
      })
      .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

    ServerVerticle.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
  }

  private Context contextForEnv(DataFetchingEnvironment env) {
    ServerVerticle serverVerticle = getServerVerticle(env);
    return getServerVerticle(env).getContext();
  }

  private ServerVerticle getServerVerticle(DataFetchingEnvironment env) {
    RoutingContext rc = env.getContext();
    return rc.get(ServerVerticle.class.getName());
  }
}
