CREATE DATABASE IF NOT EXISTS ai_naming
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ai_naming;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     openid VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(100) DEFAULT '',
    free_times_today INT DEFAULT 0,
    last_use_date DATETIME DEFAULT NULL,
    is_vip TINYINT(1) DEFAULT 0,
    vip_expire_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 姓名评分表
CREATE TABLE IF NOT EXISTS name_scores (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           full_name VARCHAR(20) NOT NULL,
    surname VARCHAR(10) DEFAULT '',
    given_name VARCHAR(10) DEFAULT '',
    gender VARCHAR(4) DEFAULT '',
    total_score INT DEFAULT 0,
    meaning_score INT DEFAULT 0,
    sound_score INT DEFAULT 0,
    wuxing_score INT DEFAULT 0,
    sancai_score INT DEFAULT 0,
    culture_score INT DEFAULT 0,
    modernity_score INT DEFAULT 0,
    analysis_text TEXT,
    ai_comment TEXT,
    characters_detail TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_full_name (full_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户收藏表
CREATE TABLE IF NOT EXISTS user_favorites (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              full_name VARCHAR(20) DEFAULT '',
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;