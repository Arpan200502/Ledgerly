# Software Requirements Specification (SRS)
## Ledgerly — AI-Powered Voice Ledger Platform for Small Business Owners

**Version:** 1.0
**Prepared for:** DBMS Course Project, MCA — VIT Vellore
**Document purpose:** Single source of truth for scope, architecture, schema, and phased build plan. Written to be read by both a human developer and an AI coding agent (e.g. OpenCode) as an implementation spec.

---

## 1. Product Overview

**Name:** Ledgerly
**Tagline (product-facing):** Talk it. Track it.
**Tagline (technical/report-facing):** An AI-powered voice copilot that turns natural language into safe, sandboxed SQL.

Ledgerly is a multi-tenant SaaS platform that lets small business owners (kirana stores, clinics, tailors, small shops) manage their customer ledger ("khata"), record sales and credit, collect payments, and query their own business data — all through natural voice conversation in Hindi/English/Hinglish, backed by a real relational database and enforced data-safety guardrails.

### 1.1 Problem Statement
Small business owners in India overwhelmingly track customer credit ("udhaar"), daily sales, and expenses in a handwritten notebook. Existing digital ledger apps require manual form-based data entry, which is slower than speech and inaccessible to less literate or less tech-comfortable users. There is no product that lets an owner simply *ask* their business data a question, or *say* a transaction into existence, while guaranteeing the underlying data stays safe, correctly scoped, and auditable.

### 1.2 Target Users
- **Primary persona:** Small shop owner (kirana store, general store) managing 20–200 regular customers on credit.
- **Secondary personas:** Small clinics (patient billing/credit), tailors, and other small service businesses with a repeat-customer credit model.
- Users are assumed to be comfortable with smartphones but not necessarily comfortable with typing-heavy apps or English-only interfaces.

### 1.3 Product Vision (Ecosystem)
Ledgerly is architected as a core engine with modules around it, following the real-world evolution path of products like OkCredit/Khatabook (ledger → payments → ecosystem):

| Layer | Status this semester |
|---|---|
| Voice Ledger Core (SQL copilot + khata engine) | **Build** |
| Payments Layer (UPI links + reconciliation) | **Build** |
| Inventory Module (stock tied to sales) | **Build** |
| Customer Engagement (reminders, self-view, loyalty) | **Build** |
| Business Insights (analytics, cash-flow health) | **Build** |
| Compliance Layer (GST-ready invoicing) | Vision — document only |
| Credit & Lending (ledger data as credit signal) | Vision — document only |

---

## 2. Scope

### 2.1 In Scope (this semester)
- Multi-tenant SaaS architecture (each shop owner = isolated tenant)
- Voice-driven read queries (natural language → SQL → spoken answer)
- Voice-driven write actions (record sale/credit/payment via structured intent extraction, not raw SQL)
- Guardrail layer enforcing SELECT-only AI-generated queries, schema allowlisting, and tenant scoping
- Multi-turn clarification for ambiguous queries
- UPI payment link generation + manual/gateway-based reconciliation
- Inventory tracking tied to sales
- WhatsApp payment reminders (via Twilio Sandbox for demo purposes)
- Analytics dashboard (top debtors, sales trend, cash-flow overview)
- React SPA frontend with mic input, dashboard, and manual-entry fallback

### 2.2 Out of Scope (documented as future vision only)
- GST-compliant invoice generation / tax compliance
- Any lending, credit-scoring, or financial-services functionality
- Production-grade Meta WhatsApp Business API verification
- Offline-first / sync-conflict handling
- Multi-language beyond Hindi/English/Hinglish (extensible design, not built)

---

