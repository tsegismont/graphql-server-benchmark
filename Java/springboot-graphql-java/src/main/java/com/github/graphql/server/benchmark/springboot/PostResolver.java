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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class PostResolver implements GraphQLResolver<Post> {

  @Autowired
  AuthorDao authorDao;

  public Author getAuthor(Post post, DataFetchingEnvironment env) {
    return authorDao.findById(post.getAuthorId(), env);
  }

  public CompletableFuture<List<Comment>> getComments(Post post, DataFetchingEnvironment env) throws ExecutionException, InterruptedException {
    DataLoader<Integer, List<Comment>> comment = env.getDataLoader("comment");
    return comment.load(post.getId());
  }
}
