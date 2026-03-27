# TimeSheet & Leave Management System Architecture Diagrams

This file collects high-level and low-level architecture diagrams for the project.
You can paste these Mermaid diagrams into Markdown renderers, Mermaid Live Editor, or presentation tools that support Mermaid.

## 1. High-Level System Architecture

```mermaid
flowchart TD
    U["Users<br/>Employee / Manager / Admin"] --> G["API Gateway"]

    G --> A["auth-service"]
    G --> T["timesheet-service"]
    G --> L["leave-service"]
    G --> AD["admin-service"]

    A <--> E["Eureka Server"]
    T <--> E
    L <--> E
    AD <--> E
    G <--> E
    C["Config Server"] --> A
    C --> T
    C --> L
    C --> AD
    C --> G

    A --> ADB["Auth DB"]
    T --> TDB["Timesheet DB"]
    L --> LDB["Leave DB"]
    AD --> DDB["Admin DB"]

    A <--> R["RabbitMQ"]
    T <--> R
    L <--> R
    AD <--> R
```

## 2. Deployment / Infrastructure View

```mermaid
flowchart LR
    Client["Browser / Swagger / Frontend"] --> Gateway["API Gateway<br/>Routing + JWT filter"]

    subgraph Infra["Platform Services"]
        Eureka["Eureka Server"]
        Config["Config Server"]
        Rabbit["RabbitMQ Broker"]
    end

    subgraph Business["Business Microservices"]
        Auth["auth-service"]
        Time["timesheet-service"]
        Leave["leave-service"]
        Admin["admin-service"]
    end

    Gateway --> Auth
    Gateway --> Time
    Gateway --> Leave
    Gateway --> Admin

    Auth --> Eureka
    Time --> Eureka
    Leave --> Eureka
    Admin --> Eureka
    Gateway --> Eureka

    Config --> Auth
    Config --> Time
    Config --> Leave
    Config --> Admin
    Config --> Gateway

    Auth --> Rabbit
    Time --> Rabbit
    Leave --> Rabbit
    Admin --> Rabbit
```

## 3. Service Ownership

```mermaid
flowchart TD
    A["auth-service"] --> A1["Registration"]
    A --> A2["Login / JWT"]
    A --> A3["Profile"]
    A --> A4["Manager Assignment"]

    T["timesheet-service"] --> T1["Projects"]
    T --> T2["Timesheet Entries"]
    T --> T3["Weekly Timesheets"]
    T --> T4["Submit / Approve / Reject"]

    L["leave-service"] --> L1["Leave Requests"]
    L --> L2["Leave Balances"]
    L --> L3["Policies"]
    L --> L4["Holidays"]

    AD["admin-service"] --> D1["Approval Queue"]
    AD --> D2["Dashboards"]
    AD --> D3["Reports"]
    AD --> D4["Notifications"]
```

## 4. Common Internal Service Pattern

```mermaid
flowchart LR
    Request["HTTP Request"] --> Security["Security Filter / Spring Security"]
    Security --> Controller["Controller"]
    Controller --> Service["Service / ServiceImpl"]
    Service --> Repo["Repository"]
    Repo --> DB["Database"]

    Service --> Feign["Feign Client"]
    Feign --> OtherService["Other Microservice"]

    Service --> Publish["RabbitTemplate Publish"]
    Broker["RabbitMQ"] --> Listener["@RabbitListener"]
    Listener --> Service
    Publish --> Broker
```

## 5. API Gateway Routing View

```mermaid
flowchart TD
    Client["Client Request"] --> Gateway["API Gateway"]

    Gateway -->|" /api/v1/auth/** "| Auth["auth-service"]
    Gateway -->|" /api/v1/timesheets/** "| Time["timesheet-service"]
    Gateway -->|" /api/v1/projects/** "| Time
    Gateway -->|" /api/v1/leave/** "| Leave["leave-service"]
    Gateway -->|" /api/v1/admin/** "| Admin["admin-service"]
```

## 6. Authentication Workflow

