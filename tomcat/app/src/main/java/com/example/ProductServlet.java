package com.example;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.annotation.WebServlet;

//@WebServlet("/products")
@WebServlet(urlPatterns = {"/products", "/admin"})
public class ProductServlet extends HttpServlet {

    private String dbUrl;
    private String dbUser;
    private String dbPass;
    private String smtpHost;
    private String smtpPort;

    @Override
    public void init() throws ServletException {
        dbUrl = System.getenv("DB_URL");
        dbUser = System.getenv("DB_USER");
        dbPass = System.getenv("DB_PASS");
        smtpHost = System.getenv("SMTP_HOST");
        smtpPort = System.getenv("SMTP_PORT");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new ServletException("Database environment variables not set!");
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("Postgres JDBC driver not found", e);
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Updated Table Schema to ensure we have standard fields
            String sql = "CREATE TABLE IF NOT EXISTS products (" +
                         "id SERIAL PRIMARY KEY," +
                         "name TEXT NOT NULL," +
                         "price REAL NOT NULL," +
                         "stock INTEGER NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new ServletException("Failed to initialize database table", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        
        // 1. If URL is /admin, show Admin Screen
        if (path.equals("/admin")) {
            getAdminScreen(resp);
            return;
        }

        // 2. Otherwise, treat as /products (Storefront)
        String action = req.getParameter("action");
        if (action == null || action.equals("list")) {
            getProducts(resp);
        } else {
            resp.getWriter().println("Unknown action: " + action);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        
        // Default to ordering if no action is specified (backward compatibility)
        if (action == null || action.equals("order")) {
            handleOrder(req, resp);
        } else if (action.equals("add")) {
            handleAddBook(req, resp);
        } else if (action.equals("delete")) {
            handleDeleteBook(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown POST action");
        }
    }

    // --- VIEW METHODS ---

    private void getProducts(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY id")) {

            PrintWriter out = resp.getWriter();
            out.println("<html><head><title>Bookstore</title></head><body>");
            out.println("<h1>Bookstore Inventory</h1>");
            //out.println("<p><a href='products?action=admin'>[Go to Admin Mode]</a></p>");
            out.println("<p><a href='admin'>[Go to Admin Mode]</a></p>");
            out.println("<table border='1'><tr><th>ID</th><th>Name</th><th>Price</th><th>Stock</th><th>Action</th></tr>");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                out.printf("<tr><td>%d</td><td>%s</td><td>$%.2f</td><td>%d</td>", id, name, price, stock);
                
                // Order Form
                if (stock > 0) {
                    out.printf("<td><form method='POST' action='products'>" +
                            "<input type='hidden' name='action' value='order'/>" +
                            "<input type='hidden' name='productId' value='%d'/>" +
                            "<input type='submit' value='Buy Now'/></form></td></tr>", id);
                } else {
                    out.println("<td><span style='color:red'>Out of Stock</span></td></tr>");
                }
            }
            out.println("</table></body></html>");
        } catch (SQLException e) {
            resp.getWriter().println("Error: " + e.getMessage());
        }
    }

    private void getAdminScreen(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY id")) {

            PrintWriter out = resp.getWriter();
            out.println("<html><head><title>Admin Panel</title></head><body>");
            out.println("<h1>Administration Panel</h1>");
            out.println("<p><a href='products'>[Back to Store]</a></p>");

            // 1. Add Book Form
            out.println("<h3>Add New Book</h3>");
            out.println("<form method='POST' action='products'>");
            out.println("<input type='hidden' name='action' value='add'/>");
            out.println("Name: <input type='text' name='name' required/> ");
            out.println("Price: <input type='number' step='0.01' name='price' required/> ");
            out.println("Stock: <input type='number' name='stock' required/> ");
            out.println("<input type='submit' value='Add Book'/>");
            out.println("</form>");
            out.println("<hr/>");

            // 2. Manage Existing Books
            out.println("<h3>Current Inventory</h3>");
            out.println("<table border='1'><tr><th>ID</th><th>Name</th><th>Stock</th><th>Actions</th></tr>");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int stock = rs.getInt("stock");

                out.printf("<tr><td>%d</td><td>%s</td><td>%d</td>", id, name, stock);
                out.printf("<td><form method='POST' action='products' style='margin:0;'>" +
                           "<input type='hidden' name='action' value='delete'/>" +
                           "<input type='hidden' name='productId' value='%d'/>" +
                           "<input type='submit' value='Delete' onclick=\"return confirm('Are you sure?');\"/></form></td></tr>", id);
            }
            out.println("</table></body></html>");
        } catch (SQLException e) {
            resp.getWriter().println("Error: " + e.getMessage());
        }
    }

    // --- ACTION METHODS ---

    private void handleAddBook(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        double price = Double.parseDouble(req.getParameter("price"));
        int stock = Integer.parseInt(req.getParameter("stock"));

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, price, stock) VALUES (?, ?, ?)")) {
            stmt.setString(1, name);
            stmt.setDouble(2, price);
            stmt.setInt(3, stock);
            stmt.executeUpdate();
            
            // Redirect back to admin screen
            resp.sendRedirect("products?action=admin");
        } catch (SQLException e) {
            resp.getWriter().println("Database error: " + e.getMessage());
        }
    }

    private void handleDeleteBook(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("productId"));

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            
            // Redirect back to admin screen
            resp.sendRedirect("products?action=admin");
        } catch (SQLException e) {
            resp.getWriter().println("Database error: " + e.getMessage());
        }
    }

    private void handleOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String productIdStr = req.getParameter("productId");
        int productId = Integer.parseInt(productIdStr);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE products SET stock = stock - 1 WHERE id = ? AND stock > 0")) {
                
                stmt.setInt(1, productId);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    conn.commit();
                    
                    // --- REQUIREMENT 1: Confirmation Screen with Home Button ---
                    resp.setContentType("text/html");
                    PrintWriter out = resp.getWriter();
                    out.println("<html><body>");
                    out.println("<h2 style='color:green'>Success!</h2>");
                    out.println("<p>Order placed successfully. Confirmation email sent.</p>");
                    out.println("<br/>");
                    out.println("<a href='products'><button style='padding:10px; cursor:pointer;'>Return to Store</button></a>");
                    out.println("</body></html>");

                    // Send Email
                    sendEmail("admin@bookstore.com", "New Order Alert", 
                              "Product ID " + productId + " was ordered. Stock reduced.");
                } else {
                    conn.rollback();
                    resp.getWriter().println("Product out of stock!");
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            resp.getWriter().println("Database error: " + e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        if (smtpHost == null || smtpPort == null) {
            System.err.println("SMTP Config missing, skipping email.");
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "false"); 
        Session session = Session.getInstance(props);
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("system@bookstore.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            System.out.println("Email sent successfully to " + to);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}