package COMP2140;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

public class Order {
    private String orderID;
    private String customerID;
    private Date orderDate;
    private Double tax;
    private Double discount;
    private Integer numberOfItems;
    private List<Item> itemList = new ArrayList<>();
    private Map<Item, Integer> cart = new HashMap<>();
    private boolean checkout = false;
    private Double orderTotal;

    public Order(String orderID, String customerID, Date orderDate) {
        this.orderID = orderID;
        this.customerID = customerID;
        this.orderDate = orderDate;
        this.orderTotal = 0.0;

        //pull the data from database for tax and discount rates
        this.tax = 0.15; // Example tax rate, replace with database call
        this.discount = 0.0; // Example discount rate, replace with database call

        if (tax < 0 || discount < 0) {
            System.out.println("Invalid tax or discount rate. Setting to default values.");
            this.tax = 0.15;
            this.discount = 0.0;
        }

        //pull the data from database for number of items to be used for the number of items as well as the actual item listings to be added
        this.numberOfItems = 0;//example value, replace with database call

        for (int i = 1; i <= numberOfItems; i++) {
            itemList.add(new Item(00000001, "Item" + i, 10.0 * i, 100)); // Example items
        }
    }

    public String getOrderID() {
        return orderID;
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

    public void getOrder() {
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
            }

            checkout = true;
            System.out.println("Checkout complete. Thank you for your purchase!");
        }

        sc.close();
    }
}