function report(filename, summary, latency, requests)
   -- check file existence
   local f = io.open(filename, "r")
   if f~=nil then
       io.close(f)
       f = io.open(filename, "a")
   else
       f = io.open(filename, "w")
       f:write("requests,duration,errors,min,99%,99.9%,99.99%,99.999%,max\n")
   end

   local errors = summary.errors.connect + summary.errors.read + summary.errors.write + summary.errors.status + summary.errors.timeout

   f:write(summary.requests)
   f:write(",")
   f:write(summary.duration)
   f:write(",")
   f:write(errors)
   f:write(",")
   f:write(latency.min)
   f:write(",")
   f:write(latency:percentile(99))
   f:write(",")
   f:write(latency:percentile(99.9))
   f:write(",")
   f:write(latency:percentile(99.99))
   f:write(",")
   f:write(latency:percentile(99.999))
   f:write(",")
   f:write(latency.max)
   f:write("\n")
   f:close()
end
