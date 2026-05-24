#!/bin/bash

# Redis Stream Kafka功能模块 - 回归测试执行脚本
# 用法: ./run-regression-tests.sh [test_type]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
REDIS_HOST="${REDIS_HOST:-192.168.10.109}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-drk@2025}"

# 测试类型
TEST_TYPE="${1:-all}"

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Redis连接
check_redis() {
    print_info "检查Redis连接..."

    if command -v redis-cli &> /dev/null; then
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping &> /dev/null; then
            print_success "Redis连接正常: $REDIS_HOST:$REDIS_PORT"
            return 0
        else
            print_error "Redis连接失败: $REDIS_HOST:$REDIS_PORT"
            return 1
        fi
    else
        print_warning "redis-cli未安装，跳过连接检查"
        return 0
    fi
}

# 清理测试数据
cleanup_test_data() {
    print_info "清理测试数据..."

    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --scan --pattern "test:*" | \
       xargs -r redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" DEL &> /dev/null; then
        print_success "测试数据清理完成"
    else
        print_warning "测试数据清理失败，继续执行..."
    fi
}

# 运行测试
run_tests() {
    local test_pattern=$1
    local description=$2

    print_info "运行: $description"
    print_info "测试模式: $test_pattern"

    cd "$PROJECT_DIR"

    ./gradlew test --tests "$test_pattern" -i || {
        print_error "$description 测试失败"
        return 1
    }

    print_success "$description 测试完成"
}

# 生成测试报告
generate_report() {
    print_info "生成测试报告..."

    local report_dir="$PROJECT_DIR/build/reports/tests"
    if [ -d "$report_dir" ]; then
        print_info "测试报告位置: $report_dir/index.html"

        if command -v python &> /dev/null; then
            print_info "启动本地测试报告服务器..."
            cd "$report_dir"
            python -m http.server 8080 &> /dev/null &
            local pid=$!
            print_info "测试报告: http://localhost:8080"
            print_info "按Ctrl+C停止服务器"

            # 等待用户中断
            trap "kill $pid 2>/dev/null; exit" INT
            wait
        fi
    else
        print_warning "测试报告未找到"
    fi
}

# 冒烟测试
run_smoke_tests() {
    print_info "========== 冒烟测试 =========="

    local smoke_tests=(
        "PartitionRegressionTest.testHashPartitionConsistency"
        "IdempotentRegressionTest.testBasicIdempotency"
        "PerformanceRegressionTest.testThroughputSmallMessages"
    )

    for test in "${smoke_tests[@]}"; do
        if ! run_tests "$test" "冒烟测试: $test"; then
            print_error "冒烟测试失败，终止执行"
            exit 1
        fi
    done

    print_success "冒烟测试全部通过"
}

# 功能回归测试
run_functional_tests() {
    print_info "========== 功能回归测试 =========="

    run_tests "PartitionRegressionTest" "分区功能测试" || return 1
    run_tests "IdempotentRegressionTest" "幂等性功能测试" || return 1

    print_success "功能回归测试完成"
}

# 性能测试
run_performance_tests() {
    print_info "========== 性能测试 =========="

    run_tests "PerformanceRegressionTest" "性能测试" || return 1

    print_success "性能测试完成"
}

# 完整回归测试
run_all_tests() {
    print_info "========== 完整回归测试 =========="

    check_redis || exit 1
    cleanup_test_data

    run_smoke_tests
    run_functional_tests
    run_performance_tests

    print_success "完整回归测试完成"
}

# 显示帮助
show_help() {
    cat << EOF
Redis Stream Kafka功能模块 - 回归测试执行脚本

用法: $0 [test_type]

测试类型:
    smoke       - 冒烟测试 (核心功能验证)
    functional  - 功能回归测试
    performance - 性能测试
    all         - 完整回归测试 (默认)

环境变量:
    REDIS_HOST     - Redis主机地址 (默认: 192.168.10.109)
    REDIS_PORT     - Redis端口 (默认: 6379)
    REDIS_PASSWORD - Redis密码 (默认: drk@2025)

示例:
    $0 smoke       # 运行冒烟测试
    $0 functional  # 运行功能测试
    $0 all         # 运行完整回归测试
    REDIS_HOST=localhost $0 all  # 使用本地Redis

EOF
}

# 主函数
main() {
    print_info "Redis Stream Kafka功能模块 - 回归测试"
    print_info "项目目录: $PROJECT_DIR"
    print_info "测试类型: $TEST_TYPE"
    print_info "Redis: $REDIS_HOST:$REDIS_PORT"
    echo ""

    case "$TEST_TYPE" in
        smoke)
            run_smoke_tests
            ;;
        functional)
            run_functional_tests
            ;;
        performance)
            run_performance_tests
            ;;
        all)
            run_all_tests
            generate_report
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知的测试类型: $TEST_TYPE"
            show_help
            exit 1
            ;;
    esac
}

# 捕获中断信号
trap 'print_info "测试被中断"; exit 130' INT

# 执行主函数
main "$@"
