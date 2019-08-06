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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class PostDao {

  @Autowired
  JdbcTemplate jdbcTemplate;

  public List<Post> findPosts() {
    return jdbcTemplate.query("select * from posts", this::toPost);
  }

  private Post toPost(ResultSet rs, int idx) throws SQLException {
    return new Post(rs.getInt("id"), rs.getInt("author_id"), rs.getString("title"), rs.getString("content"));
  }
}
