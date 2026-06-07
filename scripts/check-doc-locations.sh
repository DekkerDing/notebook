#!/bin/bash

# 文档位置检查脚本
# 检查根目录下是否有应放在openspec/目录下的文档

echo "🔍 检查文档位置规则..."

# 定义应该在openspec/目录下的文档
openspec_docs=(
    "CHANGELOG.md"
    "CONTRIBUTING.md"
    "RELEASE_NOTES.md"
    "DOC_MANAGEMENT_RULES.md"
)

# 检查根目录
violations=0
for doc in "${openspec_docs[@]}"; do
    if [ -f "../$doc" ]; then
        echo "❌ 违规: $doc 应在 openspec/ 目录下，但发现于根目录"
        violations=$((violations + 1))
    fi
done

# 检查openspec/目录
missing=0
for doc in "${openspec_docs[@]}"; do
    if [ ! -f "../openspec/$doc" ] && [ "$doc" != "RELEASE_NOTES.md" ]; then
        echo "⚠️  警告: $doc 不在 openspec/ 目录下（可选文档）"
    fi
done

# 检查README.md是否在根目录
if [ ! -f "../README.md" ]; then
    echo "❌ 错误: README.md 必须在根目录"
    violations=$((violations + 1))
fi

# 检查examples/README.md是否存在
if [ ! -f "../examples/README.md" ]; then
    echo "⚠️  警告: examples/README.md 不存在（建议创建）"
fi

# 总结
echo ""
echo "📋 检查结果:"
if [ $violations -eq 0 ]; then
    echo "✅ 所有文档位置符合规则"
    exit 0
else
    echo "❌ 发现 $violations 个违规，请修正"
    exit 1
fi
