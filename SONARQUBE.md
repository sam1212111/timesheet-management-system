# SonarQube Setup

This repository is set up for local SonarQube analysis with:

- SonarQube Community Build Docker image: `26.3.0.120487-community`
- SonarScanner for Maven: `5.5.0.6356`
- Root Maven aggregator: [pom.xml](/C:/Users/User/OneDrive/Desktop/TimeSheetLeaveManagementSystem/pom.xml)

## 1. Start SonarQube

From the repository root:

```powershell
docker compose -f docker-compose.sonarqube.yml up -d
```

Open:

```text
http://localhost:9000
```

Default credentials:

```text
username: admin
password: admin
```

After first login, create a user token in SonarQube and use it for analysis.

## 2. Run analysis from the repository root

From the repository root:

```powershell
mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token="<your_token>" -Dsonar.projectKey=tms-root -Dsonar.projectName="timesheet-management-system"
```

Optional custom server URL:

```powershell
mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar -Dsonar.host.url="<your_sonar_url>" -Dsonar.token="<your_token>" -Dsonar.projectKey=tms-root -Dsonar.projectName="timesheet-management-system"
```

If you want to run analysis even when some module tests are still failing, you can temporarily skip tests:

```powershell
mvn -DskipTests verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token="<your_token>" -Dsonar.projectKey=tms-root -Dsonar.projectName="timesheet-management-system"
```

## 3. What gets analyzed

The root aggregator includes these modules:

- `auth-service`
- `timesheet-service`
- `leave-service`
- `admin-service`
- `api-gateway`
- `Config-Server`
- `EurekaServer`

## 4. Stop SonarQube

```powershell
docker compose -f docker-compose.sonarqube.yml down
```

To also remove SonarQube data:

```powershell
docker compose -f docker-compose.sonarqube.yml down -v
```
