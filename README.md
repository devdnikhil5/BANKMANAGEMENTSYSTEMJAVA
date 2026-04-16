# 🏦 Bank Management System
Spring Boot • PostgreSQL • JWT • Bootstrap 5

A fully functional role‑based bank management system with separate dashboards for **Admin**, **Manager**, and **User**. All financial transactions require manager approval before being processed.

---

## ✨ Features

### 👑 Admin Panel
| Feature | Description |
|---------|-------------|
| 📊 Dashboard Statistics | View total users, managers, active users, and total deposits |
| 👥 Manager Management | Create, edit, activate/deactivate, and delete managers |
| 👤 User Overview | View all registered users with account details |
| 🔧 System Settings | Change admin password |

### 📋 Manager Panel
| Feature | Description |
|---------|-------------|
| 👤 User Management | Create, edit, activate/deactivate, and delete bank users |
| ⏳ Pending Approvals | Approve or reject deposit, withdrawal, and transfer requests |
| 📜 Transaction History | View all transactions with approval status |
| ⚙️ Profile Settings | Update profile and change password |

### 👤 User Panel
| Feature | Description |
|---------|-------------|
| 🏠 Account Overview | View balance and recent transactions |
| 💸 Deposits | Request deposits (requires manager approval) |
| 💳 Withdrawals | Request withdrawals (requires manager approval) |
| 🔄 Transfers | Send money to other accounts (requires manager approval) |
| 📋 Transaction History | View all own transactions with status |
| ⚙️ Profile Settings | Update profile and change password |

---

## 🛠️ Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.2 |
| Security | Spring Security, JWT (Stateless) |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Frontend | HTML5, CSS3, JavaScript |
| Build Tool | Apache Maven |
| IDE | VS Code / IntelliJ |

---

## 🚀 Quick Start

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 12+**
- **Git** (optional)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/TAN-UJ05/Bank-Management-System.git
   cd Bank-Management-System
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE bank_management;
   ```

3. **Update database credentials**  
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/bank_management
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_POSTGRES_PASSWORD
   ```

4. **Build and run**
   ```bash
   mvn clean spring-boot:run
   ```

5. **Access the application**  
   Open your browser and go to: `http://localhost:8080`

---

## 🔐 Default Login Credentials

| Role | Username | Password |
|------|----------|----------|
| **Admin** | `admin` | `admin123` |

*Managers are created by the Admin. Users are created by Managers.*

---

## 📁 Project Structure

```
Bank-Management-System/
│
├── 📄 pom.xml                          # Maven build configuration
├── 📄 README.md                        # Project documentation
│
├── 📁 src/main/java/com/bank/management/
│   │
│   ├── 📄 BankManagementApplication.java   # Main Spring Boot class
│   │
│   ├── 📁 config/
│   │   ├── 📄 DataInitializer.java        # Initializes roles and default admin
│   │   ├── 📄 SecurityConfig.java         # Spring Security + JWT configuration
│   │   └── 📄 WebConfig.java              # CORS configuration
│   │
│   ├── 📁 controller/
│   │   ├── 📄 AdminController.java        # Admin endpoints
│   │   ├── 📄 AuthController.java         # Authentication endpoints
│   │   ├── 📄 ManagerController.java      # Manager endpoints
│   │   └── 📄 UserController.java         # User endpoints
│   │
│   ├── 📁 dto/
│   │   ├── 📄 ApproveTransactionRequest.java
│   │   ├── 📄 ChangePasswordRequest.java
│   │   ├── 📄 JwtResponse.java
│   │   ├── 📄 LoginRequest.java
│   │   ├── 📄 ManagerDto.java
│   │   ├── 📄 ProfileUpdateRequest.java
│   │   ├── 📄 SignupRequest.java
│   │   ├── 📄 TransactionRequest.java
│   │   ├── 📄 TransactionResponse.java
│   │   └── 📄 UserDto.java
│   │
│   ├── 📁 exception/
│   │   ├── 📄 GlobalExceptionHandler.java
│   │   ├── 📄 ResourceNotFoundException.java
│   │   └── 📄 UnauthorizedException.java
│   │
│   ├── 📁 model/
│   │   ├── 📄 Account.java
│   │   ├── 📄 ApprovalStatus.java
│   │   ├── 📄 Role.java (with ERole inner enum)
│   │   ├── 📄 Transaction.java
│   │   └── 📄 User.java
│   │
│   ├── 📁 repository/
│   │   ├── 📄 AccountRepository.java
│   │   ├── 📄 RoleRepository.java
│   │   ├── 📄 TransactionRepository.java
│   │   └── 📄 UserRepository.java
│   │
│   ├── 📁 security/
│   │   ├── 📁 jwt/
│   │   │   ├── 📄 AuthEntryPointJwt.java
│   │   │   ├── 📄 AuthTokenFilter.java
│   │   │   └── 📄 JwtUtils.java
│   │   └── 📁 services/
│   │       ├── 📄 UserDetailsImpl.java
│   │       └── 📄 UserDetailsServiceImpl.java
│   │
│   └── 📁 service/
│       ├── 📄 AdminService.java
│       ├── 📄 AuthService.java
│       ├── 📄 ManagerService.java
│       ├── 📄 TransactionService.java
│       └── 📄 UserService.java
│
├── 📁 src/main/resources/
│   ├── 📄 application.properties          # Database, JWT, server settings
│   │
│   └── 📁 static/
│       ├── 📄 index.html                  # Landing page
│       ├── 📄 login.html                  # Login page
│       ├── 📄 admin-dashboard.html        # Admin dashboard
│       ├── 📄 manager-dashboard.html      # Manager dashboard
│       ├── 📄 user-dashboard.html         # User dashboard
│       │
│       ├── 📁 css/
│       │   └── 📄 style.css               # Professional styling
│       │
│       └── 📁 js/
│           ├── 📄 login.js
│           ├── 📄 admin.js
│           ├── 📄 manager.js
│           └── 📄 user.js
│
└── 📁 src/test/java/                     # Test classes (optional)
```

