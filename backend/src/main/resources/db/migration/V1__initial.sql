CREATE TABLE app_user (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(255),
 email VARCHAR(255),
 password_hash VARCHAR(255),
 role VARCHAR(255),
 active BIT NOT NULL,
 verified BIT NOT NULL,
 available BIT NOT NULL,
 token_version INT NOT NULL
);
CREATE TABLE catalog_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 kind VARCHAR(255),
 name VARCHAR(255),
 active BIT NOT NULL
);
CREATE TABLE settings (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 priority_strategy VARCHAR(255),
 impact_weight INT NOT NULL,
 urgency_weight INT NOT NULL,
 criticality_weight INT NOT NULL,
 affected_weight INT NOT NULL,
 medium_cutoff INT NOT NULL,
 high_cutoff INT NOT NULL,
 critical_cutoff INT NOT NULL,
 matrix VARCHAR(255),
 enabled_strategies VARCHAR(255),
 skill_weight INT NOT NULL,
 load_weight INT NOT NULL,
 skill_threshold INT NOT NULL,
 warning_thresholds VARCHAR(255),
 reopen_days INT NOT NULL,
 email_enabled BIT NOT NULL,
 revision BIGINT NOT NULL
);
CREATE TABLE incident (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 reference VARCHAR(255),
 title VARCHAR(255),
 description TEXT,
 service_id BIGINT,
 category_id BIGINT,
 reporter_id BIGINT,
 technician_id BIGINT,
 status VARCHAR(255),
 priority VARCHAR(255),
 impact INT NOT NULL,
 urgency INT NOT NULL,
 criticality INT NOT NULL,
 affected_users INT NOT NULL,
 created_at DATETIME(6),
 closed_at DATETIME(6),
 version BIGINT
);
CREATE TABLE incident_comment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 author_id BIGINT,
 body TEXT,
 internal_note BIT NOT NULL,
 created_at DATETIME(6)
);
CREATE TABLE resolution_record (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 technician_id BIGINT,
 description TEXT,
 resolved_at DATETIME(6),
 verified_at DATETIME(6),
 rejected_at DATETIME(6),
 rejection_reason TEXT
);
CREATE TABLE assignment_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 previous_technician_id BIGINT,
 technician_id BIGINT,
 actor_id BIGINT,
 strategy VARCHAR(255),
 selection_source VARCHAR(255),
 reason TEXT,
 created_at DATETIME(6),
 acknowledged_at DATETIME(6)
);
CREATE TABLE priority_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 old_priority VARCHAR(255),
 new_priority VARCHAR(255),
 actor_id BIGINT,
 config_version BIGINT NOT NULL,
 factors TEXT,
 reason TEXT,
 created_at DATETIME(6)
);
CREATE TABLE sla_policy (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 service_id BIGINT,
 priority VARCHAR(255),
 response_seconds BIGINT NOT NULL,
 resolution_seconds BIGINT NOT NULL,
 active BIT NOT NULL,
 revision BIGINT NOT NULL
);
CREATE TABLE sla_cycle (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 kind VARCHAR(255),
 policy_id BIGINT,
 policy_version BIGINT NOT NULL,
 priority VARCHAR(255),
 original_target_seconds BIGINT NOT NULL,
 started_at DATETIME(6),
 stopped_at DATETIME(6),
 paused_at DATETIME(6),
 paused_seconds BIGINT NOT NULL
);
CREATE TABLE sla_revision (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cycle_id BIGINT,
 policy_id BIGINT,
 policy_version BIGINT NOT NULL,
 old_target_seconds BIGINT NOT NULL,
 new_target_seconds BIGINT NOT NULL,
 old_priority VARCHAR(255),
 new_priority VARCHAR(255),
 actor_id BIGINT,
 created_at DATETIME(6)
);
CREATE TABLE pause_record (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cycle_id BIGINT,
 started_at DATETIME(6),
 ended_at DATETIME(6)
);
CREATE TABLE sla_signal (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cycle_id BIGINT,
 threshold INT NOT NULL,
 deadline DATETIME(6),
 detected_at DATETIME(6)
);
CREATE TABLE audit_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 actor_id BIGINT,
 action VARCHAR(255),
 entity_type VARCHAR(255),
 entity_id BIGINT,
 details TEXT,
 created_at DATETIME(6)
);
CREATE TABLE notification (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT,
 incident_id BIGINT,
 message TEXT,
 created_at DATETIME(6),
 read_flag BIT NOT NULL,
 delivery_status VARCHAR(255),
 attempts INT NOT NULL,
 delivery_error TEXT
);
CREATE TABLE account_token (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT,
 token_hash VARCHAR(255),
 kind VARCHAR(255),
 expires_at DATETIME(6),
 used BIT NOT NULL
);
CREATE TABLE mail_job (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 recipient VARCHAR(255),
 subject VARCHAR(255),
 body TEXT,
 status VARCHAR(255),
 attempts INT NOT NULL,
 error TEXT,
 created_at DATETIME(6)
);
CREATE TABLE attachment_metadata (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 uploader_id BIGINT,
 filename VARCHAR(255),
 storage_key VARCHAR(255),
 content_type VARCHAR(255),
 size_bytes BIGINT NOT NULL,
 created_at DATETIME(6),
 deleted BIT NOT NULL
);
CREATE TABLE feedback (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 user_id BIGINT,
 rating INT NOT NULL,
 comment TEXT,
 created_at DATETIME(6)
);
CREATE TABLE skill_credit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 technician_id BIGINT,
 category_id BIGINT,
 created_at DATETIME(6)
);
CREATE TABLE technician_skill (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 technician_id BIGINT,
 category_id BIGINT,
 threshold_at_recognition INT NOT NULL,
 recognized_at DATETIME(6)
);
ALTER TABLE incident ADD FOREIGN KEY (service_id) REFERENCES catalog_item(id);
ALTER TABLE incident ADD FOREIGN KEY (category_id) REFERENCES catalog_item(id);
ALTER TABLE incident ADD FOREIGN KEY (reporter_id) REFERENCES app_user(id);
ALTER TABLE incident ADD FOREIGN KEY (technician_id) REFERENCES app_user(id);
ALTER TABLE incident_comment ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE incident_comment ADD FOREIGN KEY (author_id) REFERENCES app_user(id);
ALTER TABLE resolution_record ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE resolution_record ADD FOREIGN KEY (technician_id) REFERENCES app_user(id);
ALTER TABLE assignment_history ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE assignment_history ADD FOREIGN KEY (previous_technician_id) REFERENCES app_user(id);
ALTER TABLE assignment_history ADD FOREIGN KEY (technician_id) REFERENCES app_user(id);
ALTER TABLE assignment_history ADD FOREIGN KEY (actor_id) REFERENCES app_user(id);
ALTER TABLE priority_history ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE priority_history ADD FOREIGN KEY (actor_id) REFERENCES app_user(id);
ALTER TABLE sla_policy ADD FOREIGN KEY (service_id) REFERENCES catalog_item(id);
ALTER TABLE sla_cycle ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE sla_cycle ADD FOREIGN KEY (policy_id) REFERENCES sla_policy(id);
ALTER TABLE sla_revision ADD FOREIGN KEY (cycle_id) REFERENCES sla_cycle(id);
ALTER TABLE sla_revision ADD FOREIGN KEY (policy_id) REFERENCES sla_policy(id);
ALTER TABLE sla_revision ADD FOREIGN KEY (actor_id) REFERENCES app_user(id);
ALTER TABLE pause_record ADD FOREIGN KEY (cycle_id) REFERENCES sla_cycle(id);
ALTER TABLE sla_signal ADD FOREIGN KEY (cycle_id) REFERENCES sla_cycle(id);
ALTER TABLE audit_log ADD FOREIGN KEY (actor_id) REFERENCES app_user(id);
ALTER TABLE notification ADD FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE notification ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE account_token ADD FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE attachment_metadata ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE attachment_metadata ADD FOREIGN KEY (uploader_id) REFERENCES app_user(id);
ALTER TABLE feedback ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE feedback ADD FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE skill_credit ADD FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE skill_credit ADD FOREIGN KEY (technician_id) REFERENCES app_user(id);
ALTER TABLE skill_credit ADD FOREIGN KEY (category_id) REFERENCES catalog_item(id);
ALTER TABLE technician_skill ADD FOREIGN KEY (technician_id) REFERENCES app_user(id);
ALTER TABLE technician_skill ADD FOREIGN KEY (category_id) REFERENCES catalog_item(id);
CREATE UNIQUE INDEX uq_app_user ON app_user(email);
CREATE UNIQUE INDEX uq_incident ON incident(reference);
CREATE UNIQUE INDEX uq_account_token ON account_token(token_hash);
CREATE UNIQUE INDEX uq_sla_signal ON sla_signal(cycle_id,threshold);
CREATE UNIQUE INDEX uq_feedback ON feedback(incident_id);
CREATE UNIQUE INDEX uq_skill_credit ON skill_credit(incident_id);
CREATE UNIQUE INDEX uq_technician_skill ON technician_skill(technician_id,category_id);
CREATE UNIQUE INDEX uq_catalog_item ON catalog_item(kind,name);
CREATE INDEX ix_incident_search ON incident(status,priority,created_at);
CREATE INDEX ix_sla_open ON sla_cycle(stopped_at,kind);
CREATE INDEX ix_audit ON audit_log(entity_type,entity_id,created_at);
CREATE INDEX ix_notifications ON notification(user_id,read_flag);
INSERT INTO settings (id,priority_strategy,impact_weight,urgency_weight,criticality_weight,affected_weight,medium_cutoff,high_cutoff,critical_cutoff,matrix,enabled_strategies,skill_weight,load_weight,skill_threshold,warning_thresholds,reopen_days,email_enabled,revision) VALUES (1,'FORMULA',3,3,2,1,13,20,27,'LOW,LOW,MEDIUM,LOW,MEDIUM,HIGH,MEDIUM,HIGH,CRITICAL','LEAST_LOADED,SKILL_FIRST,PRIORITY_WEIGHTED,ROUND_ROBIN',100,10,60,'50,80',90,0,1);