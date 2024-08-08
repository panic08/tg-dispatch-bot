CREATE TABLE IF NOT EXISTS admins_table(
    id SERIAL PRIMARY KEY,
    telegram_user_id BIGINT UNIQUE NOT NULL
);