# Ledgerly Development Phases
## Learning Java, Spring Boot, and SQL

This document breaks down the Ledgerly project into learning phases, each focusing on specific concepts and deliverables. Follow these phases sequentially to build the project while learning.

---

## Prerequisites
- Java 17+ installed
- Maven installed
- PostgreSQL or MySQL installed
- IDE (VS Code with Java extensions, or IntelliJ IDEA)
- Git installed
- Basic understanding of REST APIs and HTTP

---

## Phase 1: Spring Boot Fundamentals (Weeks 1-2)
**Goal:** Create a simple CRUD application to understand Spring Boot basics.

### Learning Objectives:
- Understand Spring Boot project structure
- Learn about annotations (`@SpringBootApplication`, `@RestController`, `@Service`, `@Repository`)
- Create REST endpoints (GET, POST, PUT, DELETE)
- Connect to a database using Spring Data JPA
- Understand entity relationships and repository pattern

### Tasks:
1. Initialize Spring Boot project using Spring Initializr (add dependencies: Spring Web, Spring Data JPA, PostgreSQL/MySQL Driver)
2. Create a simple `Customer` entity with fields: id, name, email, phone
3. Create a `CustomerRepository` interface extending `JpaRepository`
4. Create a `CustomerService` class with CRUD methods
5. Create a `CustomerController` with REST endpoints
6. Configure database connection in `application.properties`
7. Test endpoints using Postman or curl

### Key Concepts:
- Spring Boot auto-configuration
- Dependency injection
- JPA annotations (`@Entity`, `@Id`, `@GeneratedValue`)
- Repository pattern
- RESTful design

---

## Phase 2: Core Schema & Multi-Tenancy (Weeks 3-4)
**Goal:** Implement the core database schema and multi-tenant architecture.

### Learning Objectives:
- Design and implement relational database schema
- Understand foreign keys and relationships
- Implement multi-tenancy with `shop_id`
- Learn about database migrations (optional: Flyway/Liquibase)
- Understand transaction management

### Tasks:
1. Create SQL migration file with all tables from SRS (shops, users, customers, products, transactions, payment_requests, conversation_sessions, clarification_turns)
2. Create corresponding JPA entities for each table
3. Implement `shop_id` scoping in repositories (using `@Query` or Spring Data JPA specifications)
4. Create CRUD endpoints for shops, customers, products, and transactions
5. Implement transaction rollback for failed operations
6. Add database indexes for performance

### Key Concepts:
- Database normalization
- One-to-many relationships
- Composite keys and foreign keys
- Multi-tenancy patterns
- ACID properties

---

## Phase 3: React Frontend Integration (Weeks 4-5)
**Goal:** Build a simple React frontend to interact with the backend.

### Learning Objectives:
- Understand REST API consumption in React
- Learn about React components and state management
- Implement forms for data entry
- Display data in tables and lists

### Tasks:
1. Initialize React project with Vite or Create React App
2. Create components for:
   - Customer list (GET /api/customers)
   - Customer form (POST /api/customers)
   - Transaction form (POST /api/transactions)
   - Dashboard with basic charts (using Recharts)
3. Implement API service layer with fetch or axios
4. Add basic styling (CSS or Tailwind)
5. Handle loading states and errors

### Key Concepts:
- React hooks (useState, useEffect)
- Component lifecycle
- API integration
- State management
- Responsive design

---

## Phase 4: AI Integration - Read Lane (Weeks 6-7)
**Goal:** Integrate Groq API for natural language to SQL conversion.

### Learning Objectives:
- Understand REST API integration with external services
- Learn about WebClient/RestTemplate in Spring Boot
- Implement prompt engineering for SQL generation
- Handle API errors and rate limits

### Tasks:
1. Add Spring WebFlux dependency for WebClient
2. Create `GroqService` to call Groq API
3. Implement prompt template that includes:
   - Current shop's schema (table names, column names)
   - User's natural language question
   - Instructions for SQL generation
4. Create endpoint `POST /api/voice/query` that:
   - Accepts natural language input
   - Calls Groq to generate SQL
   - Returns generated SQL (for testing)
