DROP TABLE IF EXISTS posts;
CREATE TABLE posts
(
    id        INT
        CONSTRAINT posts_pk PRIMARY KEY,
    author_id INT     NOT NULL,
    title     VARCHAR NOT NULL,
    content   VARCHAR NOT NULL
);

DROP TABLE IF EXISTS comments;
CREATE TABLE comments
(
    id        INT
        CONSTRAINT comments_pk PRIMARY KEY,
    post_id   INT     NOT NULL,
    author_id INT     NOT NULL,
    content   VARCHAR NOT NULL
);
