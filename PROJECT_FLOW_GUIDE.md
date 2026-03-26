# TimeSheet & Leave Management System Flow Guide

## 1. Project Overview

This project is a Spring Boot microservices-based TimeSheet and Leave Management System.

It is split into separate services so that authentication, leave management, timesheet management, admin approvals, and routing can evolve independently.

Main modules in this repository:

- `EurekaServer` - service discovery
- `Config-Server` - centralized configuration server
- `api-gateway` - single entry point for client requests
- `auth-service` - user registration, login, profile, roles, manager assignment
- `timesheet-service` - projects, timesheet entries, weekly submission
- `leave-service` - leave policies, balances, leave requests, holidays
- `admin-service` - approval inbox, approval actions, dashboard, reporting
- `common` - shared classes/utilities used across services

## 2. Main Technologies Used

- Java 21
- Spring Boot 3.3.4
- Spring Cloud
- Spring Cloud Gateway
- Eureka Service Discovery
- Spring Cloud Config Server
- Spring Data JPA
- MySQL
- RabbitMQ
- OpenFeign
- Resilience4j Circuit Breaker
- OpenAPI / Swagger
- Docker
- GitHub Actions
- Kubernetes for already-running infrastructure in your environment

## 3. High-Level Architecture

The runtime flow is:

1. Client calls the `api-gateway`
2. `api-gateway` validates JWT and forwards requests
3. Services register with Eureka and communicate using service discovery
4. Services fetch configuration from Config Server on startup
5. Services store their own data in separate MySQL databases
6. RabbitMQ is used for asynchronous approval flows

## 4. What Each Service Does

### 4.1 Eureka Server

Purpose:

- keeps track of running services
- allows services to call each other using names like `lb://auth-service`

Used by:

- `auth-service`
- `timesheet-service`
- `leave-service`
- `admin-service`
- `api-gateway`

### 4.2 Config Server

Purpose:

- stores centralized configuration
- provides datasource, RabbitMQ, Zipkin, and other environment-specific settings

Important:

- when services run in Docker and MySQL/RabbitMQ/Zipkin run on your machine, config must use `host.docker.internal`, not `localhost`

### 4.3 API Gateway

Purpose:

- single external entry point
- routes traffic to internal services
- validates JWT
- adds user headers like `X-User-Id`, `X-User-Email`, `X-User-Role`
- exposes aggregated Swagger via gateway paths

Main routes:

- `/api/v1/auth/**` -> `auth-service`
- `/api/v1/timesheets/**` and `/api/v1/projects/**` -> `timesheet-service`
- `/api/v1/leave/**` -> `leave-service`
- `/api/v1/admin/**` -> `admin-service`

### 4.4 Auth Service

Purpose:

- user registration
- login and JWT creation
- profile update
- admin user update
- manager assignment
- manager lookup for other services

