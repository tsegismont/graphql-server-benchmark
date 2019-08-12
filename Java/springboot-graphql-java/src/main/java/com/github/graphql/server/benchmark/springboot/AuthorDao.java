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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.GraphQLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

@Component
public class AuthorDao {

  @Autowired
  WebClient.Builder webClientBuilder;

  @Autowired
  Environment env;

  WebClient webClient;

  @PostConstruct
  void init() {
    this.webClient = webClientBuilder.baseUrl(env.getProperty("backend.baseUrl")).build();
  }

  public Author findById(int authorId, DataFetchingEnvironment env) {
    LoadingCache<Integer, Author> authorCache = getAuthorCache(env);
    return authorCache.get(authorId);
  }

  private LoadingCache<Integer, Author> getAuthorCache(DataFetchingEnvironment env) {
    GraphQLContext context = env.getContext();
    HttpServletRequest httpServletRequest = context.getHttpServletRequest().get();
    LoadingCache<Integer, Author> authorCache = (LoadingCache<Integer, Author>) httpServletRequest.getAttribute("authorCache");
    if (authorCache == null) {
      authorCache = Caffeine.newBuilder()
        .build(key -> webClient.get().uri("/author/{id}", key).retrieve().bodyToMono(Author.class).block());
      httpServletRequest.setAttribute("authorCache", authorCache);
    }
    return authorCache;
  }
}
