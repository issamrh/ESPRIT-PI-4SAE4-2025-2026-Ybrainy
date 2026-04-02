<div align="center">

<img src="https://img.shields.io/badge/YBrainy-AI--Powered%20Learning-1A5FA5?style=for-the-badge&logoColor=white" />

# YBrainy — Intelligent E-Learning & Certification Platform

**A unified ecosystem combining personalized AI learning, secure payments, community interaction, and advanced analytics to redefine digital education.**

[![Angular](https://img.shields.io/badge/Angular-18-DD0031?style=flat-square&logo=angular&logoColor=white)](https://angular.io)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com)
[![Keycloak](https://img.shields.io/badge/Keycloak-Auth-4D4D4D?style=flat-square&logo=keycloak&logoColor=white)](https://www.keycloak.org)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=flat-square&logo=stripe&logoColor=white)](https://stripe.com)

</div>

---

## 📌 Overview

Traditional online learning platforms suffer from a lack of personalization, low engagement, weak analytics, and security gaps. **YBrainy** solves all of these with a smart, scalable, microservices-based platform built for the future of digital education.

| Problem | YBrainy Solution |
|---|---|
| ❌ No personalization | ✅ AI-powered adaptive learning paths |
| ❌ Low engagement | ✅ Forums, live sessions, chatbot support |
| ❌ Weak analytics | ✅ Real-time dashboards & predictive insights |
| ❌ Security concerns | ✅ Keycloak, JWT, HTTPS, fraud detection |
| ❌ Fragmented tooling | ✅ Fully integrated learning ecosystem |

---

## 🎯 Vision

> Deliver cutting-edge AI-powered learning with globally recognized certifications — personalized, secure, and accessible to everyone.

---

## 🧩 Platform Modules

<table>
  <tr>
    <td>

### 👤 User Management
- Secure authentication (JWT + Keycloak)
- Role-based access: Admin / Instructor / Student
- Profile management & audit logs

    </td>
    <td>

### 🧠 Courses & Certifications
- Video + rich content course creation
- Quiz & exam engine
- AI-powered recommendations & adaptive paths

    </td>
    <td>

### 💳 Payment & Finance
- Secure payments via Stripe
- Transaction tracking & financial reports
- Real-time fraud detection

    </td>
  </tr>
  <tr>
    <td>

### 💬 Forum & Events
- Discussion forums
- Student–Instructor interaction
- Webinars, live sessions & participation tracking

    </td>
    <td>

### 📊 Analytics Dashboard
- Performance dashboards
- Student progress tracking
- AI insights & predictive reporting

    </td>
    <td>

### 🤝 Partnerships
- Institutional collaborations
- B2B enterprise training
- Certification & accreditation partnerships

    </td>
  </tr>
</table>

---

## 👥 System Actors

```
┌──────────────────────┬──────────────────────┬──────────────────────┐
│   🛠️  Administrator  │  👨‍🏫  Instructor       │   🎓  Student        │
├──────────────────────┼──────────────────────┼──────────────────────┤
│ Manage users & roles │ Create & manage       │ Access & enroll in   │
│ Oversee courses &    │ courses               │ courses              │
│ certifications       │ Design quizzes &      │ Take quizzes & exams │
│ Monitor platform     │ assessments           │ Earn certifications  │
│ View stats & reports │ Track student         │ Track achievements   │
│                      │ progress              │ Get AI suggestions   │
└──────────────────────┴──────────────────────┴──────────────────────┘
```

---

## 🤖 AI Features

| Feature | Description |
|---|---|
| 🔍 **Recommendation Engine** | Suggests courses and paths based on learner behavior and history |
| 📈 **Predictive Analytics** | Forecasts student performance and flags at-risk learners |
| 🛡️ **Fraud Detection** | Monitors payment activity in real time and blocks suspicious transactions |
| 💬 **Chatbot Assistant** | 24/7 AI-powered support for students, instructors, and navigation |
| 🎯 **Adaptive Quizzing** | Dynamically adjusts difficulty based on individual responses |
| 🔮 **Performance Forecasting** | Predicts long-term learner outcomes to support proactive intervention |

---

## ⚙️ Core Features

<details>
<summary><strong>🧠 Personalization</strong></summary>

- AI-powered course and content recommendations
- Adaptive quiz difficulty based on performance
- Predictive analytics for custom learning paths

</details>

<details>
<summary><strong>💬 Interaction & Community</strong></summary>

- 24/7 AI chatbot assistant
- Discussion forums per course
- Direct student–instructor messaging
- Live webinars and virtual sessions

</details>

<details>
<summary><strong>🔐 Security</strong></summary>

- Keycloak identity & access management
- JWT-based stateless authentication
- HTTPS encryption across all services
- Real-time payment fraud detection

</details>

<details>
<summary><strong>📊 Analytics</strong></summary>

- Real-time performance dashboards
- Granular student progress tracking
- AI-generated insights and reports
- Financial analytics and transaction monitoring

</details>

---

## 🏗️ Architecture

YBrainy is built on a **microservices architecture** with an API Gateway, service discovery, and async messaging for high scalability and resilience.

```
┌─────────────────────────────────────────────────────┐
│                    Angular 18 Frontend               │
│              (Frontoffice + Backoffice)               │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / REST
┌──────────────────────▼──────────────────────────────┐
│                    API Gateway                        │
│             (Routing + Auth + Rate Limiting)          │
└──┬───────┬────────┬────────┬────────┬───────┬───────┘
   │       │        │        │        │       │
   ▼       ▼        ▼        ▼        ▼       ▼
 User   Course    Quiz   Payment Finance  Forum &
Service Service  Service  Service Service  Events
   │       │        │        │        │       │
   └───────┴────────┴────────┴────────┴───────┘
                       │
          ┌────────────▼────────────┐
          │    MySQL  |  RabbitMQ   │
          │   (Data)  |  (Events)   │
          └─────────────────────────┘
                       │
          ┌────────────▼────────────┐
          │  Keycloak  |  Eureka    │
          │   (Auth)   | (Discovery)│
          └─────────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | Angular 18 |
| **Backend** | Spring Boot (Microservices) |
| **Security** | Keycloak, JWT |
| **Database** | MySQL |
| **Messaging** | RabbitMQ |
| **DevOps** | Docker |
| **Payment** | Stripe |
| **AI / ML** | Python |

---

## 📦 Project Structure

```
ybrainy/
│
├── angular-app/                  # Frontend (Frontoffice + Backoffice)
│
└── backend/
    ├── gateway/                  # API Gateway
    ├── discovery-service/        # Eureka Service Discovery
    ├── user-service/             # Authentication & user management
    ├── course-service/           # Course & content management
    ├── quiz-service/             # Quiz, exam & certification engine
    ├── payment-service/          # Stripe payments & transactions
    ├── finance-service/          # Financial reports & analytics
    ├── forum-service/            # Forums & messaging
    └── event-service/            # Events, webinars & live sessions
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- MySQL 8

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd angular-app
npm install
ng serve
```

### Run with Docker

```bash
docker-compose up --build
```

---

## 📊 Non-Functional Requirements

| Requirement | Approach |
|---|---|
| 🔒 **Security** | JWT, Keycloak, HTTPS encryption |
| ⚡ **Performance** | Caching, async messaging via RabbitMQ |
| 📈 **Scalability** | Microservices + Docker containerization |
| 🎯 **Usability** | Responsive Angular UI/UX |
| 🌐 **Availability** | 24/7 uptime with service discovery & failover |

---

## 🆚 Competitive Advantages

- ✅ Strong AI personalization across the entire learning journey
- ✅ Fully integrated ecosystem — learning, finance, and community in one place
- ✅ Local & global adaptability with partnership and certification support
- ✅ Advanced analytics and engagement tools for instructors and admins

---

## 🗺️ Roadmap

- [ ] Advanced AI recommendation system with collaborative filtering
- [ ] Smart proctoring with AI-powered anti-cheating detection
- [ ] Native mobile application (iOS & Android)
- [ ] Global certification partnerships and international accreditation

---

## 👨‍💻 Team

**YBrainy Development Team**

---

## 📜 License

This project is developed for academic and educational purposes.
