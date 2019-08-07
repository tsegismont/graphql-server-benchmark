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

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@Component
public class CommentDao {

  @Autowired
  JdbcTemplate jdbcTemplate;

  public List<Comment> findByAuthorId(Integer authorId) {
    return jdbcTemplate.query(
      "select * from comments where author_id = ?",
      ps -> ps.setInt(1, authorId),
      this::toComment
    );
  }

  public Map<Integer, List<Comment>> findComments(Set<Integer> keys) {
    Integer[] array = keys.toArray(new Integer[0]);
    return jdbcTemplate.query(
      "select * from comments where post_id = any(?)",
      ps -> ps.setArray(1, ps.getConnection().createArrayOf(JDBCType.INTEGER.getName(), array)),
      this::toComment
    ).stream().collect(groupingBy(Comment::getPostId));
  }

  private Comment toComment(ResultSet rs, int idx) throws SQLException {
    return new Comment(rs.getInt("post_id"), rs.getInt("author_id"), rs.getString("content"));
  }
}
