package io.github.dekkerding.examples.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试配置类
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@TestConfiguration
@ComponentScan(basePackages = {
        "io.github.dekkerding.examples.infrastructure",
        "io.github.dekkerding.examples.domain",
        "io.github.dekkerding.examples.testing"
})
public class TestConfig {
}
