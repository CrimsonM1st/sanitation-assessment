DROP TABLE IF EXISTS assessment_task;
CREATE TABLE `assessment_task`
(
    `id`              BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'id',
    `department_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
    `status`          VARCHAR(20) NOT NULL COMMENT '状态',
    `score`           TINYINT UNSIGNED COMMENT '分数',
    `created_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `version`         INT         NOT NULL DEFAULT 0 COMMENT '版本',
    PRIMARY KEY (`id`),
    KEY `idx_department_status_created_at`
        (`department_name`, `status`, `created_at`),
    KEY `idx_status_created_at_id`
        (`status`, `created_at`, `id`),
    CONSTRAINT `chk_assessment_task_score`
        CHECK (`score` IS NULL OR `score` BETWEEN 0 AND 100),
    CONSTRAINT `chk_assessment_task_completed_score`
        CHECK (`status` <> 'COMPLETED' OR `score` IS NOT NULL),
    CONSTRAINT `chk_assessment_task_status`
        CHECK (`status` IN (
                            'PENDING',
                            'PROCESSING',
                            'COMPLETED',
                            'CANCELLED'
            ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = '考评任务表';
DROP TABLE IF EXISTS assessment_task_audit_log;
CREATE TABLE `assessment_task_audit_log`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'id',
    task_id    BIGINT UNSIGNED NOT NULL COMMENT '任务id',
    action     VARCHAR(20)     NOT NULL COMMENT '操作',
    detail     VARCHAR(50)     NOT NULL COMMENT '操作细节',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id_created_at`
        (`task_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = '审计日志表';
DROP TABLE IF EXISTS system_user;
CREATE TABLE `sys_user`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'id',
    username      VARCHAR(50)  NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希值',
    role          VARCHAR(20)  NOT NULL COMMENT '角色',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username`
        (`username`),
    CONSTRAINT `chk_sys_user_role`
        CHECK (`role` IN (
                          'ADMIN',
                          'INSPECTOR')
            )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = '系统用户表';