Important endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/profile/{id}`
- `PUT /api/v1/auth/profile/{id}`
- `PUT /api/v1/auth/admin/users/{id}`
- `PUT /api/v1/auth/admin/users/{id}/manager`
- `GET /api/v1/auth/users/{employeeId}/manager`

Important business idea:

- each employee can have a `managerId`
- `leave-service` and `timesheet-service` use this to route approval requests

### 4.5 Timesheet Service

Purpose:

- manages projects
- stores daily timesheet entries
- groups entries into a weekly timesheet
- validates and submits weekly timesheets

Important endpoints:

- `POST /api/v1/timesheets/entries`
- `PUT /api/v1/timesheets/entries/{entryId}`
- `DELETE /api/v1/timesheets/entries/{entryId}`
- `GET /api/v1/timesheets`
- `GET /api/v1/timesheets/weeks/{date}/validate`
- `GET /api/v1/timesheets/all`
- `POST /api/v1/timesheets/submit`
- `POST /api/v1/projects`
- `PUT /api/v1/projects/{id}`
- `GET /api/v1/projects/{id}`
- `GET /api/v1/projects`

Important business idea:

- employee adds daily entries
- service creates a draft weekly timesheet
- manager is assigned automatically from `auth-service`
- submission publishes a RabbitMQ event

### 4.6 Leave Service

Purpose:

- manages leave policies
- initializes leave balances
- accepts leave requests
- shows leave balances and team calendar
- stores holidays

Important endpoints:

- `GET /api/v1/leave/balances`
- `POST /api/v1/leave/requests`
- `GET /api/v1/leave/requests`
- `POST /api/v1/leave/balances/initialize`
- `GET /api/v1/leave/team-calendar`
- `GET /api/v1/leave/policies`
- `GET /api/v1/leave/policies/{type}`
- `POST /api/v1/leave/policies`
- `DELETE /api/v1/leave/policies/{id}`
- `GET /api/v1/leave/holidays`
- `POST /api/v1/leave/holidays`
- `DELETE /api/v1/leave/holidays/{id}`

Important business idea:

- leave balances are stored per employee + leave type
- active leave policies drive balance initialization
- leave request submission assigns approver from `auth-service`
- submission publishes a RabbitMQ event

### 4.7 Admin Service

Purpose:

- receives approval tasks for leave and timesheets
- stores approval queue
- allows approve and reject actions
- provides dashboard and reports

Important endpoints:

- `GET /api/v1/admin/approvals/pending`
- `POST /api/v1/admin/approvals/{taskId}/approve`
- `POST /api/v1/admin/approvals/{taskId}/reject`
- `GET /api/v1/admin/dashboard/compliance`
- `GET /api/v1/admin/dashboard/employee-summary`
- `GET /api/v1/admin/reports/utilization`

Important business idea:

- admin service acts as the approval inbox
- it stores approval tasks separately from the original leave/timesheet tables
- after approval or rejection, it publishes an event back to the source service

## 5. Database Design

Each service has its own database.

Examples:

- `auth-service` -> `tms_auth_db`
- `timesheet-service` -> `tms_timesheet_db`
- `leave-service` -> `tms_leave_db`
- `admin-service` -> `tms_admin_db`

Why separate databases:

- service independence
- cleaner ownership
- avoids tight coupling between tables of different services

### Key tables by service

Auth:

- `users`

Timesheet:

- `projects`
- `timesheets`
- `timesheet_entries`

Leave:

- `leave_policies`
- `leave_balances`
- `leave_requests`
- `holidays`

Admin:

- `approval_tasks`

## 6. How Authentication Works

1. User registers in `auth-service`
2. User logs in
3. `auth-service` returns JWT
4. Client sends JWT to `api-gateway`
5. `api-gateway` validates JWT
6. `api-gateway` forwards request to downstream service
7. `api-gateway` injects identity headers such as:
   - `X-User-Id`
   - `X-User-Email`
   - `X-User-Role`

This allows internal services to know who the caller is without each one redoing full login logic.

## 7. How Manager Assignment Works

1. Admin assigns manager through:
   - `PUT /api/v1/auth/admin/users/{id}/manager`
2. `auth-service` stores `managerId` on the employee
3. `leave-service` fetches manager during leave request submission
4. `timesheet-service` fetches manager during draft/submission flow
5. approver ID is stored in the business record or approval event

Important note:

- `approverId` means "the assigned approver", not "already approved"

## 8. Leave Flow

### 8.1 Leave Setup

1. Admin creates leave policies
2. Employee leave balances are initialized from active policies

### 8.2 Leave Request Flow

1. Employee submits leave request
2. `leave-service` validates dates and leave balance
3. `leave-service` fetches employee manager from `auth-service`
4. `leave-service` creates `leave_requests` row with status like `SUBMITTED`
5. `leave-service` publishes event to RabbitMQ
6. `admin-service` consumes the event
7. `admin-service` creates an `approval_tasks` record
8. Approver approves or rejects from admin API
9. `admin-service` publishes approval-completed event
10. `leave-service` consumes the event and updates leave request/balance

### 8.3 RabbitMQ objects for Leave

Producer:

- exchange: `leave.exchange`
- routing key: `leave.requested`

Admin listener:

- queue: `admin.leave.queue`

Return event:

- exchange: `admin.exchange`
- routing key: `approval.completed`

Leave listener:

- queue binding to approval completion for leave updates

## 9. Timesheet Flow

### 9.1 Project Setup

1. Admin creates project
2. project has:
   - internal `id`
   - business-friendly `code`

### 9.2 Timesheet Entry Flow

1. Employee adds daily entries
2. `timesheet-service` creates or updates a weekly draft timesheet
3. employee manager is assigned from `auth-service`
4. entries are stored in `timesheet_entries`
5. weekly wrapper is stored in `timesheets`

### 9.3 Submission Flow

1. Employee submits weekly timesheet
2. `timesheet-service` validates the week
3. manager must exist
4. timesheet status changes to `SUBMITTED`
5. event is published to RabbitMQ
6. `admin-service` consumes event and creates approval task
7. approver approves or rejects
8. `admin-service` publishes approval-completed event
9. `timesheet-service` consumes event and updates timesheet status

### 9.4 RabbitMQ objects for Timesheet

Producer:

- exchange: `timesheet.exchange`
- routing key: `timesheet.submitted`

Admin listener:

- queue: `admin.timesheet.queue`

Return event:

- exchange: `admin.exchange`
- routing key: `approval.completed`

Timesheet listener:

- queue: `timesheet.approval.completed`

## 10. Admin Approval Flow

Admin service is the central approval inbox.

It receives:

- leave approval tasks
- timesheet approval tasks

Stored in:

- `approval_tasks`

Important fields:

- `id` -> approval task row ID
- `targetId` -> original leave request ID or timesheet ID
- `targetType` -> `LEAVE` or `TIMESHEET`
- `approverId`
- `status`

Meaning:

- `id` is the admin-service task record
- `targetId` is the business record being approved

## 11. Feign Client Usage

Feign is used for service-to-service API calls.

Examples:

- `leave-service` -> `auth-service`
- `timesheet-service` -> `auth-service`
- `admin-service` -> `auth-service`
- `admin-service` -> `leave-service`
- `admin-service` -> `timesheet-service`

This is used for:

- manager lookup
- record lookup
- cross-service details

## 12. RabbitMQ vs Feign: Why Both Are Used

Feign is used when:

- immediate synchronous response is needed
- example: fetch manager ID before submit

RabbitMQ is used when:

- the flow is asynchronous
- example: submission creates approval task in another service

So:

- Feign = request/response
- RabbitMQ = event-driven workflow

## 13. Swagger / API Docs Flow

Swagger is available both:

- directly on the service
- through the gateway

Gateway Swagger aggregates docs for:

- Auth Service
- Timesheet Service
- Leave Service
- Admin Service

This makes testing easier from one UI.

## 14. Docker and Runtime Setup

### 14.1 What Runs in Docker

Current image-based Docker setup is for:

- `auth-service`
- `timesheet-service`
- `leave-service`
- `admin-service`
- `api-gateway`

### 14.2 What Is Already External

In your setup, these are already running outside the app compose stack:

- Eureka
- Config Server
- MySQL
- RabbitMQ
- Zipkin

### 14.3 Important Docker Networking Rule

When containers need to access something running on your machine:

- do not use `localhost`
- use `host.docker.internal`

Examples:

- MySQL -> `jdbc:mysql://host.docker.internal:3306/...`
- RabbitMQ -> `host.docker.internal`
- Zipkin -> `http://host.docker.internal:9411/api/v2/spans`
- Eureka URL from containers -> `http://host.docker.internal:8761/eureka/`

