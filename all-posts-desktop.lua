require("report")

wrk.method = "POST"
wrk.headers["Accept"] = "application/json"
wrk.headers["Content-Type"] = "application/json"
wrk.body   = [[{"query":"{\n  posts {\n    author {\n      firstName\n      lastName\n    }\n    title\n    content\n    comments {\n      author {\n        firstName\n        lastName\n      }\n      content\n    }\n  }\n}"}]]

done = function(summary, latency, requests)
   local filename = os.getenv("REPORT_FILE")
   if filename~=nil then
      report(filename, summary, latency, requests)
   end
end
