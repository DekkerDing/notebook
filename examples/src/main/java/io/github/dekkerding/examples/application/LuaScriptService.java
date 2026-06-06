package io.github.dekkerding.examples.application;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lua脚本服务（简化版）
 */
@Service
public class LuaScriptService {

    private static final Logger log = LoggerFactory.getLogger(LuaScriptService.class);

    @Autowired(required = false)
    private RedissonClient redisson;

    public Object execute(String luaScript, List<?> keys, List<?> values) {
        if (redisson == null) return null;
        try {
            RScript script = redisson.getScript();
            @SuppressWarnings("unchecked")
            List<Object> objectKeys = (List<Object>) (List<?>) keys;
            return script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.VALUE,
                    objectKeys,
                    values.toArray()
            );
        } catch (Exception e) {
            log.error("执行Lua脚本失败", e);
            return null;
        }
    }

    public Long executeForLong(String luaScript, List<?> keys, List<?> values) {
        if (redisson == null) return null;
        try {
            RScript script = redisson.getScript();
            @SuppressWarnings("unchecked")
            List<Object> objectKeys = (List<Object>) (List<?>) keys;
            return script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    objectKeys,
                    values.toArray()
            );
        } catch (Exception e) {
            log.error("执行Lua脚本失败", e);
            return null;
        }
    }

    public String loadScript(String luaScript) {
        if (redisson == null) return null;
        try {
            RScript script = redisson.getScript();
            return script.scriptLoad(luaScript);
        } catch (Exception e) {
            log.error("加载Lua脚本失败", e);
            return null;
        }
    }

    public boolean executeLockScript(String lockKey, String lockValue, long ttlMs) {
        String script = "local key = KEYS[1]\n" +
                "local value = ARGV[1]\n" +
                "local ttl = ARGV[2]\n" +
                "local locked = redis.call('SET', key, value, 'NX', 'PX', ttl)\n" +
                "if locked then\n" +
                "    return 1\n" +
                "else\n" +
                "    return 0\n" +
                "end";

        Long result = executeForLong(script,
                Collections.singletonList(lockKey),
                java.util.Arrays.asList(lockValue, String.valueOf(ttlMs))
        );
        return result != null && result == 1;
    }
}
