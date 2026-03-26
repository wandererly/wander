CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS course (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(1000),
  teacher_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS course_section (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  order_index INT NOT NULL,
  content_url VARCHAR(500),
  CONSTRAINT fk_section_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE IF NOT EXISTS enrollment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  created_at DATETIME,
  CONSTRAINT uk_enrollment UNIQUE (user_id, course_id),
  CONSTRAINT fk_enroll_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE IF NOT EXISTS learning_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  completed_section_count INT,
  progress_percent INT,
  CONSTRAINT uk_learning_progress UNIQUE (user_id, course_id),
  CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_progress_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE IF NOT EXISTS section_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  section_id BIGINT NOT NULL,
  completed_at DATETIME,
  CONSTRAINT uk_section_progress UNIQUE (user_id, course_id, section_id),
  CONSTRAINT fk_sp_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_sp_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_sp_section FOREIGN KEY (section_id) REFERENCES course_section(id)
);

CREATE TABLE IF NOT EXISTS role_change_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  target_role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(500),
  admin_note VARCHAR(500),
  notified BIT DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME,
  CONSTRAINT fk_role_req_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(1000),
  read_flag BIT DEFAULT 0,
  created_at DATETIME,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);
