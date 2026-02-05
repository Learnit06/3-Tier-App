# Setup Guide - User Registration Web Application

This guide provides step-by-step instructions for deploying the User Registration web application in a three-tier architecture.

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Pre-installation Checklist](#pre-installation-checklist)
3. [MySQL Database Setup](#mysql-database-setup)
4. [Application Build and Compilation](#application-build-and-compilation)
5. [WebSphere Deployment](#websphere-deployment)
6. [Apache HTTP Server Configuration](#apache-http-server-configuration)
7. [Testing and Verification](#testing-and-verification)
8. [Post-Deployment Configuration](#post-deployment-configuration)

---

## System Requirements

### Hardware Requirements
- CPU: 2+ cores recommended
- RAM: 4GB minimum, 8GB recommended
- Disk Space: 5GB free space

### Software Requirements
- Operating System: Linux (RHEL 8/9, CentOS 8/9, Ubuntu 20.04+)
- IBM WebSphere Application Server 9.0 or higher
- MySQL 5.7+ or MariaDB 10.0+
- Apache HTTP Server 2.4+
- Java Development Kit (JDK) 8 or higher

---

## Pre-installation Checklist

Before starting, verify the following:

```bash
# 1. Check if Java is installed
java -version

# 2. Check if MySQL is running
systemctl status mysqld

# 3. Check if Apache httpd is installed
httpd -v

# 4. Check WebSphere installation
ls -la /opt/IBM/WebSphere/AppServer*

# 5. Check available ports
netstat -tuln | grep -E ':80|:9080|:3306'
```

### Create Working Directory

```bash
mkdir -p ~/userapp-deploy
cd ~/userapp-deploy
```

### Extract the Application Package

```bash
# Extract the zip file
unzip userapp.zip

# Verify contents
ls -la
```

You should see:
- `src/` - Source code directory
- `lib/` - JDBC driver
- `web.xml` - Deployment descriptor
- `mysql-setup.sql` - Database script
- `userapp.conf` - Apache configuration
- `README.md` - Main documentation
- `SETUP.md` - This file

---

## MySQL Database Setup

### Step 1: Login to MySQL

```bash
mysql -u root -p
# Enter root password when prompted
```

### Step 2: Create Database and User

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS userdb;

-- Use the database
USE userdb;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create application user
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'AppPass123!';

-- Grant privileges
GRANT ALL PRIVILEGES ON userdb.* TO 'appuser'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Exit MySQL
EXIT;
```

**Or use the provided script:**

```bash
mysql -u root -p < mysql-setup.sql
```

### Step 3: Verify Database Setup

```bash
# Test connection
mysql -u appuser -p'AppPass123!' -e "SHOW DATABASES;"

# Verify table structure
mysql -u appuser -p'AppPass123!' -e "USE userdb; DESCRIBE users;"
```

---

## Application Build and Compilation

### Step 1: Set Environment Variables

```bash
# Set WebSphere home (adjust path to your installation)
export WAS_HOME=/opt/IBM/WebSphere/AppServer

# Set Java home
export JAVA_HOME=$WAS_HOME/java/8.0
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java is accessible
javac -version
```

### Step 2: Create Build Directories

```bash
mkdir -p WebContent/WEB-INF/classes
mkdir -p WebContent/WEB-INF/lib
```

### Step 3: Download JDBC Driver (if not included)

```bash
cd lib
wget https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
cd ..
```

### Step 4: Update Database Credentials

Edit `src/com/example/servlet/UserServlet.java` and update these lines:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/userdb";
private static final String DB_USER = "appuser";
private static final String DB_PASSWORD = "AppPass123!";  // Change this!
```

### Step 5: Compile the Servlet

```bash
javac -cp "$WAS_HOME/dev/JavaEE/j2ee.jar:lib/mysql-connector-j-8.0.33.jar" \
      -d WebContent/WEB-INF/classes \
      src/com/example/servlet/UserServlet.java
```

Verify compilation:
```bash
ls -la WebContent/WEB-INF/classes/com/example/servlet/
# Should show UserServlet.class
```

### Step 6: Copy Dependencies

```bash
# Copy web.xml
cp web.xml WebContent/WEB-INF/

# Copy JDBC driver
cp lib/mysql-connector-j-8.0.33.jar WebContent/WEB-INF/lib/
```

### Step 7: Create WAR File

```bash
cd WebContent
jar -cvf ../userapp.war *
cd ..
```

Verify WAR contents:
```bash
jar -tvf userapp.war | grep -E "UserServlet.class|web.xml|mysql-connector"
```

---

## WebSphere Deployment

### Step 1: Locate WebSphere Profile

```bash
# Find your profile directory
ls -la $WAS_HOME/profiles/

# Set profile path (adjust name as needed)
export WAS_PROFILE=$WAS_HOME/profiles/AppSrv01
```

### Step 2: Check WebSphere Status

```bash
$WAS_PROFILE/bin/serverStatus.sh server1
```

If not running:
```bash
$WAS_PROFILE/bin/startServer.sh server1
```

### Step 3: Deploy the Application

```bash
cd $WAS_PROFILE/bin

# Deploy WAR file
./wsadmin.sh -lang jython -c \
  "AdminApp.install('$HOME/userapp-deploy/userapp.war', \
  '[-contextroot /userapp -appname UserApp -MapWebModToVH [[\"User Registration App\" \"userapp.war,WEB-INF/web.xml\" default_host]]]')"

# Save configuration
./wsadmin.sh -lang jython -c "AdminConfig.save()"
```

### Step 4: Restart WebSphere Server

```bash
$WAS_PROFILE/bin/stopServer.sh server1
$WAS_PROFILE/bin/startServer.sh server1
```

Wait 30-60 seconds for the server to start completely.

### Step 5: Verify Deployment

```bash
# List deployed applications
./wsadmin.sh -lang jython -c "print AdminApplication.listApplications()"

# Should show: ['UserApp']
```

### Step 6: Test Direct Access to WebSphere

```bash
curl http://localhost:9080/userapp/UserServlet
```

Expected output: HTML content with the registration form.

---

## Apache HTTP Server Configuration

### Step 1: Enable Proxy Modules

Check if proxy modules are loaded:
```bash
httpd -M | grep proxy
```

Should show:
- proxy_module
- proxy_http_module

If not loaded, edit `/etc/httpd/conf.modules.d/00-proxy.conf` and uncomment:
```apache
LoadModule proxy_module modules/mod_proxy.so
LoadModule proxy_http_module modules/mod_proxy_http.so
```

### Step 2: Create Virtual Host Configuration

```bash
# Copy the configuration file
sudo cp userapp.conf /etc/httpd/conf.d/

# Or create it manually
sudo vi /etc/httpd/conf.d/userapp.conf
```

Content:
```apache
<VirtualHost *:80>
    ServerName localhost
    
    ErrorLog /var/log/httpd/userapp_error.log
    CustomLog /var/log/httpd/userapp_access.log combined
    
    ProxyPreserveHost On
    ProxyRequests Off
    
    ProxyPass /userapp http://localhost:9080/userapp
    ProxyPassReverse /userapp http://localhost:9080/userapp
    
    RedirectMatch ^/$ /userapp
    
    <Location /userapp>
        Require all granted
    </Location>
</VirtualHost>
```

### Step 3: Configure Firewall

```bash
# Allow HTTP traffic
sudo firewall-cmd --permanent --add-service=http

# Or add port 80 specifically
sudo firewall-cmd --permanent --add-port=80/tcp

# Reload firewall
sudo firewall-cmd --reload

# Verify
sudo firewall-cmd --list-all
```

### Step 4: Test and Restart Apache

```bash
# Test configuration syntax
sudo httpd -t

# If OK, restart Apache
sudo systemctl restart httpd

# Check status
sudo systemctl status httpd
```

---

## Testing and Verification

### Test 1: Check All Services Are Running

```bash
# Apache
sudo systemctl status httpd

# MySQL
sudo systemctl status mysqld

# WebSphere
$WAS_PROFILE/bin/serverStatus.sh server1
```

All should show "running" or "STARTED".

### Test 2: Verify Port Listening

```bash
sudo ss -tuln | grep -E ':80|:9080|:3306'
```

Expected output:
- Port 80 listening (Apache)
- Port 9080 listening (WebSphere)
- Port 3306 listening (MySQL)

### Test 3: Test Direct WebSphere Access

```bash
curl http://localhost:9080/userapp/UserServlet
```

Should return HTML with the registration form.

### Test 4: Test Through Apache Proxy

```bash
curl http://localhost/userapp/UserServlet
```

Should return the same HTML content.

### Test 5: Test Form Submission

```bash
curl -X POST http://localhost/userapp/UserServlet \
     -d "name=Test User&email=test@example.com"
```

Then verify in database:
```bash
mysql -u appuser -p'AppPass123!' -e "SELECT * FROM userdb.users;"
```

### Test 6: Access from Browser

Open a web browser and navigate to:
- `http://localhost/userapp`
- `http://YOUR_SERVER_IP/userapp`

You should see the user registration form.

Try submitting a user:
1. Enter a name
2. Enter an email
3. Click Submit
4. The page should refresh and show the new user in the table

---

## Post-Deployment Configuration

### Security Hardening

1. **Change Default Password:**
```sql
ALTER USER 'appuser'@'localhost' IDENTIFIED BY 'NewSecurePassword123!';
FLUSH PRIVILEGES;
```

Update in `UserServlet.java` and redeploy.

2. **Enable SELinux for httpd:**
```bash
sudo setsebool -P httpd_can_network_connect 1
```

3. **Configure HTTPS (Optional but Recommended):**
- Obtain SSL certificate
- Configure Apache for HTTPS
- Update virtual host to use port 443

### Performance Tuning

1. **Configure Connection Pooling in WebSphere:**
- Use WebSphere Admin Console
- Navigate to Resources → JDBC → Data Sources
- Create a new data source for MySQL

2. **Tune JVM Heap Size:**
Edit `$WAS_PROFILE/bin/server.xml` and adjust:
```xml
<jvmEntries initialHeapSize="512" maximumHeapSize="2048"/>
```

3. **Enable Apache Caching:**
Add to `/etc/httpd/conf.d/userapp.conf`:
```apache
CacheEnable disk /userapp
CacheRoot /var/cache/httpd/proxy
```

### Monitoring and Logging

1. **Set up log rotation for Apache:**
```bash
sudo vi /etc/logrotate.d/httpd
```

2. **Monitor WebSphere logs:**
```bash
tail -f $WAS_PROFILE/logs/server1/SystemOut.log
```

3. **Monitor MySQL slow queries:**
Enable slow query log in `/etc/my.cnf`:
```ini
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 2
```

### Backup Strategy

1. **Database Backup:**
```bash
mysqldump -u root -p userdb > userdb_backup_$(date +%Y%m%d).sql
```

2. **Application Backup:**
```bash
cp userapp.war /backup/userapp_$(date +%Y%m%d).war
```

3. **Configuration Backup:**
```bash
cp /etc/httpd/conf.d/userapp.conf /backup/
```

---

## Common Issues and Solutions

### Issue: Port Already in Use

**Solution:**
```bash
# Find process using port
sudo lsof -ti:9080

# Kill the process
sudo kill -9 <PID>
```

### Issue: ClassNotFoundException

**Solution:**
- Verify JDBC driver is in `WEB-INF/lib`
- Rebuild WAR file
- Redeploy application

### Issue: Database Connection Refused

**Solution:**
```bash
# Check MySQL is running
sudo systemctl status mysqld

# Verify connection
mysql -u appuser -p -h localhost
```

### Issue: 404 Not Found

**Solution:**
- Verify application is deployed and started
- Check context root is `/userapp`
- Check WebSphere logs for errors

### Issue: 503 Service Unavailable

**Solution:**
- WebSphere may not be running
- Check proxy configuration in Apache
- Verify WebSphere is listening on port 9080

---

## Support and Resources

- **MySQL Documentation:** https://dev.mysql.com/doc/
- **WebSphere Documentation:** https://www.ibm.com/docs/en/was
- **Apache HTTP Server Documentation:** https://httpd.apache.org/docs/

---

## Conclusion

Your three-tier User Registration web application should now be fully deployed and accessible. For any issues, refer to the troubleshooting section or check the log files mentioned in this guide.

Remember to:
- Change default passwords
- Enable HTTPS in production
- Set up regular backups
- Monitor application logs
- Apply security updates regularly