5. Test with simple queries like "Show all customers" or "What is John's balance?"

### Key Concepts:
- HTTP clients in Spring Boot
- JSON serialization/deserialization
- API key management
- Error handling
- Prompt engineering basics

---

## Phase 5: Guardrail Layer & Security (Weeks 8-9)
**Goal:** Implement SQL validation and security guardrails.

### Learning Objectives:
- Understand SQL parsing and AST (Abstract Syntax Tree)
- Learn about JSqlParser library
- Implement input validation and sanitization
- Understand database security (read-only users, permissions)

### Tasks:
1. Add JSqlParser dependency
2. Create `SqlValidatorService` that:
   - Parses SQL to AST
   - Checks statement type (SELECT only)
   - Validates table/column names against allowlist
   - Injects tenant scope (`AND shop_id = :current_shop_id`)
   - Adds row LIMIT if missing
3. Create read-only database user in PostgreSQL/MySQL
4. Configure separate DataSource for AI queries
5. Implement `statement_timeout` for queries
6. Add logging for all AI-generated queries

### Key Concepts:
- SQL parsing and validation
- Security best practices
- Database permissions
- Input sanitization
- Defense in depth

---

## Phase 6: Voice Integration & Write Lane (Weeks 10-11)
**Goal:** Implement voice input/output and write actions with confirmation.

### Learning Objectives:
- Understand speech-to-text and text-to-speech integration
- Learn about structured action extraction (not SQL generation)
- Implement confirmation flows
- Understand atomic transactions

### Tasks:
1. Integrate Sarvam AI STT API for voice-to-text
2. Integrate Sarvam AI TTS API for text-to-voice
3. Create `VoiceService` that:
   - Converts voice to text
   - Classifies intent (READ/WRITE)
   - For WRITE: extracts structured parameters (JSON)
   - For READ: uses existing read lane
4. Implement write actions:
   - `add_transaction` (credit given, payment received)
   - `add_customer`
   - `record_payment`
5. Create confirmation flow:
   - Speak back the intended action
   - Wait for voice confirmation
   - Execute on confirmation
6. Implement atomic transactions for balance updates

### Key Concepts:
- External API integration
- Intent classification
- Structured data extraction
- Transaction management
- Voice user interface (VUI) design

---

## Phase 7: Polish & Advanced Features (Weeks 12+)
**Goal:** Add remaining features and prepare for demo.

### Learning Objectives:
- Implement UPI payment links
- Add WhatsApp notifications
- Create analytics dashboard
- Understand production deployment basics

### Tasks:
1. **UPI Integration:**
   - Generate UPI deep links (`upi://pay?...`)
   - Create QR codes (using library like ZXing)
   - Implement manual settlement flow

2. **WhatsApp Reminders:**
   - Integrate Twilio WhatsApp Sandbox
   - Create reminder templates
   - Schedule reminders for overdue customers

3. **Analytics Dashboard:**
   - Implement endpoints for:
     - Top debtors
     - Monthly sales trends
     - Credit vs payments
   - Create charts with Recharts

4. **Final Touches:**
   - Add input validation
   - Improve error messages
   - Add loading indicators
   - Write unit tests
   - Prepare demo script

### Key Concepts:
- Payment gateway integration
- Third-party API integration
- Data visualization
- Testing strategies
- Deployment preparation

---

## Resources
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- JSqlParser: https://github.com/JSQLParser/JSqlParser
- Groq API: https://groq.com/
- Sarvam AI: https://sarvam.ai/
- Twilio WhatsApp: https://www.twilio.com/docs/whatsapp
- PostgreSQL Documentation: https://www.postgresql.org/docs/

---

## Tips for Learning
1. **Start small:** Get each phase working before moving to the next
2. **Test frequently:** Use Postman to test API endpoints
3. **Read errors carefully:** Spring Boot provides detailed error messages
4. **Use version control:** Commit your code regularly
5. **Ask for help:** Use documentation, Stack Overflow, or AI assistants
6. **Practice SQL:** Write queries manually to understand what the AI generates

---

*This file will be updated as the project progresses.*