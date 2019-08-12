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

import com.coxautodev.graphql.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class CommentResolver implements GraphQLResolver<Comment> {

  @Autowired
  AuthorDao authorDao;

  public CompletableFuture<Post> getPost(Comment comment, DataFetchingEnvironment env) {
    DataLoader<Integer, Post> post = env.getDataLoader("post");
    return post.load(comment.getPostId());
  }

  public Author getAuthor(Comment comment, DataFetchingEnvironment env) {
    return authorDao.findById(comment.getAuthorId(), env);
  }
}
