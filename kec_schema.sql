-- KEC R2024 Student Academic & Continuous Evaluation System
CREATE DATABASE IF NOT EXISTS kec_eval;
USE kec_eval;

CREATE TABLE IF NOT EXISTS students (
  roll_no          VARCHAR(20)  PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  department       VARCHAR(20)  NOT NULL,
  current_semester INT          NOT NULL DEFAULT 3,
  created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_sem CHECK (current_semester BETWEEN 1 AND 8)
);

CREATE TABLE IF NOT EXISTS courses (
  course_code VARCHAR(20)  PRIMARY KEY,
  title       VARCHAR(150) NOT NULL,
  credits     INT          NOT NULL,
  course_type ENUM('THEORY','PRACTICAL','EMBEDDED') NOT NULL,
  semester    INT          NOT NULL DEFAULT 3,
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_credits CHECK (credits BETWEEN 1 AND 10),
  CONSTRAINT chk_csem CHECK (semester BETWEEN 1 AND 8)
);

CREATE TABLE IF NOT EXISTS enrollments (
  enrollment_id    INT AUTO_INCREMENT PRIMARY KEY,
  roll_no          VARCHAR(20) NOT NULL,
  course_code      VARCHAR(20) NOT NULL,
  semester         INT         NOT NULL DEFAULT 3,
  total_classes    INT         NOT NULL DEFAULT 0,
  attended_classes INT         NOT NULL DEFAULT 0,
  is_condoned      TINYINT(1)  NOT NULL DEFAULT 0,
  created_at       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_enr_student FOREIGN KEY (roll_no) REFERENCES students(roll_no) ON DELETE CASCADE,
  CONSTRAINT fk_enr_course  FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE,
  CONSTRAINT uq_enrollment UNIQUE (roll_no, course_code, semester),
  CONSTRAINT chk_att CHECK (attended_classes <= total_classes)
);

CREATE TABLE IF NOT EXISTS assessment_marks (
  mark_id          INT AUTO_INCREMENT PRIMARY KEY,
  enrollment_id    INT NOT NULL UNIQUE,
  cia1_marks       DECIMAL(5,2) NULL,
  cia2_marks       DECIMAL(5,2) NULL,
  assignment_marks DECIMAL(5,2) NULL,
  ese_marks        DECIMAL(5,2) NULL,
  is_ese_absent    TINYINT(1) NOT NULL DEFAULT 0,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_marks_enr FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
  CONSTRAINT chk_cia1 CHECK (cia1_marks IS NULL OR cia1_marks BETWEEN 0 AND 100),
  CONSTRAINT chk_cia2 CHECK (cia2_marks IS NULL OR cia2_marks BETWEEN 0 AND 100),
  CONSTRAINT chk_asg  CHECK (assignment_marks IS NULL OR assignment_marks BETWEEN 0 AND 100),
  CONSTRAINT chk_ese  CHECK (ese_marks IS NULL OR ese_marks BETWEEN 0 AND 100)
);
