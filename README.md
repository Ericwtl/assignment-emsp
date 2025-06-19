✨ 项目概述
基于Spring Boot的系统，实现多状态账户管理、卡片生命周期控制及实时数据校验。


⚡ 核心功能
模块	能力描述
​账户管理​	✅ 多状态账户控制（ACTIVE/INACTIVE）
✅ 1:N账户-卡片关联体系
​卡片管理​	✅ 主键支持（PostgreSQL原生集成）
✅ 三态流转（CREATED→ASSIGNED→ACTIVATED→DEACTIVATED）
​数据安全​	✅ JPA实体关系自动校验
✅ Hibernate Schema验证（DDL-auto: validate）


🔧 技术栈
​后端框架​
Spring Boot  + Spring Data JPA + Validation

​数据库​
PostgreSQL 15

🚀 快速启动

# 1. 克隆项目
git clone https://github.com/Ericwtl/assignment-emsp.git

# 2. 配置数据库（需提前创建）
使用docker-compose 启动数据库，数据库表 通过执行flyway脚本 实现
spring.datasource.url=jdbc:postgresql://localhost:5432/emsp_db
spring.datasource.username=emsp
spring.datasource.password=emsp_123

# 3. 启动应用
./mvnw spring-boot:run  # Maven