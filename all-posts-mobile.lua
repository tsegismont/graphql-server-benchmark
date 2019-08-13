wrk.method = "POST"
wrk.headers["Accept"] = "application/json"
wrk.headers["Content-Type"] = "application/json"
wrk.body   = [[{"query":"{\n  posts {\n    author {\n      firstName\n      lastName\n    }\n    title\n    content\n  }\n}"}]]
