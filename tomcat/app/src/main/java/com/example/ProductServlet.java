package com.example;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import jakarta.servlet.annotation.WebServlet;
@WebServlet("/products")


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
            throw new ServletException("Database environment variables (DB_URL, DB_USER, DB_PASS) not set!");
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("Postgres JDBC driver not found", e);
        }


        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
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
        String action = req.getParameter("action");
        if (action == null || action.equals("list")) {
            getProducts(resp);
        } else {
            resp.getWriter().println("Unknown action: " + action);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String productIdStr = request.getParameter("productId");
        
        if (productIdStr == null || productIdStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing productId parameter");
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(productIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid productId");
            return;
        }

        processOrder(productId, response);
    }

    private void processOrder(int productId, HttpServletResponse response) throws IOException {
        try (Connection conn = getConnection()) {
            // Transactional update
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE products SET stock = stock - 1 WHERE id = ? AND stock > 0")) {
                
                stmt.setInt(1, productId);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    conn.commit();
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("Order placed successfully. Confirmation email sent.");
                    
                    // Requirement: Send Email on update 
                    sendEmail("admin@bookstore.com", "New Order Alert", 
                              "Product ID " + productId + " was ordered. Stock reduced.");
                } else {
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.getWriter().write("Product out of stock or does not exist");
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void getProducts(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY id")) {

            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2>Bookstore Inventory</h2>");
            out.println("<table border='1'><tr><th>ID</th><th>Name</th><th>Price</th><th>Stock</th><th>Action</th></tr>");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                out.printf("<tr><td>%d</td><td>%s</td><td>$%.2f</td><td>%d</td>", id, name, price, stock);
                
                // Add a simple form to trigger the POST order
                out.printf("<td><form method='POST' action='products'>" +
                           "<input type='hidden' name='productId' value='%d'/>" +
                           "<input type='submit' value='Order'/></form></td></tr>", id);
            }
            out.println("</table></body></html>");
        } catch (SQLException e) {
            resp.getWriter().println("Error fetching products: " + e.getMessage());
        }
    }

    /**
     * Sends an email via the MailHog container */
    private void sendEmail(String to, String subject, String body) {
        if (smtpHost == null || smtpPort == null) {
            System.err.println("SMTP Config missing, skipping email.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        // MailHog does not require authentication by default
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
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
