CREATE INDEX idx_course_teacher ON course(teacher_id);
CREATE INDEX idx_course_status ON course(status);

CREATE INDEX idx_section_course ON course_section(course_id);
CREATE INDEX idx_section_course_order ON course_section(course_id, order_index);

CREATE INDEX idx_enrollment_user ON enrollment(user_id);
CREATE INDEX idx_enrollment_course ON enrollment(course_id);

CREATE INDEX idx_progress_user ON learning_progress(user_id);
CREATE INDEX idx_progress_course ON learning_progress(course_id);

CREATE INDEX idx_section_progress_user ON section_progress(user_id);
CREATE INDEX idx_section_progress_course ON section_progress(course_id);
CREATE INDEX idx_section_progress_section ON section_progress(section_id);

CREATE INDEX idx_role_request_user ON role_change_request(user_id);
CREATE INDEX idx_role_request_status ON role_change_request(status);

CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_read ON notification(read_flag);