```mermaid
sequenceDiagram
    participant U as User
    participant G as API Gateway
    participant A as auth-service
    participant DB as Auth DB

    U->>G: POST /api/v1/auth/login
    G->>A: Forward login request
    A->>DB: Find user by email
    DB-->>A: User record
    A->>A: Validate password and status
    A-->>G: JWT token + user details
    G-->>U: Login response
```

## 7. Timesheet Submission Workflow

```mermaid
sequenceDiagram
    participant U as Employee
    participant G as API Gateway
    participant T as timesheet-service
    participant A as auth-service
    participant MQ as RabbitMQ
    participant AD as admin-service
    participant DBT as Timesheet DB
    participant DBA as Admin DB

    U->>G: POST /api/v1/timesheets/submit
    G->>T: Forward with JWT headers
    T->>DBT: Load timesheet
    T->>T: Validate entries and status
    T->>A: Get manager id for employee
    A-->>T: approverId
    T->>DBT: Save timesheet as SUBMITTED
    T->>MQ: Publish TimesheetSubmittedEvent
    MQ->>AD: Deliver to admin.timesheet.queue
    AD->>DBA: Create ApprovalTask
```

## 8. Leave Request Workflow

```mermaid
sequenceDiagram
    participant U as Employee
    participant G as API Gateway
    participant L as leave-service
    participant A as auth-service
    participant MQ as RabbitMQ
    participant AD as admin-service
    participant DBL as Leave DB
    participant DBA as Admin DB

    U->>G: POST /api/v1/leave/requests
    G->>L: Forward with JWT headers
    L->>L: Validate dates, overlap, balance
    L->>A: Get manager id for employee
    A-->>L: approverId
    L->>DBL: Save leave request
    L->>DBL: Update pending balance
    L->>MQ: Publish LeaveRequestedEvent
    MQ->>AD: Deliver to admin.leave.queue
    AD->>DBA: Create ApprovalTask
```

## 9. Approval Completion Workflow

```mermaid
sequenceDiagram
    participant M as Manager
    participant G as API Gateway
    participant AD as admin-service
    participant DBA as Admin DB
    participant MQ as RabbitMQ
    participant T as timesheet-service
    participant L as leave-service
    participant DBT as Timesheet DB
    participant DBL as Leave DB

    M->>G: Approve / Reject task
    G->>AD: Approval action request
    AD->>DBA: Update ApprovalTask status
    AD->>MQ: Publish ApprovalCompletedEvent

    MQ->>T: approval.completed
    T->>T: Filter targetType == TIMESHEET
    T->>DBT: Update timesheet status

    MQ->>L: approval.completed
    L->>L: Filter targetType == LEAVE
    L->>DBL: Update leave request and balances
```

## 10. RabbitMQ High-Level View

```mermaid
flowchart LR
    TS["timesheet-service"] -->|"TimesheetSubmittedEvent"| EX1["timesheet.exchange"]
    LS["leave-service"] -->|"LeaveRequestedEvent"| EX2["leave.exchange"]
    AS["auth-service"] -->|"UserRegisteredEvent"| EX3["notification.exchange"]

    EX1 --> Q1["admin.timesheet.queue"]
    EX2 --> Q2["admin.leave.queue"]
    EX3 --> Q3["notification.user.registered.queue"]

    Q1 --> AD["admin-service"]
    Q2 --> AD
    Q3 --> AD

    AD -->|"ApprovalCompletedEvent"| EX4["admin.exchange"]
    EX4 --> Q4["timesheet.approval.completed"]
    EX4 --> Q5["leave.approval.completed"]

    Q4 --> TS
    Q5 --> LS
```

## 11. RabbitMQ Leave Approval Low-Level View

```mermaid
flowchart TD
    A["LeaveController.requestLeave"] --> B["LeaveServiceImpl.requestLeave"]
    B --> C["Validate request"]
    C --> D["Find manager through AuthServiceClient"]
    D --> E["Save LeaveRequest"]
    E --> F["Update LeaveBalance.pending"]
    F --> G["Create LeaveRequestedEvent"]
    G --> H["RabbitTemplate.convertAndSend(leave.exchange, leave.requested, event)"]
    H --> I["RabbitMQ Exchange: leave.exchange"]
    I --> J["Binding: admin.leave.queue <- leave.requested"]
    J --> K["Queue: admin.leave.queue"]
    K --> L["EventConsumer.handleLeaveRequested"]
    L --> M["Create ApprovalTask"]
    M --> N["Save ApprovalTask in Admin DB"]
```

