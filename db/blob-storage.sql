ALTER TABLE users
  ADD COLUMN avatar MEDIUMBLOB NULL,
  ADD COLUMN avatar_type VARCHAR(50) NULL;

CREATE TABLE article_images (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  article_id BIGINT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  data MEDIUMBLOB NOT NULL,
  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY fk_images_owner (owner_id),
  KEY fk_images_article (article_id),
  CONSTRAINT fk_images_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_images_article FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
