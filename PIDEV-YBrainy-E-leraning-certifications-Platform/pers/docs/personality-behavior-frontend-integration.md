# Personality, Behavior, Warning, and Ban Appeal Integration Guide

## Combined microservices

- `personality-behavior-service`: `http://localhost:8084`
- `warning-ban-appeal-service`: `http://localhost:8086`
- `breadandbutteruser` internal lookup: `http://localhost:8899/api/users/internal/{id}`

## Final backend layout

- Personality and behavior stay together in one Mongo microservice.
- Warning and ban appeal stay together in one Mongo microservice.
- Both microservices validate `userId` through OpenFeign before creating records.
- Both microservices consume `user.deleted` from RabbitMQ so user cleanup cascades automatically.

## Frontend personality module

- Personality endpoints use `http://localhost:8084/api/personalities`
- Behavior endpoints use `http://localhost:8084/api/behaviors`
- Personality responses may include nested behavior data, and behavior can still be managed through its own CRUD screens.

## Run order

1. Start MongoDB.
2. Start RabbitMQ.
3. Start `breadandbutteruser`.
4. Start `personality-behavior-service`.
5. Start `warning-ban-appeal-service`.
6. Start the Angular host app.

## Smoke checklist

- `GET /api/users/internal/{id}`
- `GET /api/personalities`
- `GET /api/behaviors`
- `GET /api/warnings`
- `GET /api/ban-appeals`
