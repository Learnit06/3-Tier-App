# User Registration Web Application

A simple three-tier Java web application demonstrating the classic architecture pattern with Apache HTTP Server, IBM WebSphere Application Server, and MySQL Database.

## Overview

This application provides a user registration form where users can submit their name and email address. The data is stored in a MySQL database and displayed in a table showing all registered users.

## Architecture

The application follows a three-tier architecture:

- **Tier 1 (Presentation Layer)**: Apache HTTP Server (reverse proxy)
- **Tier 2 (Application Layer)**: IBM WebSphere Application Server (Java Servlet)
- **Tier 3 (Data Layer)**: MySQL Database Server

## Features

- User registration form with name and email fields
- Data validation and sanitization
- Prepared statements to prevent SQL injection
- Real-time display of all registered users
- Responsive HTML interface with embedded CSS

## Technology Stack

- **Frontend**: HTML5, CSS3
- **Backend**: Java Servlet (Java 8+)
- **Application Server**: IBM WebSphere Application Server 9.0+
- **Database**: MySQL 5.7+ or MariaDB
- **Web Server**: Apache HTTP Server 2.4+
- **JDBC Driver**: MySQL Connector/J 8.0.33

## Prerequisites

Before you begin, ensure you have the following installed:

- IBM WebSphere Application Server 9.0 or higher
- MySQL 5.7+ or MariaDB 10.0+
- Apache HTTP Server 2.4+
- Java Development Kit (JDK) 8 or higher
- Linux operating system (tested on RHEL 9)

## Quick Start

### 1. Database Setup

```bash
# Login to MySQL as root
mysql -u root -p

# Run the database setup script
source mysql-setup.sql
```

This will create:
- Database: `userdb`
- Table: `users` with columns (id, name, email, created_at)
- Application user: `appuser` with password `AppPass123!`

**Important**: Change the default password in production!

### 2. Build the Application

```bash
# Set environment variables
export WAS_HOME=/path/to/WebSphere/AppServer
export JAVA_HOME=$WAS_HOME/java/8.0
export PATH=$JAVA_HOME/bin:$PATH

# Create directory structure
mkdir -p WebContent/WEB-INF/classes
mkdir -p WebContent/WEB-INF/lib

# Compile the servlet
javac -cp "$WAS_HOME/dev/JavaEE/j2ee.jar:lib/mysql-connector-j-8.0.33.jar" \
      -d WebContent/WEB-INF/classes \
      src/com/example/servlet/UserServlet.java

# Copy dependencies
cp lib/mysql-connector-j-8.0.33.jar WebContent/WEB-INF/lib/
cp web.xml WebContent/WEB-INF/

# Create WAR file
cd WebContent
jar -cvf ../userapp.war *
cd ..
```

### 3. Deploy to WebSphere

```bash
# Deploy the application
$WAS_HOME/profiles/AppSrv01/bin/wsadmin.sh -lang jython -c \
  "AdminApp.install('/path/to/userapp.war', '[-contextroot /userapp -appname UserApp]')"

# Save configuration
$WAS_HOME/profiles/AppSrv01/bin/wsadmin.sh -lang jython -c "AdminConfig.save()"

# Restart WebSphere server
$WAS_HOME/profiles/AppSrv01/bin/stopServer.sh server1
$WAS_HOME/profiles/AppSrv01/bin/startServer.sh server1
```

### 4. Configure Apache HTTP Server

Create `/etc/httpd/conf.d/userapp.conf`:

```apache
<VirtualHost *:80>
    ServerName localhost
    
    ProxyPreserveHost On
    ProxyRequests Off
    
    ProxyPass /userapp http://localhost:9080/userapp
    ProxyPassReverse /userapp http://localhost:9080/userapp
    
    <Location /userapp>
        Require all granted
    </Location>
</VirtualHost>
```

Enable proxy modules and restart Apache:

```bash
# Test configuration
httpd -t

# Restart Apache
systemctl restart httpd
```

### 5. Access the Application

- Direct WebSphere: `http://localhost:9080/userapp`
- Through Apache: `http://localhost/userapp`

## Project Structure

```
userapp/
├── src/
│   └── com/
│       └── example/
│           └── servlet/
│               └── UserServlet.java      # Main servlet
├── lib/
│   └── mysql-connector-j-8.0.33.jar     # JDBC driver
├── WebContent/
│   └── WEB-INF/
│       ├── web.xml                       # Deployment descriptor
│       ├── classes/                      # Compiled classes
│       └── lib/                          # Runtime libraries
├── web.xml                               # Deployment descriptor source
├── mysql-setup.sql                       # Database setup script
├── userapp.conf                          # Apache configuration
└── README.md                             # This file
```

## Configuration

### Database Connection

Edit `src/com/example/servlet/UserServlet.java` to update database credentials:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/userdb";
private static final String DB_USER = "appuser";
private static final String DB_PASSWORD = "YourSecurePassword";
```

### Context Root

The application is deployed with context root `/userapp`. To change it, modify the deployment command or update the application server configuration.

## Testing

### Test Database Connection

```bash
mysql -u appuser -p -e "SELECT * FROM userdb.users;"
```

### Test WebSphere Directly

```bash
curl http://localhost:9080/userapp/UserServlet
```

### Test Through Apache

```bash
curl http://localhost/userapp/UserServlet
```

### Submit Test Data

```bash
curl -X POST http://localhost/userapp/UserServlet \
     -d "name=John Doe&email=john@example.com"
```

## Troubleshooting

### Port Conflicts

Check if required ports are available:

```bash
netstat -tuln | grep -E ':80|:9080|:3306'
```

### Database Connection Issues

- Verify MySQL is running: `systemctl status mysqld`
- Test credentials: `mysql -u appuser -p`
- Check JDBC driver is in WEB-INF/lib

### WebSphere Deployment Issues

Check logs:
```bash
tail -f $WAS_HOME/profiles/AppSrv01/logs/server1/SystemOut.log
```

### Apache Proxy Issues

Check Apache logs:
```bash
tail -f /var/log/httpd/error_log
```

## Security Considerations

**Important security recommendations for production:**

1. Change default database password
2. Use environment variables for sensitive data
3. Enable HTTPS/SSL
4. Implement input validation and sanitization
5. Add authentication and authorization
6. Configure firewall rules
7. Regular security updates
8. Enable SELinux policies
9. Use connection pooling
10. Implement rate limiting

## Performance Optimization

- Configure WebSphere connection pooling
- Enable Apache caching for static content
- Add database indexes on frequently queried columns
- Tune JVM heap size based on load
- Implement session clustering for high availability

## License

This project is provided as-is for educational and demonstration purposes.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## Support

For questions or issues, please open an issue in the repository.

## Acknowledgments

- IBM WebSphere Application Server documentation
- MySQL JDBC driver documentation
- Apache HTTP Server documentation
