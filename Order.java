import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
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
    private Double orderTotal;

    public Order(String customerID) {
        Order.orderID++;
        this.customerID = customerID;
        this.orderTotal = 0.0;
        this.tax = 0.15; // default tax rate
        this.discount = 0.0; // default discount rate

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
        id = 1; // this will reset id for future use
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

    private double getTax() {
        return tax;
    }

    private double getDiscount() {
        return discount;
    }

    // -------------------- Add All Items --------------------
    public void getAllItems() {
        System.out.println("Available Items:");
        for (Item item : itemList) {
            System.out.println("Name: " + item.getItemName()
                    + " | Price: " + item.getItemPrice()
                    + " | Stock: " + item.getItemStock());
        }
    }

    // ------------------- Cart Management -------------------
    public void addItemToCart(String product, int quantity) {
        Item foundItem = null;

        for (Item item : itemList) {
            if (item.getItemName().equalsIgnoreCase(product.trim())) {
                foundItem = item;
                break;
            }
        }

        if (foundItem == null) {
            System.out.println("Item not found.");
        }

        if (quantity > foundItem.getItemStock()) {
            System.out.println("Not enough stock available.");
        }

        cart.put(foundItem, cart.getOrDefault(foundItem, 0) + quantity);
    }

    public void removeItemFromCart(String product) {
        Item foundItem = null;

        for (Item item : itemList) {
            if (item.getItemName().equalsIgnoreCase(product.trim())) {
                foundItem = item;
                break;
            }
        }

        if (foundItem != null && cart.containsKey(foundItem)) {
            cart.remove(foundItem);
            System.out.println("Item removed from cart.");
        } else {
            System.out.println("Item not found in cart.");
        }
    }

    public String viewCart() {
        if (cart.isEmpty()) {
            return "Your cart is empty.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\n--- YOUR CART ---").append("\n");
        for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            sb.append("Item: ").append(item.getItemName())
              .append(" | Price: ").append(item.getItemPrice())
              .append(" | Quantity: ").append(quantity).append("\n");
        }
        sb.append("}");

        sb.append("\n----------\n");
        sb.append(printSubtotal());
        return sb.toString();
    }

    public void clearCart() {
        cart.clear();
        orderTotal = 0.0;
        System.out.println("Cart cleared.");
    }

    // -------------- Subtotal and Calculations --------------
    public String printSubtotal() {

        double subTotal = 0;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            double itemTotal = item.getItemPrice() * quantity;

            subTotal += itemTotal;

            sb.append(item.getItemName()).append(String.valueOf(item.getItemPrice())).append(String.valueOf(quantity)).append(itemTotal).append("\n");
        }
        sb.append(calculations(subTotal));
        return sb.toString();
    }

    private String calculations(double subTotal) {
        double discountAmount = subTotal * getDiscount();
        double discountedTotal = subTotal - discountAmount;

        double taxAmount = discountedTotal * getTax();
        double finalTotal = discountedTotal + taxAmount;

        if (finalTotal < 0) {
            finalTotal = 0;
        }

        orderTotal = finalTotal; // final value sent to database

        StringBuilder sb = new StringBuilder();
        sb.append("Subtotal: $").append(String.format("%.2f", subTotal)).append("\n");
        sb.append("Discount (").append(String.format("%.2f", getDiscount() * 100)).append("%): -$").append(String.format("%.2f", discountAmount)).append("\n");
        sb.append("Tax (").append(String.format("%.2f", getTax() * 100)).append("%): +$").append(String.format("%.2f", taxAmount)).append("\n");
        sb.append("Total: $").append(String.format("%.2f", finalTotal)).append("\n");

        return sb.toString();
    }

    public void checkout() {
        while (!cart.isEmpty()) {
            try {
                for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
                    Item item = entry.getKey();
                    int quantity = entry.getValue();
                    item.setItemStock(item.getItemStock() - quantity);
                    //add database update logic here for item stock and order creation
                    saveProductsToSupabase(item.getItemID());
                }
                saveOrderToSupabase();
                clearCart();
            } catch (IOException | InterruptedException e) {
                System.out.println("Error during checkout: " + e.getMessage());
            }
        }
    }

    // ------------------- Supabase helpers / JSON -------------------
    public String toJsonOrder() {
        StringBuilder sb = new StringBuilder();
        StringBuilder customerItems = new StringBuilder();
        sb.append("{");
        sb.append("\"customer_id\":\"").append(escapeJson(customerID)).append("\",");
        for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            customerItems.append(escapeJson(item.getItemName())).append("|").append(quantity).append(";");
        }
        sb.append("\"items\":\"").append(escapeJson(customerItems.toString())).append("\",");
        sb.append("\"order_total\":\"").append(escapeJson(String.valueOf(orderTotal))).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public String toJsonProducts(Integer id) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<Item, Integer> entry : cart.entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            if (item.getItemID() == id) {
                String newQuantity = String.valueOf(item.getItemStock());
                sb.append("\"product_id\":").append(escapeJson(String.valueOf(id))).append(",");
                sb.append("\"stock\":\"").append(escapeJson(newQuantity)).append("\"");
                break;
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public boolean saveOrderToSupabase() throws IOException, InterruptedException {
        String body = "[" + toJsonOrder() + "]";
        HttpResponse<String> resp = SupabaseClient.postUpsert("orders", body, null, null);
        return resp.statusCode() >= 200 && resp.statusCode() < 300;
    }

    public boolean saveProductsToSupabase(Integer id) throws IOException, InterruptedException {
        String body = "[" + toJsonProducts(id) + "]";
        String idString = String.valueOf(id);
        HttpResponse<String> resp = SupabaseClient.postUpsert("products", body, "product_id", null);
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