wrk.method = "POST"
wrk.headers["Accept"] = "application/json"
wrk.headers["Content-Type"] = "application/json"

counter = 1

request = function()
   body = [[{"query":"query ($authorId: Int!) {\n  author(id: $authorId) {\n    firstName\n    lastName\n    bio\n    posts {\n      title\n      content\n    }\n    comments {\n      post {\n       title\n      }\n      content\n    }\n  }\n}","variables":{"authorId": ]]..counter..[[}}]]
   counter = counter < 10 and (counter + 1) or 1
   return wrk.format(nil, nil, nil, body)
end