## 3. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Backend framework | Spring Boot 3.x (Java 17) | REST controllers, services, JPA |
| ORM / DB access | Spring Data JPA + Hibernate | For core entities (CRUD path) |
| Read-only AI query execution | Raw JDBC, separate read-only `DataSource` | Bypasses JPA entity layer for AI-generated SELECTs |
| SQL AST parsing / validation | JSqlParser | Guardrail statement-type + schema checks |
| Database | PostgreSQL (preferred) or MySQL | Postgres preferred for `statement_timeout` support |
| Frontend | React (SPA) | Separate from backend, calls REST API |
| Charts | Recharts | Analytics dashboard |
| LLM (SQL generation, intent extraction, summarization) | Groq API | Called via Spring `WebClient`/`RestTemplate` |
| Speech-to-text | Sarvam AI STT | Hindi/English/Hinglish support |
| Text-to-speech | Sarvam AI TTS | Spoken responses and confirmations |
| Payment links | Direct UPI deep link + QR (primary) / Razorpay Payment Links API sandbox (stretch) | See Section 7.2 |
| WhatsApp notifications | Twilio WhatsApp Sandbox | Free tier, join-code opt-in for demo |
| Auth | Spring Security + JWT | Shop owner login, tenant identity |
| Build tool | Maven | Standard Spring Initializr default |
| Version control | Git + GitHub | |
| API testing | Postman | |
| IDE | VS Code (Extension Pack for Java + Spring Boot Extension Pack) | |

---

## 4. System Architecture

### 4.1 High-Level Flow (Read Path)
```
Voice input (React mic capture)
  -> Sarvam STT -> raw text
  -> Groq (question + shop's schema context) -> candidate SQL + confidence flag
  -> IF ambiguous: Groq returns clarification question -> spoken back -> user answers -> re-sent with full context (max 3 turns)
  -> Guardrail layer:
       1. Parse SQL to AST (JSqlParser)
       2. Statement type check (SELECT only; reject multi-statement)
       3. Schema allowlist check (tables/columns must exist and be permitted)
       4. Inject mandatory tenant scope: AND shop_id = :current_shop_id
       5. Wrap with row LIMIT + statement timeout
  -> Execute on read-only, tenant-scoped DataSource
  -> Result rows -> Groq summarization call -> plain-language answer
  -> Sarvam TTS -> spoken response
```

### 4.2 High-Level Flow (Write Path)
```
Voice input -> Sarvam STT -> raw text
  -> Groq intent classification: READ vs WRITE
  -> IF WRITE:
       -> Groq extracts structured parameters (customer_name, amount, type) — NOT SQL
       -> Spoken confirmation generated and read back to user
       -> User confirms (voice) or rejects
       -> IF confirmed: execute one of a small fixed set of pre-written,
          parameterized actions (add_transaction, record_payment, add_customer)
          inside a single @Transactional method (insert record + update balance atomically)
       -> IF rejected: discard, optionally re-prompt
```

### 4.3 Multi-Tenancy Model
- Every business-data table carries a `shop_id` foreign key.
- Tenant scope is enforced at two levels: (a) JWT-derived `shop_id` injected into every JPA query via a base repository/service pattern, and (b) AST-level injection for AI-generated SQL (guardrail layer). Enforcement is structural, never dependent on the LLM remembering to scope itself.

---

## 5. Functional Requirements by Module

### 5.1 Voice Ledger Core
- FR-1.1: System shall accept voice input and convert to text via Sarvam STT.
- FR-1.2: System shall classify each utterance as READ or WRITE intent.
- FR-1.3: System shall generate SQL for READ intents using Groq, given the current shop's schema.
- FR-1.4: System shall validate all AI-generated SQL through the guardrail layer before execution (see Section 7.1).
- FR-1.5: System shall support multi-turn clarification (max 3 turns) when a query term is ambiguous (e.g. "regulars," "recent").
- FR-1.6: System shall summarize query results into natural language and speak them back via Sarvam TTS.
- FR-1.7: System shall extract structured parameters (not SQL) for WRITE intents and require spoken confirmation before committing.
- FR-1.8: System shall support "undo last entry" via voice, implemented as a reversal action, not a hard delete.
- FR-1.9: System shall support correcting a prior entry by voice ("last entry was 400, not 500") with confirmation.

