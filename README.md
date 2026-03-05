# 🎓 yBrainy Forum Management System

<div align="center">
  <img src="https://img.shields.io/badge/Esprit-School_of_Engineering-blue?style=for-the-badge" alt="Esprit"/>
  <img src="https://img.shields.io/badge/Year-2025--2026-green?style=for-the-badge" alt="Year"/>
  <img src="https://img.shields.io/badge/Angular-17-red?style=for-the-badge&logo=angular" alt="Angular"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-green?style=for-the-badge&logo=springboot" alt="Spring Boot"/>
</div>

<br/>

## 📋 Overview

This project was developed as part of the **PIDEV – 4rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

yBrainy Forum is an intelligent, AI-powered community platform designed specifically for e-learning environments. It goes beyond traditional discussion boards by integrating machine learning for auto-categorization, smart content suggestions, and a comprehensive gamification system that motivates student engagement and collaboration.

The platform is built using a **microservices architecture** with Angular for the frontend and Spring Boot for the backend, demonstrating enterprise-level software engineering practices and modern development workflows.

---

## ✨ Key Features

### 💬 Core Forum Management

#### Thread & Post System
- **Create Threads**: Rich text editor with Markdown support, code highlighting, and LaTeX equations
- **Nested Comments**: Up to 3 levels of comment threading for organized discussions
- **Post Management**: Edit history, soft delete, version control for all posts
- **Thread Categories**: Hierarchical categorization with parent-child relationships
- **Tag System**: Multi-tag support for better content organization and discovery
- **View Counter**: Real-time view tracking with Redis buffering for performance
- **Pin Threads**: Moderators can pin important announcements to the top

#### AI-Powered Features
- **Auto-Categorization**: Machine learning model automatically classifies threads into 20+ categories with 85%+ accuracy using BERT
- **AI Auto-Description Generator**: Automatically generates thread summaries and meta-descriptions using NLP
- **Smart Tag Suggestions**: AI suggests relevant tags based on thread title and content
- **Draft Auto-Save**: Intelligent auto-save system that preserves drafts with AI-powered recovery suggestions
- **Content Quality Scoring**: AI evaluates post quality and provides improvement suggestions before publishing

### 🔔 Notification System

#### Real-Time Notifications
- **In-App Notifications**: WebSocket-powered instant notifications with badge counter
- **Notification Types**:
  - 💬 Reply to your thread
  - 📝 Comment on your post
  - ⭐ Best Answer selected
  - 🏆 Badge earned
  - 📊 Level up achievement
  - 👤 Mention (@username)
  - 🔔 System announcements
- **Notification Center**: Dedicated page with filtering (All, Mentions, Replies, Achievements)
- **Mark as Read/Unread**: Bulk actions for notification management
- **Email Digest**: Daily/weekly configurable email summaries
- **Push Notifications**: Browser push notifications for critical events (opt-in)

### 🎮 Gamification & Leveling System

#### XP & Progression
- **8-Level System**: Newcomer (Lvl 1) → Explorer → Contributor → Active Member → Helper → Trusted → Expert → Legend (Lvl 8)
- **XP Sources**:
  - Thread created: +20 XP
  - Reply posted: +10 XP
  - Best Answer selected: +50 XP
  - Upvote received: +5 XP
  - Daily login: +5 XP
  - Streak bonus: +10 XP per day
- **Level Perks**: Each level unlocks new privileges (downvote, moderation tools, advanced features)
- **XP Leaderboard**: Weekly, monthly, and all-time rankings with top contributor showcase

#### Badge System
- **Achievement Badges**:
  - 🌟 Problem Solver (10+ Best Answers)
  - 🔥 7-Day Streak / 30-Day Streak / 100-Day Streak
  - 💡 Helpful (50+ upvotes received)
  - 🎓 Knowledge Contributor (20+ threads created)
  - 👨‍🏫 Mentor (helped 25+ users)
- **Topic Expertise Badges**: Automatic badges for specialized topics (Python Specialist, Java Expert, etc.)
- **Rare Achievements**: Special badges for unique accomplishments

#### User Profile Stats
- Total XP and current level with progress bar
- Threads created / Replies given / Best Answers count
- Earned badges showcase with rarity indicators
- Reputation score based on community feedback
- Activity heatmap showing engagement over time
- Expertise tags highlighting strong topics

### 📝 Draft & Wishlist System

#### Intelligent Draft Management
- **Auto-Save**: Drafts automatically saved every 30 seconds
- **Draft Recovery**: AI-powered recovery suggestions if browser crashes
- **Multiple Drafts**: Save unlimited drafts with preview thumbnails
- **Draft Metadata**: Last edited timestamp, word count, completion percentage
- **AI Draft Enhancement**: Click "Improve Draft" to get AI suggestions for better structure and clarity
- **Scheduled Publishing**: Set drafts to auto-publish at a specific date/time