### 14.4 Why This Is Needed

Inside a container:

- `localhost` means the container itself
- not your Windows host machine

## 15. GitHub Actions and Docker Hub Flow

Workflow files:

- `.github/workflows/build-and-push.yml` -> Eureka
- `.github/workflows/build-and-push-config-server.yml` -> Config Server
- `.github/workflows/build-and-push-services.yml` -> auth, admin, leave, timesheet, gateway

What the workflow does:

1. checks out code
2. sets up Java 21
3. builds jars with Maven
4. logs in to Docker Hub
5. builds Docker image
6. pushes:
   - `latest`
   - commit SHA tag

Required GitHub secrets:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## 16. Local Run Options

### 16.1 Direct local run

Use when:

- all services run from IDE or local terminal
- `localhost` values are fine in config

### 16.2 Docker image run

Use when:

- service jars are packaged into images
- app services run in Docker
- infrastructure is on host machine or elsewhere

In this case, host-based infra must be referred to with `host.docker.internal`.

## 17. Common Problems Already Found During This Project

### 17.1 First Swagger/register response issue

Likely due to gateway/service-discovery warm-up, not auth registration logic.

### 17.2 Leave balance initialization

Old logic was hardcoded.

Fixed to:

- initialize balances from active leave policies
- create one balance row per employee + leave type

### 17.3 Manager approval routing

Open queue vs assigned manager was discussed.

Current model uses assigned manager, which fits the PRD better.

### 17.4 Feign authorization

Internal service-to-service calls needed auth forwarding / explicit header passing.

### 17.5 Schema mismatch

Some DB enums were older than the code and caused insert failures until aligned.

### 17.6 Docker runtime config

Containers initially failed because config used `localhost` for host-based services.

Fixed by switching host references to `host.docker.internal`.

## 18. End-to-End Summary

### User registration/login

Client -> Gateway -> Auth Service -> MySQL -> JWT back to client

### Leave request

Client -> Gateway -> Leave Service -> Auth Service (manager lookup) -> Leave DB -> RabbitMQ -> Admin Service -> Admin DB -> approval event -> Leave Service -> Leave DB update

### Timesheet submission

Client -> Gateway -> Timesheet Service -> Auth Service (manager lookup) -> Timesheet DB -> RabbitMQ -> Admin Service -> Admin DB -> approval event -> Timesheet Service -> Timesheet DB update

### Admin action

Client -> Gateway -> Admin Service -> Admin DB -> RabbitMQ -> source service update

## 19. Practical Deployment Notes

If services are containerized but infrastructure stays on your machine:

- Config Server URL from containers: `host.docker.internal:7777`
- Eureka URL from containers: `host.docker.internal:8761`
- MySQL URL from containers: `host.docker.internal:3306`
- RabbitMQ host from containers: `host.docker.internal`
- Zipkin endpoint from containers: `host.docker.internal:9411`

If everything is later moved fully into Kubernetes:

- these host values should be replaced with Kubernetes service names instead

## 20. Final Mental Model

You can think of the project like this:

- `auth-service` = identity and hierarchy
- `timesheet-service` = work logging
- `leave-service` = time-off management
- `admin-service` = approval inbox
- `api-gateway` = front door
- `Eureka` = phone book
- `Config Server` = central settings
- `RabbitMQ` = event bus
- `MySQL` = storage

This combination gives you:

- synchronous service-to-service business lookups through Feign
- asynchronous approval workflows through RabbitMQ
- centralized config
- centralized routing
- service discovery