### 5.2 Payments Layer
- FR-2.1: System shall generate a UPI deep link and QR code for a given customer's outstanding balance.
- FR-2.2: System shall allow the shop owner to mark a payment request as settled by voice, matched against pending requests for the named customer.
- FR-2.3 (stretch): System shall support Razorpay Payment Links API (sandbox/test mode) with webhook-based automatic reconciliation, signature-verified.
- FR-2.4: Every settlement shall update the ledger and customer balance atomically.

### 5.3 Inventory Module
- FR-3.1: System shall allow products to be defined per shop (name, unit, current stock).
- FR-3.2: Recording a sale via voice or manual entry shall decrement linked product stock atomically with the transaction insert.
- FR-3.3: System shall prevent stock from going negative; if a sale would exceed available stock, the write shall be rejected with a spoken/visual warning.

### 5.4 Customer Engagement
- FR-4.1: System shall identify customers with overdue balances past a configurable threshold (days).
- FR-4.2: System shall send a WhatsApp reminder to overdue customers via Twilio Sandbox.
- FR-4.3: System shall provide a customer self-view link/page showing their own balance without requiring shop owner mediation.
- FR-4.4 (stretch): System shall support a credit limit per customer, with a spoken warning if a new credit entry would breach it.

### 5.5 Business Insights
- FR-5.1: System shall provide a dashboard showing: top debtors by balance, monthly sales trend, credit-given vs payments-received trend.
- FR-5.2: System shall support an on-demand voice daily/weekly summary ("aaj ka business kaisa raha").
- FR-5.3: System shall expose a full audit trail (history) per customer, showing every transaction with timestamp and source (voice/manual).

### 5.6 SaaS / Tenancy
- FR-6.1: System shall support shop owner sign-up, creating an isolated tenant (`shops` record).
- FR-6.2: System shall authenticate shop owners via JWT-based login (Spring Security).
- FR-6.3: System shall support tiered access: Free (manual entry, basic ledger) vs Pro (voice, UPI links, WhatsApp reminders, analytics).

---

## 6. Non-Functional Requirements

- NFR-1 (Security): No AI-generated query shall ever execute a non-SELECT statement. Enforced at parser level, not by prompt instruction alone.
- NFR-2 (Isolation): No tenant shall be able to access another tenant's data under any query path, including AI-generated ones.
- NFR-3 (Auditability): Every write (voice or manual) shall be logged with timestamp, source, and (for voice) the original utterance transcript.
- NFR-4 (Resilience): AI-generated queries shall run under a row limit and a database statement timeout to prevent runaway queries.
- NFR-5 (Usability): The system shall support Hindi, English, and Hinglish input/output.
- NFR-6 (Data integrity): Any operation touching balance (sale, credit, payment) shall be wrapped in a single atomic database transaction.
- NFR-7 (Availability of fallback): All voice-driven actions shall have an equivalent manual UI path, so the product remains usable if voice fails or is unavailable.

---

## 7. Detailed Design Specs

### 7.1 Guardrail Layer (SELECT-only enforcement)
1. **Parse to AST** using JSqlParser — never regex.
2. **Statement type check**: reject anything whose top-level statement is not `SELECT`; reject multi-statement strings (semicolon-separated) outright.
3. **Schema allowlist check**: every table/column referenced in the AST must exist in a whitelist generated from the actual shop schema. Reject references to any table outside the allowed set (e.g. internal admin tables).
4. **Tenant scope injection**: programmatically modify the AST's WHERE clause to force `AND shop_id = :current_shop_id`, then regenerate the SQL string from the modified AST. This must never rely on the LLM including this clause itself.
5. **Limit + timeout wrapping**: inject a `LIMIT 500` if absent; execute under a DB-level `statement_timeout` (Postgres) of a few seconds.
6. **Execution**: run only on a connection pool backed by a read-only DB user (no INSERT/UPDATE/DELETE/DDL grants at the database level — defense in depth beyond the application layer).
7. **Rejection handling**: any failed check returns a structured reason to the SQL-generation step for an informed retry, capped at 3 attempts total.

