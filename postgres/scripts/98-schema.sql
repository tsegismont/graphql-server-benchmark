DROP TABLE IF EXISTS posts;
CREATE TABLE posts
(
    id        INT     NOT NULL,
    author_id INT     NOT NULL,
    title     VARCHAR NOT NULL,
    content   VARCHAR NOT NULL
);

DROP TABLE IF EXISTS comments;
CREATE TABLE comments
(
    post_id   INT     NOT NULL,
    author_id INT     NOT NULL,
    content   VARCHAR NOT NULL
);
