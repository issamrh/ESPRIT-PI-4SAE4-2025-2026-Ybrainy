MERGE INTO users (
    user_id,
    keycloak_user_id,
    username,
    email,
    role,
    bio,
    profile_picture,
    xp,
    level,
    streak_days
) KEY (user_id) VALUES (
    1,
    'local-seed-user',
    'localtester',
    'localtester@ybrainy.test',
    'USER',
    'Local integration test user',
    null,
    120,
    2,
    3
);
