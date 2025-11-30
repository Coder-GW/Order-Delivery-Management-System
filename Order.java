import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

public class Order {
    private static Integer orderID = 0;
    private String customerID;
    private Date orderDate = new Date();
    private Double tax;
    private Double discount;
    private Item newItem;
    private List<Item> itemList = new ArrayList<>();
    private Map<Item, Integer> cart = new HashMap<>();
    private boolean checkout = false;
    private Double orderTotal;

    public Order(String customerID) {
        Order.orderID++;
        this.customerID = customerID;
        this.orderTotal = 0.0;

        //pull the data from database for tax and discount rates
        this.tax = 0.15; // Example tax rate, replace with database call
        this.discount = 0.0; // Example discount rate, replace with database call

        if (tax < 0 || discount < 0) {
            System.out.println("Invalid tax or discount rate. Setting to default values.");
            this.tax = 0.15;
            this.discount = 0.0;
        }

        Integer id=1;
        do {
             try {
                newItem = fetchFromSupabase(id);
            } catch (IOException | InterruptedException e) {
                newItem = null;
            }
            id++;
            if (newItem != null) {
                itemList.add(newItem);
            }
        }while (newItem != null);
        id = 1; // reset id for future use

        try {
            getOrder();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public String getCustomerID() {
        return customerID;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public double getOrderTotal() {
        return orderTotal;
    }

    public void setCheckout(Boolean status) {
        this.checkout = status;
    }

    public void printSubtotal(double taxRate, double discountRate) {
        if (cart.isEmpty()) {
            System.out.println("\n--- CART IS EMPTY ---");
            return;
        }

        System.out.println("\n--------- CART SUBTOTAL ---------");
        System.out.printf("%-15s %-10s %-10s %-10s%n", 
            "Item", "Price", "Qty", "Sub Total");
        System.out.println("--------------------------------");

        double subTotal = 0;

        for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            double itemTotal = item.getItemPrice() * quantity;

            subTotal += itemTotal;

            System.out.printf("%-15s $%-9.2f %-10d $%-9.2f%n",
                item.getItemName(),
                item.getItemPrice(),
                quantity,
                itemTotal
            );
        }

        // ===== FINANCIAL CALCULATIONS =====
        if (discountRate > 1.0) {
            discountRate = 1.0;
        }

        double discountAmount = subTotal * discountRate;
        double discountedTotal = subTotal - discountAmount;

        double taxAmount = discountedTotal * taxRate;
        double finalTotal = discountedTotal + taxAmount;

        if (finalTotal < 0) {
            finalTotal = 0;
        }

        System.out.println("--------------------------------");
        System.out.printf("SUBTOTAL:        $%.2f%n", subTotal);
        System.out.printf("DISCOUNT (%.0f%%):  -$%.2f%n", discountRate * 100, discountAmount);
        System.out.printf("TAX (%.0f%%):        +$%.2f%n", taxRate * 100, taxAmount);
        System.out.println("--------------------------------");
        System.out.printf("FINAL TOTAL:     $%.2f%n", finalTotal);
        System.out.println("--------------------------------\n");

        orderTotal = finalTotal; // ✅ final value sent to database
    }

    public void clearCart() {
        cart.clear();
        orderTotal = 0.0;
        System.out.println("Cart cleared.");
    }

    public void getOrder() throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        checkout = false;

        System.out.println("Welcome!\nAvailable Items:");
        for (Item item : itemList) {
            System.out.println("Name: " + item.getItemName()
                    + " | Price: " + item.getItemPrice()
                    + " | Stock: " + item.getItemStock());
        }

        while (!checkout) {

            // ===================== ADD ITEMS =====================
            while (true) {
                System.out.print("Enter item name (-1 to checkout): ");
                String userInput = sc.nextLine();

                if (userInput.equals("-1")) {
                    break;
                }

                Item foundItem = null;

                for (Item item : itemList) {
                    if (item.getItemName().equalsIgnoreCase(userInput.trim())) {
                        foundItem = item;
                        break;
                    }
                }

                if (foundItem == null) {
                    System.out.println("Item not found.");
                    continue;
                }

                System.out.print("Enter quantity: ");
                int quantity;

                try {
                    quantity = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number.");
                    continue;
                }

                if (quantity <= 0) {
                    System.out.println("Quantity must be positive.");
                    continue;
                }

                if (quantity > foundItem.getItemStock()) {
                    System.out.println("Not enough stock available.");
                    continue;
                }

                cart.put(foundItem, cart.getOrDefault(foundItem, 0) + quantity);
                System.out.println("Item added to cart.");

                printSubtotal(tax, discount); // ✅ live subtotal after every add
            }

            // ===================== CHECKOUT OR CANCEL =====================
            if (cart.isEmpty()) {
                System.out.println("Your cart is empty.");
                continue;
            }

            printSubtotal(tax, discount);

            System.out.println("Press Enter to proceed to checkout or -1 to cancel order and clear cart.");
            String choice = sc.nextLine();

            if (choice.equals("-1")) {
                clearCart();
                continue;
            }

            if (cart.isEmpty()) {
                System.out.println("Cannot checkout with an empty cart.");
                continue;
            }

            // ===================== FINALIZE CHECKOUT =====================
            for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();
                item.setItemStock(item.getItemStock() - quantity);
                //add database update logic here for item stock and order creation
                saveToSupabase();
            }

            checkout = true;
            System.out.println("Checkout complete. Thank you for your purchase!");
        }
    }

    // ------------------- Supabase helpers / JSON -------------------
    // Note: ensure Supabase table "delivery_drivers" has columns:
    // driver_id, name, contact_number, license_number, vehicle_info, is_available
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"order_id\":\"").append(escapeJson(String.valueOf(orderID))).append("\",");
        sb.append("\"customer_id\":\"").append(escapeJson(customerID)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJson() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("orders", body, "order_id", null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    private static String extractJsonString(String json, String key) {
        String look = "\"" + key + "\"";
        int idx = json.indexOf(look);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx + look.length());
        if (colon == -1) return "";
        int start = json.indexOf("\"", colon);
        if (start == -1) {
            int end = json.indexOf(",", colon);
            if (end == -1) end = json.length();
            return json.substring(colon+1, end).trim().replaceAll("[,\\s\"]", "");
        }
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }

    public static Item fetchFromSupabase(Integer productID) throws IOException, InterruptedException {
        String id = String.valueOf(productID);
        String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
        String query = "products?select=*&product_id=eq." + encodedId;
        HttpResponse<String> resp = SupabaseClient.get(query, null);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            String body = resp.body();
            if (body != null && body.trim().startsWith("[") && body.trim().length() > 2) {
                String obj = body.trim();
                obj = obj.substring(1, obj.length()-1).trim();
                String product = extractJsonString(obj, "product_name");
                String stock = extractJsonString(obj, "stock");
                String uPrice = extractJsonString(obj, "unit_price");
                Item i = new Item(productID, product, Double.parseDouble(uPrice), Integer.parseInt(stock));
                return i;
            }
        }
        return null;
    }
}