public class Item {
    private Integer itemID;
    private String itemName;
    private Double itemPrice;
    private Integer itemStock;

    public Item(Integer itemID, String itemName, Double itemPrice, Integer itemStock) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.itemStock = itemStock;
    }

    public Integer getItemID() {
        return itemID;
    }

    public String getItemName() {
        return itemName;
    }

    public Double getItemPrice() {
        return itemPrice;
    }

    public Integer getItemStock() {
        return itemStock;
    }

    public void setItemStock(Integer itemStock) {
        this.itemStock = itemStock;
        // Additional logic to update the database can be added here
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item other = (Item) obj;
        return itemName.equalsIgnoreCase(other.itemName);
    }

    @Override
    public int hashCode() {
        return itemName.toLowerCase().hashCode();
    }
}