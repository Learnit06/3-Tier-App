package com.example.servlet;

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class UserServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/userdb";
    private static final String DB_USER = "appuser";
    private static final String DB_PASSWORD = "AppPass123!";
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>User Registration</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }");
        out.println("h2 { color: #333; }");
        out.println("form { background: #f4f4f4; padding: 20px; border-radius: 5px; }");
        out.println("input { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 3px; }");
        out.println("button { background: #4CAF50; color: white; padding: 12px 20px; border: none; border-radius: 3px; cursor: pointer; }");
        out.println("button:hover { background: #45a049; }");
        out.println(".users { margin-top: 30px; }");
        out.println("table { width: 100%; border-collapse: collapse; }");
        out.println("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
        out.println("th { background-color: #4CAF50; color: white; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h2>User Registration Form</h2>");
        out.println("<form method='post' action='UserServlet'>");
        out.println("<input type='text' name='name' placeholder='Enter Name' required>");
        out.println("<input type='email' name='email' placeholder='Enter Email' required>");
        out.println("<button type='submit'>Submit</button>");
        out.println("</form>");
        
        // Display existing users
        out.println("<div class='users'>");
        out.println("<h2>Registered Users</h2>");
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY created_at DESC");
            
            out.println("<table>");
            out.println("<tr><th>ID</th><th>Name</th><th>Email</th><th>Created At</th></tr>");
            
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getInt("id") + "</td>");
                out.println("<td>" + rs.getString("name") + "</td>");
                out.println("<td>" + rs.getString("email") + "</td>");
                out.println("<td>" + rs.getTimestamp("created_at") + "</td>");
                out.println("</tr>");
            }
            
            out.println("</table>");
            
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            out.println("<p style='color:red;'>Error: " + e.getMessage() + "</p>");
        }
        
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
            
            pstmt.close();
            conn.close();
            
            response.sendRedirect("UserServlet");
        } catch (Exception e) {
            response.getWriter().println("Error: " + e.getMessage());
        }
    }
}
