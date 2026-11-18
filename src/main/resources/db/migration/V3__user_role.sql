-- RBAC：为用户表增加角色字段，默认普通用户。
-- dev/test 由 Hibernate ddl-auto 自动加列；prod 由本迁移管理版本。
ALTER TABLE app_user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 演示环境由 DataInitializer 创建的内置管理员账号提升为 ADMIN；
-- prod 不预置账号，部署方在自注册后手动将该用户角色改为 ADMIN。
UPDATE app_user SET role = 'ADMIN' WHERE username = 'admin';
