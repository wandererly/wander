UPDATE role_change_request SET notified = 0 WHERE notified IS NULL;
ALTER TABLE role_change_request MODIFY notified BIT NOT NULL DEFAULT 0;

UPDATE notification SET read_flag = 0 WHERE read_flag IS NULL;
ALTER TABLE notification MODIFY read_flag BIT NOT NULL DEFAULT 0;
