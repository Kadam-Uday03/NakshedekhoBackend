# NaksheDekho - Premium Backend Support Design Project Management Platform

A full-stack interior design service platform built with **Java Spring Boot** backend and **HTML/CSS/JavaScript** frontend. This platform enables customers to explore interior design projects, purchase packages, and track their project progress through a professional workflow system.

## 🎯 Project Overview

**NaksheDekho** is a comprehensive interior design management platform featuring:

- **Public Marketing Website** - Showcase services and design packages
- **Customer Dashboard** - Track projects, view progress, and manage payments
- **Manager Admin Panel** - Update project progress and manage assigned projects
- **Owner Admin Panel** - Full system control, analytics, and package management

## 🛠️ Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** with JWT Authentication
- **Spring Data JPA** with Hibernate
- **MySQL Database**
- **Maven** for dependency management

### Frontend
- **HTML5**
- **CSS3** (Vanilla CSS with custom design system)
- **JavaScript** (Vanilla JS, no frameworks)
- **RESTful API** integration

## 🎨 Design System

### Color Palette
- **Primary Blue**: `#0D3B66` - Headers, Buttons
- **Accent Cyan**: `#00B5E2` - Highlights, Icons
- **White**: `#FFFFFF` - Background
- **Light Grey**: `#F5F5F5` - Section backgrounds
- **Dark Grey**: `#333333` - Text

### Typography
- **Primary Font**: Inter
- **Heading Font**: Outfit

## 📋 Prerequisites

Before running this application, ensure you have:

1. **Java Development Kit (JDK) 17** or higher
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Verify: `java -version`

2. **Maven 3.6+**
   - Download from: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

3. **MySQL 8.0+**
   - Download from: https://dev.mysql.com/downloads/mysql/
   - Verify: `mysql --version`

4. **Git** (optional, for version control)

## 🚀 Installation & Setup

### Step 1: Database Setup

1. Start MySQL server
2. Create the database:

```sql
CREATE DATABASE nakshedekho_db;
```

3. (Optional) Create a dedicated MySQL user:

```sql
CREATE USER 'nakshedekho_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON nakshedekho_db.* TO 'nakshedekho_user'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: Configure Database Connection

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nakshedekho_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

**Important**: Update the `username` and `password` with your MySQL credentials.

### Step 3: Build and Run Backend

Navigate to the backend directory:

```bash
cd backend
```

Build the project:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```

The backend server will start on **http://localhost:8080**

### Step 4: Run the Application

Navigate to the project directory:

```bash
cd backend
mvn spring-boot:run
```

The application will be available at:
- **Website**: http://localhost:8080
- **Admin Panels**: Accessible through the login page

**Note**: Since the frontend is integrated into the Spring Boot backend, you don't need a separate server to run the HTML files.

## 👥 User Roles & Access

### 1. Customer (CUSTOMER)
- Browse and purchase design packages
- Track project progress
- View payment status
- Download design files
- Access: `/customer/dashboard.html`

### 2. Manager Admin (MANAGER_ADMIN)
- View assigned projects
- Update project progress
- Upload design files
- Track payments
- Update estimated completion dates
- Access: `/manager/dashboard.html`

### 3. Owner Admin (OWNER_ADMIN)
- Create/update/delete design packages
- Assign projects to managers
- View analytics
- Manage all projects
- Full system control
- Access: `/owner/dashboard.html`

## 🔐 Authentication Flow

1. **Registration**: Users register with email, password, and role
2. **Login**: Email and password authentication
3. **JWT Token**: Server generates JWT token upon successful login
4. **Token Storage**: Token stored in localStorage
5. **API Requests**: Token sent in Authorization header
6. **Role-Based Access**: Backend validates role for protected endpoints

## 📡 API Endpoints

