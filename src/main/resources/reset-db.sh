#!/bin/bash
echo "reset db - confirm？(y/n)"
read answer

if [ "$answer" != "${answer#[Yy]}" ]; then
    echo "reset db starting..."

    # 获取脚本所在目录
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

    # 备份数据库（直接操作Docker容器）
    docker exec assignment-db pg_dump -U emsp emsp_db > "$SCRIPT_DIR/db_backup_$(date +%Y%m%d).sql"

    # 修改配置（使用绝对路径）
    CONFIG_FILE="$SCRIPT_DIR/application-dev.yml"
    sed -i 's/validate-on-migrate: true/validate-on-migrate: false/' "$CONFIG_FILE"

    # 在项目根目录执行Flyway
    cd "$SCRIPT_DIR/../../.."  # 返回项目根目录
    mvn flyway:clean flyway:migrate -Dspring-boot.run.profiles=dev

    # 恢复配置
    sed -i 's/validate-on-migrate: false/validate-on-migrate: true/' "$CONFIG_FILE"

    echo "db reset finished！bak file location: $SCRIPT_DIR/db_backup_*.sql"
else
    echo "reset canceled"
fi