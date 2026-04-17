# Files To Send To Friend For Integration

This is the handoff list for integrating these two microservices into a friend's project:

- `personality-behavior-service`
- `warning-ban-appeal-service`

The safest handoff is to send the full folders listed below, then apply the small user-service, gateway, and frontend integration files.

## 1. Backend Microservices To Send

Send these full backend folders:

```text
personality-behavior-service/
warning-ban-appeal-service/
```

These folders contain the full Spring Boot microservices:

- MongoDB entities and repositories
- CRUD controllers
- DTOs
- services
- RabbitMQ configuration
- RabbitMQ event publishers/consumers
- OpenFeign user validation clients
- Eureka discovery configuration
- Maven `pom.xml`

## 2. User Service Files Your Friend Must Merge

Your friend already has the working user service, so do not overwrite his whole user service. Send only these files and tell him to merge the code manually:

```text
user/src/main/java/esprit/tn/breadandbutteruser/controllers/InternalUserController.java
user/src/main/java/esprit/tn/breadandbutteruser/dto/InternalUserResponse.java
user/src/main/java/esprit/tn/breadandbutteruser/services/UserService.java
user/src/main/resources/application.properties
```

Why these are needed:

- `InternalUserController.java` exposes `/api/users/internal/{id}`.
- `InternalUserResponse.java` is the DTO returned to other microservices.
- `UserService.java` contains `getInternalUser(Long userId)`.
- `application.properties` contains service discovery, RabbitMQ, and local health settings.

Important merge note:

If your friend's user service already has its own `UserService.java`, do not replace it. He only needs to add this method:

```java
@Transactional(readOnly = true)
public InternalUserResponse getInternalUser(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    return InternalUserResponse.fromUser(user);
}
```

And this controller endpoint:

```java
@GetMapping("/{id}")
public ResponseEntity<InternalUserResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getInternalUser(id));
}
```

## 3. API Gateway File To Merge

Send this file:

```text
Parteneriat/backend/api-gateway/src/main/resources/application.yml
```

Your friend should merge only the routes for:

```text
/api/personalities/**
/api/behaviors/**
/api/warnings/**
/api/ban-appeals/**
```

Expected gateway targets:

```text
lb://personality-behavior-service
lb://personality-behavior-service
lb://warning-ban-appeal-service
lb://warning-ban-appeal-service
```

## 4. Frontend Files To Send

Send these Angular frontend files:

```text
user/angular/angular-app/src/app/models/personality.model.ts
user/angular/angular-app/src/app/services/personality.service.ts
user/angular/angular-app/src/app/backoffice/personality/personality.module.ts
user/angular/angular-app/src/app/backoffice/personality/personality-dashboard/personality-dashboard.component.ts
user/angular/angular-app/src/app/backoffice/personality/personality-list/personality-list.component.ts
user/angular/angular-app/src/app/backoffice/personality/personality-form/personality-form.component.ts
user/angular/angular-app/src/app/backoffice/personality/personality-detail/personality-detail.component.ts
user/angular/angular-app/src/app/backoffice/personality/behavior-monitor/behavior-monitor.component.ts
user/angular/angular-app/src/app/backoffice/personality/behavior-list/behavior-list.component.ts
user/angular/angular-app/src/app/backoffice/personality/behavior-form/behavior-form.component.ts
user/angular/angular-app/src/app/backoffice/personality/behavior-detail/behavior-detail.component.ts
```

Frontend merge note:

The personality and behavior components in this repo use inline templates/styles inside their `.ts` files. There are no separate `.html` or `.css` files for those components in this snapshot.

The current frontend service points directly to:

```text
http://localhost:8084/api/personalities
http://localhost:8084/api/behaviors
```

If your friend wants everything through the API gateway, change those frontend URLs to:

```text
http://localhost:8096/api/personalities
http://localhost:8096/api/behaviors
```

If warning and ban appeal screens are added later, their gateway URLs should be:

```text
http://localhost:8096/api/warnings
http://localhost:8096/api/ban-appeals
```

## 5. Optional Runner Files To Send

Send these if your friend wants the same local launch scripts:

```text
run-services/_run-service.ps1
run-services/run-user-service.ps1
run-services/run-personality-behavior-service.ps1
run-services/run-warning-ban-appeal-service.ps1
run-services/run-eureka.ps1
run-services/README.md
```

These are optional. The microservices can also be launched with normal Maven commands.

## 6. Required Runtime Services

Your friend should run:

```text
MongoDB
RabbitMQ
Eureka discovery server
API gateway
user service
personality-behavior-service
warning-ban-appeal-service
```

Default ports:

```text
Eureka: 8761
API gateway: 8096
user service: 8899
personality-behavior-service: 8084
warning-ban-appeal-service: 8086
RabbitMQ: 5672
MongoDB: 27017
```

## 7. OpenFeign Contract

Both new microservices call the user service through OpenFeign:

```text
GET /api/users/internal/{id}
```

The user service response must look like this:

```json
{
  "userId": 1,
  "username": "localtester",
  "email": "localtester@ybrainy.test",
  "firstName": null,
  "lastName": null
}
```

If this endpoint does not exist, creating personality, behavior, warning, or ban appeal records will fail because the services validate that the user exists.

## 8. RabbitMQ Contract

Shared exchange:

```text
ybrainy.events
```

Important routing keys:

```text
user.deleted
personality.created
personality.updated
personality.behavior.created
personality.behavior.updated
warning.created
warning.updated
warning.deleted
ban-appeal.created
ban-appeal.updated
ban-appeal.deleted
```

When a user is deleted, publish this event:

```json
{
  "eventType": "USER_DELETED",
  "userId": 1
}
```

Expected behavior:

- `personality-behavior-service` deletes personality/behavior data for that user.
- `warning-ban-appeal-service` deletes warning/appeal data for that user.

## 9. Quick Test URLs

Direct service URLs:

```text
http://localhost:8084/api/personalities
http://localhost:8084/api/behaviors
http://localhost:8086/api/warnings
http://localhost:8086/api/ban-appeals
```

Gateway URLs:

```text
http://localhost:8096/api/personalities
http://localhost:8096/api/behaviors
http://localhost:8096/api/warnings
http://localhost:8096/api/ban-appeals
```

Health URLs:

```text
http://localhost:8899/actuator/health
http://localhost:8084/actuator/health
http://localhost:8086/actuator/health
http://localhost:8096/actuator/health
http://localhost:8761
```