### Public Endpoints (No Authentication Required)
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/public/packages` - Get all active packages
- `GET /api/public/packages/{id}` - Get package by ID
- `POST /api/public/contact` - Submit contact form

### Customer Endpoints (Requires CUSTOMER role)
- `POST /api/customer/purchase` - Purchase a package
- `GET /api/customer/projects` - Get customer's projects
- `GET /api/customer/projects/{id}` - Get project details
- `GET /api/customer/projects/{id}/stages` - Get project stages

### Manager Endpoints (Requires MANAGER_ADMIN role)
- `GET /api/manager/projects` - Get assigned projects
- `GET /api/manager/projects/{id}` - Get project details
- `PUT /api/manager/projects/{id}` - Update project
- `GET /api/manager/projects/{id}/stages` - Get project stages
- `PUT /api/manager/stages/{stageId}` - Update stage

### Owner Endpoints (Requires OWNER_ADMIN role)
- `GET /api/owner/packages` - Get all packages
- `POST /api/owner/packages` - Create package
- `PUT /api/owner/packages/{id}` - Update package
- `DELETE /api/owner/packages/{id}` - Delete package
- `GET /api/owner/projects` - Get all projects
- `PUT /api/owner/projects/{projectId}/assign/{managerId}` - Assign manager
- `DELETE /api/owner/projects/{id}` - Delete project
- `GET /api/owner/managers` - Get all managers
- `GET /api/owner/enquiries` - Get contact enquiries
- `GET /api/owner/analytics` - Get system analytics

## 🗂️ Project Structure

```
nakshedekho/
├── backend/
│   ├── src/main/java/com/nakshedekho/
│   │   ├── config/          # Security & CORS configuration
│   │   ├── controller/      # REST API controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # JWT utilities
│   │   └── service/         # Business logic
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── static/          # Integrated Website (HTML, CSS, JS)
│   │       ├── css/
│   │       ├── js/
│   │       ├── index.html
│   │       ├── login.html
│   │       └── ...
│   ├── uploads/             # Project files storage
│   └── pom.xml
└── README.md
```

## 🧪 Testing the Application

### 1. Test User Registration
1. Go to http://localhost:5500/login.html
2. Click "Register" tab
3. Fill in details and select role
4. Click "Register"

### 2. Test Customer Flow
1. Login as CUSTOMER
2. Browse packages at `/packages.html`
3. Purchase a package
4. View dashboard to track progress

### 3. Test Manager Flow
1. Login as MANAGER_ADMIN
2. View assigned projects
3. Update project progress
4. Update stage status

### 4. Test Owner Flow
1. Login as OWNER_ADMIN
2. Create/manage packages
3. Assign projects to managers
4. View analytics

## 🔧 Configuration

### JWT Configuration
Edit `application.properties`:
```properties
jwt.secret=YourSecretKeyHere
jwt.expiration=86400000  # 24 hours in milliseconds
```

### CORS Configuration
Update allowed origins in `SecurityConfig.java`:
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "http://127.0.0.1:5500",
    "http://localhost:5500"
));
```

## 🐛 Troubleshooting

### Backend Issues

**Issue**: Database connection failed
- **Solution**: Verify MySQL is running and credentials are correct in `application.properties`

**Issue**: Port 8080 already in use
- **Solution**: Change port in `application.properties`: `server.port=8081`

**Issue**: JWT token errors
- **Solution**: Ensure JWT secret is properly configured and token is being sent in headers

### Frontend Issues

**Issue**: CORS errors
- **Solution**: Verify backend CORS configuration includes your frontend URL

**Issue**: API calls failing
- **Solution**: Check that backend is running on http://localhost:8080

**Issue**: Login not working
- **Solution**: Check browser console for errors, verify credentials

## 📝 License

This project is created for educational purposes.

## 👨‍💻 Support

For issues or questions:
1. Check the troubleshooting section
2. Review API endpoint documentation
3. Verify database and backend are running
4. Check browser console for frontend errors

## 🎉 Features Implemented

✅ JWT Authentication & Authorization  
✅ Role-Based Access Control (3 roles)  
✅ Customer Package Purchase Flow  
✅ Project Progress Tracking  
✅ Payment Management  
✅ Project Stage Milestones  
✅ Manager Project Updates  
✅ Owner Analytics Dashboard  
✅ Responsive Design  
✅ Premium UI/UX  
✅ RESTful API Architecture  

## 🚧 Future Enhancements

- Real payment gateway integration (Razorpay/Stripe)
- File upload functionality for design files
- Email notifications
- Real-time chat between customer and manager
- Mobile app version
- Advanced analytics and reporting

---

**Built with ❤️ for NaksheDekho - Premium Interior Design Platform**
#   N a k s h e d e k h o B a c k e n d 
 
 #   N a k s h e d e k h o B a c k e n d 
 
 #   N a k s h e d e k h o B a c k e n d 
 
 
