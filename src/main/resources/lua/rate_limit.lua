-- KEYS[1] 限流 key
-- ARGV[1] 当前毫秒时间戳
-- ARGV[2] 窗口毫秒数
-- ARGV[3] 窗口内最大请求数
-- ARGV[4] 本次请求的唯一标识
local now    = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit  = tonumber(ARGV[3])

-- 清理窗口外的历史请求
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - window)

-- 统计窗口内请求数
local current = redis.call('ZCARD', KEYS[1])
if current >= limit then
    return 0
end

-- 记录本次请求并设置兜底过期时间
redis.call('ZADD', KEYS[1], now, ARGV[4])
redis.call('PEXPIRE', KEYS[1], window + 1000)
return 1
