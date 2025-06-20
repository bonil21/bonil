package manginasalordersystem;

import java.sql.*;
import java.util.Scanner;

public class MangInasalOrderSystem {
    private static final String URL = "jdbc:mysql://localhost:3306/manginasal";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("✅ Connected to the database successfully!");

            int orderId = createOrder(conn);
            if (orderId == -1) return;

            placeOrder(scanner, conn, orderId);
            double totalAmount = calculateTotal(conn, orderId);

            if (totalAmount > 0) {
                System.out.println("\nTotal Bill: PHP " + String.format("%.2f", totalAmount));
                processPayment(scanner, conn, orderId, totalAmount);

                // ✅ Display normalized data
                displayNormalizationForms(conn, orderId);
            } else {
                System.out.println("❌ Order is empty. Payment cannot be processed.");
            }

            System.out.println("\nThank you for ordering at Mang Inasal!");
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    private static int createOrder(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Orders (TotalAmount) VALUES (0)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static void placeOrder(Scanner scanner, Connection conn, int orderId) throws SQLException {
        while (true) {
            displayMenu(conn);
            System.out.print("Enter item number (0 to finish): ");
            int choice = getIntInput(scanner);
            if (choice == 0) break;

            System.out.print("Enter quantity: ");
            int quantity = getIntInput(scanner);
            if (quantity > 0) addOrderItem(conn, orderId, choice, quantity);
        }
    }

    private static void displayMenu(Connection conn) throws SQLException {
        System.out.println("\nMenu:");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Menu")) {
            while (rs.next()) {
                System.out.println(rs.getInt("MenuID") + ". " + rs.getString("ItemName") + " - PHP " + rs.getDouble("Price"));
            }
        }
    }

    private static void addOrderItem(Connection conn, int orderId, int menuId, int quantity) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT Price FROM Menu WHERE MenuID = ?")) {
            stmt.setInt(1, menuId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double subtotal = rs.getDouble("Price") * quantity;
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO OrderDetails (OrderID, MenuID, Quantity, Subtotal) VALUES (?, ?, ?, ?)")) {
                    insertStmt.setInt(1, orderId);
                    insertStmt.setInt(2, menuId);
                    insertStmt.setInt(3, quantity);
                    insertStmt.setDouble(4, subtotal);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    private static double calculateTotal(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT SUM(Subtotal) AS Total FROM OrderDetails WHERE OrderID = ?")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double total = rs.getDouble("Total");
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE Orders SET TotalAmount = ? WHERE OrderID = ?")) {
                    updateStmt.setDouble(1, total);
                    updateStmt.setInt(2, orderId);
                    updateStmt.executeUpdate();
                }
                return total;
            }
        }
        return 0;
    }

    private static void processPayment(Scanner scanner, Connection conn, int orderId, double totalAmount) throws SQLException {
        System.out.println("\n1. Cash\n2. GCash\n3. Credit/Debit Card");
        System.out.print("Select Payment Method (1-3): ");
        int paymentMethodID = getIntInput(scanner, 1, 3);

        double amountPaid = totalAmount;
        double change = 0;
        if (paymentMethodID == 1) {
            System.out.print("\nEnter cash amount paid: PHP ");
            amountPaid = getPositiveDouble(scanner, totalAmount);
            change = amountPaid - totalAmount;
            System.out.println("Change: PHP " + String.format("%.2f", change));
        } else {
            System.out.println("✅ Payment Successful!");
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Payments (OrderID, PaymentMethodID, AmountPaid, ChangeAmount) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, paymentMethodID);
            stmt.setDouble(3, amountPaid);
            stmt.setDouble(4, change);
            stmt.executeUpdate();
        }
    }

    private static int getIntInput(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Try again: ");
            scanner.next();
        }
        return scanner.nextInt();
    }

    private static int getIntInput(Scanner scanner, int min, int max) {
        int input;
        do {
            input = getIntInput(scanner);
            if (input < min || input > max) System.out.print("Invalid choice. Try again: ");
        } while (input < min || input > max);
        return input;
    }

    private static double getPositiveDouble(Scanner scanner, double min) {
        double value;
        do {
            while (!scanner.hasNextDouble()) {
                System.out.print("Invalid input. Try again: ");
                scanner.next();
            }
            value = scanner.nextDouble();
            if (value < min) System.out.print("Enter at least PHP " + min + ": ");
        } while (value < min);
        return value;
    }

    // ✅ NEW: Display normalization forms
    private static void displayNormalizationForms(Connection conn, int orderId) throws SQLException {
        System.out.println("\n=== 1NF ===");
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT OrderDetailID, OrderID, MenuID, Quantity, Subtotal FROM OrderDetails WHERE OrderID=?")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-15s%-10s%-10s%-10s%-10s\n",
                    "OrderDetailID","OrderID","MenuID","Quantity","Subtotal");
            while (rs.next()) {
                System.out.printf("%-15d%-10d%-10d%-10d%-10.2f\n",
                        rs.getInt("OrderDetailID"), rs.getInt("OrderID"), rs.getInt("MenuID"),
                        rs.getInt("Quantity"), rs.getDouble("Subtotal"));
            }
        }

        System.out.println("\n=== 2NF (Grouped by MenuID) ===");
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT MenuID, SUM(Quantity) AS TotalQty FROM OrderDetails WHERE OrderID=? GROUP BY MenuID")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-10s%-10s\n","MenuID","TotalQty");
            while (rs.next()) {
                System.out.printf("%-10d%-10d\n", rs.getInt("MenuID"), rs.getInt("TotalQty"));
            }
        }

        System.out.println("\n=== 3NF (Separate Entities) ===");

        // Order Info
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT OrderID, TotalAmount FROM Orders WHERE OrderID=?")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("\nOrder Info:");
            while (rs.next()) {
                System.out.println("OrderID: " + rs.getInt("OrderID") +
                                   ", TotalAmount: " + rs.getDouble("TotalAmount"));
            }
        }

        // Menu Info
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT DISTINCT m.MenuID, m.ItemName, m.Price " +
                "FROM Menu m JOIN OrderDetails od ON m.MenuID=od.MenuID WHERE od.OrderID=?")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("\nMenu Info:");
            while (rs.next()) {
                System.out.println("MenuID: " + rs.getInt("MenuID") +
                                   ", ItemName: " + rs.getString("ItemName") +
                                   ", Price: PHP " + rs.getDouble("Price"));
            }
        }

        // Payment Info
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT PaymentMethodID, AmountPaid, ChangeAmount, PaymentDate FROM Payments WHERE OrderID=?")) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("\nPayment Info:");
            while (rs.next()) {
                System.out.println(
                        "MethodID: " + rs.getInt("PaymentMethodID") +
                        ", AmountPaid: PHP " + rs.getDouble("AmountPaid") +
                        ", Change: PHP " + rs.getDouble("ChangeAmount") +
                        ", Date: " + rs.getTimestamp("PaymentDate"));
            }
        }
    }
}