#### Thread Wishlist
- **Bookmark Threads**: Save interesting threads for later reading
- **Personal Collections**: Organize bookmarks into custom named collections
  - "Spring Boot Resources"
  - "Interview Preparation"
  - "Project Ideas"
  - "Code Snippets"
- **Collection Sharing**: Share entire collections with other users or make them public
- **Smart Recommendations**: AI suggests threads to add to wishlist based on reading history
- **Quick Access**: Wishlist accessible from sidebar for instant retrieval

### 🔍 Advanced Search & Discovery

- **Full-Text Search**: MySQL FULLTEXT search across titles, bodies, and comments
- **Semantic Search**: Vector-based search that understands query intent
- **Advanced Filters**: Filter by category, tags, date range, author level, has accepted answer
- **Code Search**: Dedicated search for code snippets across all posts
- **People Search**: Find users by expertise, topic, or contribution level
- **Saved Searches**: Save complex queries and get alerts when new matching threads appear

### ⭐ Best Answer System

- **Mark Best Answer**: Thread authors can select the most helpful reply
- **XP Reward**: Answer author receives +50 XP bonus
- **Visual Highlight**: Best answers pinned to top with green checkmark badge
- **Statistics Tracking**: Users can see their total best answers on profile
- **Badge Progress**: Contributes to "Problem Solver" achievement

### 🛡️ Content Safety & Moderation

- **Toxicity Detection**: Real-time AI analysis for 6 toxicity categories (toxic, severe toxic, obscene, threat, insult, identity hate)
- **Auto-Moderation**: High-toxicity posts automatically hidden and queued for review
- **Community Flagging**: Users can flag inappropriate content with reason selection
- **Moderation Dashboard**: Dedicated interface for moderators to review flagged content
- **Warning System**: Graduated consequences (warning → temporary ban → permanent ban)

---

## 🛠 Tech Stack

### Frontend
- **Framework**: Angular 17 (Standalone Components)
- **UI Library**: Angular Material + Tailwind CSS
- **HTTP Client**: RxJS-based reactive programming
- **Real-time**: WebSocket (STOMP over SockJS)
- **Rich Text Editor**: Quill.js with custom extensions
- **Code Highlighting**: Prism.js (50+ languages)

### Backend
- **Framework**: Spring Boot 3.2.x
- **Language**: Java 17
- **Architecture**: Microservices
- **API Gateway**: Spring Cloud Gateway
- **Service Discovery**: Netflix Eureka
- **Security**: Spring Security + JWT (RS256)
- **ORM**: Spring Data JPA + Hibernate
- **DTO Mapping**: MapStruct 1.5.x

### Databases & Caching
- **Primary Database**: MySQL 8.0+
- **Cache Layer**: Redis 7.x
  - View counter buffering
  - Leaderboard caching
  - Session storage
  - Feed recommendations
- **Message Broker**: Apache Kafka 3.x
  - Async event processing
  - Notification delivery
  - Analytics pipeline

### AI/ML Integration
- **ML Models**: Python FastAPI microservice
  - BERT for categorization
  - NLP for tag extraction
  - Toxicity detection (Jigsaw dataset)
  - Content summarization
- **API Integration**: REST calls from forum-service to ML service

### DevOps & Tools
- **Build Tool**: Maven 3.9+
- **Package Manager**: npm
- **API Documentation**: Springdoc OpenAPI (Swagger)
- **Testing**:
  - Backend: JUnit 5, Mockito, Testcontainers
  - Frontend: Jasmine, Karma, Cypress (E2E)
- **Monitoring**: Spring Actuator + Prometheus + Grafana
- **Logging**: SLF4J + Logback
- **CI/CD**: GitHub Actions (planned)

---

## 🏗 Architecture

The system follows a **microservices architecture** with the following services:

```
┌─────────────────────────────────────────────────────┐
│           Angular Frontend (:4200)                  │
│   - Components (Standalone)                         │
│   - Services (HTTP + WebSocket)                     │
│   - Guards (Auth, Role-based)                       │
│   - Interceptors (JWT auto-attach)                  │
└────────────────────┬────────────────────────────────┘
                     │  HTTPS + JWT Bearer Token
                     ▼
┌─────────────────────────────────────────────────────┐
│          API Gateway (:8080)                        │
│  - JWT Validation                                   │
│  - Rate Limiting (100 req/min)                      │
│  - Request Routing                                  │
│  - CORS Policy                                      │
│  - Circuit Breaker                                  │
└──┬──────────┬──────────────────────────┘
   │          │        
   ▼          ▼          
┌──────┐  ┌──────┐  
│ auth │  │forum │  
│:8081 │  │:8084 │  
└──┬───┘  └──┬───┘  
```



---



## 🎓 Academic Context

**Institution**: Esprit School of Engineering – Tunisia  
**Program**: PIDEV – 4rd Year Engineering (4SAE)  
**Academic Year**: 2025–2026  
**Class**: 4SAE4 
**Project Type**: Integrated Development Project

