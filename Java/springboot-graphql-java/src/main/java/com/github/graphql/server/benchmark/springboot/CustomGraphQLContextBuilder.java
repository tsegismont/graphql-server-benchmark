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

package com.github.graphql.server.benchmark.springboot;

import graphql.servlet.DefaultGraphQLContextBuilder;
import graphql.servlet.GraphQLContext;
import graphql.servlet.GraphQLContextBuilder;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class CustomGraphQLContextBuilder implements GraphQLContextBuilder {

  @Autowired
  CommentDao commentDao;

  @Autowired
  PostDao postDao;

  @Override
  public GraphQLContext build(HttpServletRequest req, HttpServletResponse response) {
    GraphQLContext context = new DefaultGraphQLContextBuilder().build(req, response);
    context.setDataLoaderRegistry(buildDataLoaderRegistry());
    return context;
  }

  @Override
  public GraphQLContext build() {
    GraphQLContext context = new DefaultGraphQLContextBuilder().build();
    context.setDataLoaderRegistry(buildDataLoaderRegistry());
    return context;
  }

  @Override
  public GraphQLContext build(Session session, HandshakeRequest request) {
    GraphQLContext context = new DefaultGraphQLContextBuilder().build(session, request);
    context.setDataLoaderRegistry(buildDataLoaderRegistry());
    return context;
  }

  private DataLoaderRegistry buildDataLoaderRegistry() {
    DataLoader<Integer, List<Comment>> commentDataLoader = DataLoader.newMappedDataLoader((keys, env) -> {
      return CompletableFuture.completedFuture(commentDao.findComments(keys));
    });
    DataLoader<Integer, Post> postDataLoader = DataLoader.newMappedDataLoader((keys, env) -> {
      return CompletableFuture.completedFuture(postDao.findPosts(keys));
    });
    return new DataLoaderRegistry()
      .register("comment", commentDataLoader)
      .register("post", postDataLoader);
  }
}
