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

public class Post {

  private int id;
  private int authorId;
  private String title;
  private String content;

  public Post(int id, int authorId, String title, String content) {
    this.id = id;
    this.authorId = authorId;
    this.title = title;
    this.content = content;
  }

  public int getId() {
    return id;
  }

  public int getAuthorId() {
    return authorId;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }
}