## 12. RabbitMQ Timesheet Approval Low-Level View

```mermaid
flowchart TD
    A["TimesheetController.submitTimesheet"] --> B["TimesheetServiceImpl.submitTimesheet"]
    B --> C["Validate timesheet"]
    C --> D["Get manager through AuthServiceClient"]
    D --> E["Save Timesheet as SUBMITTED"]
    E --> F["Create TimesheetSubmittedEvent"]
    F --> G["RabbitTemplate.convertAndSend(timesheet.exchange, timesheet.submitted, event)"]
    G --> H["RabbitMQ Exchange: timesheet.exchange"]
    H --> I["Binding: admin.timesheet.queue <- timesheet.submitted"]
    I --> J["Queue: admin.timesheet.queue"]
    J --> K["EventConsumer.handleTimesheetSubmitted"]
    K --> L["Create ApprovalTask"]
    L --> M["Save ApprovalTask in Admin DB"]
```

## 13. Notification Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as auth-service
    participant MQ as RabbitMQ
    participant AD as admin-service
    participant Mail as WelcomeEmailService

    U->>A: Register
    A->>A: Save user
    A->>MQ: Publish UserRegisteredEvent
    MQ->>AD: Deliver to notification.user.registered.queue
    AD->>Mail: sendWelcomeEmail(event)
    Mail->>Mail: buildBody(event)
    Mail-->>U: Welcome email
```

## 14. Database Ownership Diagram

```mermaid
flowchart LR
    Auth["auth-service"] --> AuthDB["users"]
    Time["timesheet-service"] --> TimeDB["projects<br/>timesheets<br/>timesheet_entries"]
    Leave["leave-service"] --> LeaveDB["leave_requests<br/>leave_balances<br/>leave_policies<br/>holidays"]
    Admin["admin-service"] --> AdminDB["approval_tasks<br/>policy/report config"]
```

## 15. Low-Level Timesheet Service Internal Structure

```mermaid
flowchart TD
    Controller["TimesheetController"] --> Service["TimesheetServiceImpl"]
    Service --> TimesheetRepo["TimesheetRepository"]
    Service --> EntryRepo["TimesheetEntryRepository"]
    Service --> ProjectRepo["ProjectRepository"]
    Service --> AuthClient["AuthServiceClient"]
    Service --> Rabbit["RabbitTemplate"]
    Service --> Entity["Timesheet / TimesheetEntry / Project"]
    Service --> DTO["Request / Response DTOs"]
```

## 16. Low-Level Leave Service Internal Structure

```mermaid
flowchart TD
    Controller["LeaveController"] --> Service["LeaveServiceImpl"]
    Service --> ReqRepo["LeaveRequestRepository"]
    Service --> BalRepo["LeaveBalanceRepository"]
    Service --> PolicyRepo["LeavePolicyRepository"]
    Service --> HolidaySvc["HolidayService"]
    Service --> AuthClient["AuthServiceClient"]
    Service --> Rabbit["RabbitTemplate"]
    Service --> Entity["LeaveRequest / LeaveBalance / LeavePolicy"]
    Service --> DTO["Leave DTOs / Event DTOs"]
```

## 17. Low-Level Admin Service Internal Structure

```mermaid
flowchart TD
    ApprovalController["AdminApprovalController"] --> ApprovalService["ApprovalServiceImpl"]
    ApprovalService --> TaskRepo["ApprovalTaskRepository"]
    ApprovalService --> Rabbit["RabbitTemplate"]

    EventConsumer["EventConsumer"] --> TaskRepo
    EventConsumer --> Mail["WelcomeEmailService"]
    Mail --> MailSender["JavaMailSender"]
```

## 18. Presentation Tip

Use these diagrams in this order during explanation:

1. High-Level System Architecture
2. Service Ownership
3. API Gateway Routing View
4. Authentication Workflow
5. Leave Workflow
6. Timesheet Workflow
7. RabbitMQ High-Level View
8. One low-level workflow diagram
9. Database Ownership