---

## 🗃️ Database Schema

| Table | Description |
|-------|-------------|
| `users` | Stores user credentials and profile (admin, manager, user) |
| `roles` | User roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER` |
| `user_roles` | Join table for users and roles |
| `accounts` | Bank accounts linked to users (account number, balance) |
| `transactions` | All financial transactions with approval status |

---

## 🎯 API Endpoints

### 🔓 Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signin` | Authenticate and receive JWT |

### 👑 Admin Endpoints (requires `ROLE_ADMIN`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/managers` | List all managers |
| POST | `/api/admin/managers` | Create a new manager |
| GET | `/api/admin/managers/{id}` | Get manager details |
| PUT | `/api/admin/managers/{id}` | Update manager |
| DELETE | `/api/admin/managers/{id}` | Delete manager |
| PATCH | `/api/admin/managers/{id}/toggle-status` | Activate/deactivate manager |
| GET | `/api/admin/users` | List all users |
| DELETE | `/api/admin/users/{id}` | Delete a user |
| GET | `/api/admin/statistics` | System statistics |

### 📋 Manager Endpoints (requires `ROLE_MANAGER`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/manager/users` | List all users |
| POST | `/api/manager/users` | Create a new user |
| GET | `/api/manager/users/{id}` | Get user details |
| PUT | `/api/manager/users/{id}` | Update user |
| DELETE | `/api/manager/users/{id}` | Delete user |
| PATCH | `/api/manager/users/{id}/toggle-status` | Activate/deactivate user |
| GET | `/api/manager/transactions/pending` | List pending transactions |
| GET | `/api/manager/transactions` | List all transactions |
| PATCH | `/api/manager/transactions/{id}/approve` | Approve a transaction |
| PATCH | `/api/manager/transactions/{id}/reject` | Reject a transaction |

### 👤 User Endpoints (requires `ROLE_USER`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get own profile |
| PUT | `/api/user/profile` | Update profile |
| POST | `/api/user/change-password` | Change own password |
| GET | `/api/user/balance` | Get account balance |
| POST | `/api/user/transactions/deposit` | Create deposit request |
| POST | `/api/user/transactions/withdraw` | Create withdrawal request |
| POST | `/api/user/transactions/transfer` | Create transfer request |
| GET | `/api/user/transactions` | View own transactions |
| GET | `/api/user/transactions/pending` | View pending transactions |

---

## 💻 Usage Guide

### For Administrators
1. Login with `admin` / `admin123`
2. Create one or more **Managers** from the "Managers" section
3. View system statistics and all users

### For Managers
1. Login with the credentials provided by the Admin
2. Create **Users** from the "Manage Users" section
3. Approve or reject pending transactions in the "Pending Approvals" tab
4. View all transactions in the "All Transactions" tab

### For Users
1. Login with the credentials provided by the Manager
2. View balance and recent activity on the Overview page
3. Request deposits, withdrawals, or transfers
4. Check transaction history and pending requests
5. Update profile and change password

---

## 🔒 Security Features

- **JWT‑based authentication** (stateless)
- **Password hashing** with BCrypt
- **Role‑based access control** (`@PreAuthorize`)
- **CORS configuration**
- **SQL injection prevention** via Spring Data JPA
- **Native SQL deletes** to avoid foreign key violations

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 already in use | Change `server.port` in `application.properties` |
| Database connection failure | Ensure PostgreSQL is running and credentials are correct |
| Tables not created | Set `spring.jpa.hibernate.ddl-auto=update` |
| "mvn not recognized" | Add Maven `bin` folder to system PATH |
| JWT secret placeholder error | Ensure `application.properties` exists in `src/main/resources` |
| Foreign key error on delete | Use the native delete methods already implemented in the services |

---

## 🚀 Future Enhancements

- 📧 Email notifications for transaction approvals
- 📊 Advanced reporting and export to PDF/Excel
- 📱 Progressive Web App (PWA) support
- 🔔 Real‑time notifications via WebSocket
- 📈 Charts and analytics dashboard
- 🌐 Multi‑language support

---

## 👨‍💻 Author

**Nikhil Devari**  
- GitHub: [@devdnikhil5](https://github.com/devdnikhil5)  
- Email: devdnikhil5@gmail.com

---

## ⚖️ License

This project is licensed under the **MIT License**.

---

**Made with ❤️ using Spring Boot and PostgreSQL.**