### 7.2 Write Lane (Structured Actions, Not Generated SQL)
- LLM output for WRITE intent is a JSON object matching one of a small fixed set of action schemas, e.g.:
```json
{ "action": "add_transaction", "customer_name": "Sharma", "amount": 500, "type": "credit_given" }
```
- Each action name maps to exactly one hand-written, parameterized query/service method — the LLM never generates SQL for writes.
- Before execution, the system speaks back the intended action for confirmation.
- On confirmation, the write executes inside a single `@Transactional` service method that (a) inserts the ledger row and (b) updates the customer's running balance (and stock, if applicable) together.
- Business rules (amount must be positive, customer must exist or trigger explicit "new customer" confirmation, stock must not go negative) are enforced in this service method in ordinary Java code — not left to the LLM.

### 7.3 Multi-Turn Clarification
- A `conversation_sessions` record is created on first ambiguity, storing the original question.
- Each clarification round is stored as a linked `clarification_turns` row (question asked, user's answer).
- Sessions have a TTL (e.g. 60 seconds of inactivity expires the session).
- Max 3 clarification turns; on exceeding, the system fails gracefully ("I'm having trouble understanding — try rephrasing") rather than looping indefinitely.

### 7.4 UPI Payment Reconciliation
- **Primary (no gateway):** generate `upi://pay?pa=<vpa>&pn=<name>&am=<amount>&cu=INR&tn=<note>` and render as QR (e.g. `qrcode` library). Reconciliation is manual — owner voice-confirms payment, matched against the customer's pending `payment_requests` row, then settled atomically.
- **Stretch (gateway):** Razorpay Payment Links API (test/sandbox mode) create a link, store `gateway_ref` on the `payment_requests` row; Razorpay webhook (signature-verified) marks it settled automatically.

---

## 8. Database Schema

```sql
-- Tenant / shop
CREATE TABLE shops (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  owner_name VARCHAR(255) NOT NULL,
  phone VARCHAR(20) UNIQUE NOT NULL,
  upi_vpa VARCHAR(255),
  business_type VARCHAR(50), -- kirana, clinic, tailor, other
  tier VARCHAR(20) DEFAULT 'free', -- free, pro
  created_at TIMESTAMP DEFAULT NOW()
);

-- Shop owner / staff login
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) DEFAULT 'owner', -- owner, staff
  created_at TIMESTAMP DEFAULT NOW()
);

-- Customers of a shop
CREATE TABLE customers (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  balance NUMERIC(12,2) DEFAULT 0, -- positive = customer owes shop
  credit_limit NUMERIC(12,2),
  created_at TIMESTAMP DEFAULT NOW()
);

-- Products (inventory module)
CREATE TABLE products (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  name VARCHAR(255) NOT NULL,
  unit VARCHAR(50),
  stock_quantity NUMERIC(12,2) DEFAULT 0,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Ledger transactions
CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  product_id BIGINT REFERENCES products(id), -- nullable, only for sales
  quantity NUMERIC(12,2),
  amount NUMERIC(12,2) NOT NULL,
  type VARCHAR(30) NOT NULL, -- credit_given, payment_received, sale, expense, reversal
  source VARCHAR(20) DEFAULT 'manual', -- voice, manual
  original_transcript TEXT, -- raw voice utterance, if source = voice
  reversed_transaction_id BIGINT REFERENCES transactions(id), -- for undo
  created_at TIMESTAMP DEFAULT NOW()
);

-- UPI / gateway payment requests
CREATE TABLE payment_requests (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  amount NUMERIC(12,2) NOT NULL,
  upi_link TEXT,
  qr_code_url TEXT,
  gateway_ref VARCHAR(255), -- Razorpay payment_link_id, null for direct UPI
  status VARCHAR(20) DEFAULT 'pending', -- pending, settled, expired
  created_at TIMESTAMP DEFAULT NOW(),
  settled_at TIMESTAMP
);

-- Voice clarification sessions
CREATE TABLE conversation_sessions (
  id BIGSERIAL PRIMARY KEY,
  shop_id BIGINT NOT NULL REFERENCES shops(id),
  original_question TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'open', -- open, resolved, expired, failed
  resolved_sql TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  expires_at TIMESTAMP
);

CREATE TABLE clarification_turns (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES conversation_sessions(id),
  turn_number INT NOT NULL,
  question_asked TEXT NOT NULL,
  user_answer TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Recommended indexes
CREATE INDEX idx_transactions_customer_created ON transactions(customer_id, created_at);
CREATE INDEX idx_transactions_shop ON transactions(shop_id);
CREATE INDEX idx_customers_shop ON customers(shop_id);
CREATE INDEX idx_payment_requests_status ON payment_requests(shop_id, status);
```

---

## 9. API Endpoint Specification (Draft)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/signup` | Create shop + owner account |
| POST | `/api/auth/login` | Authenticate, return JWT |
| POST | `/api/voice/query` | Voice/text in (read path) -> spoken/text answer out |
| POST | `/api/voice/action` | Voice/text in (write path) -> confirmation prompt |
| POST | `/api/voice/action/confirm` | Confirm/reject a pending write action |
| GET | `/api/customers` | List customers for current shop |
| POST | `/api/customers` | Add customer manually |
| GET | `/api/customers/{id}/history` | Full transaction history (audit trail) |
| POST | `/api/transactions` | Manual transaction entry (fallback UI) |
| POST | `/api/payment-requests` | Generate UPI link/QR for a customer |
| POST | `/api/payment-requests/{id}/settle` | Manually mark settled |
| POST | `/webhooks/razorpay` | Gateway webhook (stretch feature) |
| GET | `/api/dashboard/summary` | Analytics: top debtors, trends |
| GET | `/api/products` | List inventory items |
| POST | `/api/products` | Add/update product |
| POST | `/api/reminders/overdue` | Trigger WhatsApp reminders for overdue customers |

---

## 10. Development Phases (Semester Plan)

| Phase | Weeks | Goal | Key Deliverable |
|---|---|---|---|
| 1 | 1–2 | Java → Spring Boot fundamentals | Throwaway CRUD app (Customer entity) working end to end |
| 2 | 3–4 | Core khata schema + CRUD | Real entities (`shops`, `customers`, `transactions`) with REST endpoints, testable in Postman |
| 3 | 4–5 (overlap) | React frontend wired to backend | Dashboard: list customers, view balances, manual transaction form |
| 4 | 6–7 | Groq integration, read-lane SQL generation | Natural language → SQL working for simple queries (no guardrail yet) |
| 5 | 8–9 | Guardrail layer | JSqlParser validation, tenant scope injection, read-only sandboxed datasource — core DBMS depth section |
| 6 | 10–11 | Voice (Sarvam) + write lane + confirmation | Mic input on frontend, full voice read+write loop with spoken confirmation |
| 7 | 12+ | Polish, extra features, report | UPI link, WhatsApp reminders, analytics; final report and demo script |

---

## 11. Assumptions & Constraints
- Development and demo will use test/sandbox credentials for all third-party services (Groq, Sarvam, Twilio, Razorpay) — no production business verification will be pursued this semester.
- WhatsApp reminders use Twilio Sandbox (free, requires one-time recipient opt-in via join code) rather than the official Meta Cloud API, due to multi-day business verification overhead being disproportionate for a course project.
- Single database instance (Postgres or MySQL) is assumed sufficient; no read-replica/horizontal scaling is in scope.
- Compliance (GST/invoicing) and Credit/Lending layers are documented as future vision only and are not implemented.

---

## 12. Glossary
- **Khata**: Traditional handwritten ledger book used by Indian shopkeepers to track customer credit.
- **Udhaar**: Credit/amount owed by a customer.
- **VPA**: Virtual Payment Address, a UPI identifier (e.g. `name@bank`).
- **Guardrail layer**: The validation stage between AI-generated SQL and database execution that enforces safety (statement type, schema allowlist, tenant scoping).
- **Tenant**: A single shop/business account, isolated from all others in a multi-tenant system.
- **AST**: Abstract Syntax Tree — structured representation of parsed SQL used for programmatic validation.

---

*End of document. This file is intended to be handed directly to an AI coding agent as an implementation reference alongside human review.*
