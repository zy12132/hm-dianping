--判断锁中的线程标识是否与当前线程标识一致
if (ARGV[1] == redis.call('get', KEYS[1])) then
    --释放锁 del key
    return redis.call('del', KEYS[1])
end
return 0