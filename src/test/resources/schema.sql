DROP TABLE IF EXISTS assessment_task;

CREATE TABLE assessment_task
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    score           TINYINT,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version`       INT          NOT NULL DEFAULT 0,

    CONSTRAINT chk_assessment_task_score
        CHECK (score IS NULL OR score BETWEEN 0 AND 100),

    CONSTRAINT chk_assessment_task_completed_score
        CHECK (status <> 'COMPLETED' OR score IS NOT NULL)
);

DROP TABLE IF EXISTS assessment_task_audit_log;

CREATE TABLE `assessment_task_audit_log`
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT       NOT NULL,
    action     VARCHAR(20)  NOT NULL,
    detail     VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);