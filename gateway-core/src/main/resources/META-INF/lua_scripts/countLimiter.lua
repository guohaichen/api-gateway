-- 没用classpath读取，io操作，写在了类中，供快速参考
local count = redis.call("incr",KEYS[1])
if count == 1 then
    redis.call('expire',KEYS[1],ARGV[2])
end
if count > tonumber(ARGV[1]) then
    return 0
end
return 1