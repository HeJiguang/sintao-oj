#!/bin/bash

# 生成一个随机字符串
COOKIE=$(openssl rand -hex 16)

echo "Generated cookie: $COOKIE"

# 将随机字符串输出到标准输出
echo "$COOKIE"