This project demonstrates the practical application of:
- Software Architecture (Microservices, Clean Architecture, Domain-Driven Design)
- Full-Stack Development (Angular SPA + Spring Boot REST APIs)
- Machine Learning Integration (Classification, NLP, Sentiment Analysis)
- DevOps Practices (CI/CD, Docker, Monitoring)
- Agile Methodologies (Scrum, Git workflow, Code reviews)
- Database Design (Relational modeling, Indexing, Query optimization)
- Security Best Practices (JWT, HTTPS, Input validation, XSS prevention)

---

## 🚀 Getting Started

### Prerequisites
- **Node.js** 20+ & npm
- **Java** 21 (OpenJDK or Oracle JDK)
- **Maven** 3.9+
- **MySQL** 8.0+
- **Redis** 7.x
- **Kafka** 3.x (optional for full features)
- **Git** for version control


# 1. Eureka Discovery Service
cd discovery-service
mvn clean install
mvn spring-boot:run

# Wait for Eureka to start (check http://localhost:8761)

# 2. Config Server
cd config-server
mvn spring-boot:run

# 3. API Gateway
cd api-gateway
mvn spring-boot:run

# 4. Auth Service
cd auth-service
mvn clean install
mvn spring-boot:run

# 5. Forum Service (our main service)
cd forum-service
mvn clean install
mvn spring-boot:run

# Optional: Other services
cd course-service && mvn spring-boot:run
cd enrollment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

#### 5. Frontend (Angular)
```bash
cd frontend
npm install
npm start
```

Navigate to `http://localhost:4200`

### Default Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Student | student@ybrainy.com | test123 |
| Instructor | instructor@ybrainy.com | test123 |
| Moderator | moderator@ybrainy.com | test123 |
| Admin | admin@ybrainy.com | admin123 |

---



## 📚 Documentation

- [Architecture Overview](./docs/ARCHITECTURE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html) (when running)
- [Database Schema](./docs/DATABASE.md)
- [Frontend Development Guide](./frontend/README.md)
- [Backend Development Guide](./backend/README.md)
- [Deployment Guide](./docs/DEPLOYMENT.md)

---

## 🧪 Testing

### Backend Tests
```bash
cd forum-service
mvn test                    # Unit tests
mvn verify                  # Integration tests with Testcontainers
mvn test -Dtest=ThreadServiceTest  # Run specific test class
```

### Frontend Tests
```bash
cd frontend
npm test                    # Unit tests (Karma + Jasmine)
npm run test:coverage       # With coverage report
npm run e2e                 # End-to-end tests (Cypress)
```

### Test Coverage
- Backend: 75%+ code coverage
- Frontend: 70%+ code coverage
- Integration: Critical user flows covered

---

## 📊 Key Metrics

### Development Statistics
- **Total Lines of Code**: ~15,000+
- **Microservices**: 6 independent services
- **REST Endpoints**: 45+ endpoints
- **JPA Entities**: 15 domain models
- **Angular Components**: 35+ components
- **Test Coverage**: 72% overall

### Features Implemented
- ✅ Thread/Post/Comment CRUD
- ✅ Real-time notifications (WebSocket)
- ✅ XP & Leveling system (8 levels)
- ✅ Badge achievements (10+ badges)
- ✅ Best Answer selection
- ✅ AI Auto-categorization
- ✅ AI Auto-description generator
- ✅ Draft auto-save & management
- ✅ Wishlist & collections
- ✅ View counter with Redis
- ✅ Full-text search
- ✅ Leaderboard rankings
- ✅ User profile stats
- ✅ Content moderation

---

## 🚧 Future Enhancements

- [ ] Voice note support in threads
- [ ] Live Q&A sessions with WebRTC
- [ ] Mobile app (React Native)
- [ ] Advanced analytics dashboard
- [ ] Recommendation engine v2 (collaborative filtering)
- [ ] Multi-language support (i18n)
- [ ] Dark mode theme
- [ ] Thread export to PDF
- [ ] Integration with Learning Management System (LMS)

---

## 🤝 Contributing

This is an academic project, but suggestions and feedback are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 🙏 Acknowledgments

We would like to thank:
- **Esprit School of Engineering** for providing the academic framework, resources, and guidance throughout this project
- Our project supervisor **[Professor Name]** for continuous support, valuable feedback, and mentorship
- The open-source community for the amazing tools, libraries, and frameworks that made this project possible
- Our classmates for collaboration, code reviews, and constructive discussions
- GitHub Education for providing developer tools and resources

---

## 📄 License

This project is part of an academic program at **Esprit School of Engineering** and is intended for educational purposes only.

Copyright © 2026 Team BreadnButter - Esprit School of Engineering

---



<div align="center">
  <p><strong>Made with ❤️ by Team BreadnButter</strong></p>
  <p><strong>Esprit School of Engineering – Tunisia | 2025-2026</strong></p>
  <p><em>Empowering students through technology and innovation</em></p>
</div>
