CREATE TABLE IF NOT EXISTS groups_table(
    id SERIAL PRIMARY KEY,
    telegram_chat_id BIGINT UNIQUE NOT NULL
);