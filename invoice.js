// Supabase setup 
const SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
const SUPABASE_ANON_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";
const supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

document.addEventListener("DOMContentLoaded", function () {
  // HTML elements
  var ordersBody = document.getElementById("confirmedOrdersBody");
  var noOrdersMsg = document.getElementById("noConfirmedMsg");
  var orderText = document.getElementById("selectedOrderText");
  var invoiceSection = document.getElementById("invoiceSection");
  var invoiceItems = document.getElementById("invoiceItemsBody");
  var itemsTotal = document.getElementById("itemsTotalDisplay");
  var invoiceTotal = document.getElementById("invoiceTotalDisplay");
  var messageBox = document.getElementById("messageBox");

  var currentOrder = null;

  function money(n) {
    n = Number(n);
    if (isNaN(n)) n = 0;
    return "$" + n.toFixed(2);
  }

  function showMessage(msg, type) {
    messageBox.textContent = msg;
    messageBox.className = "message-box";
    if (type) {
      messageBox.classList.add(type);
    }
  }

  function resetInvoice() {
    currentOrder = null;
    invoiceItems.innerHTML = "";
    itemsTotal.textContent = "$0.00";
    invoiceTotal.textContent = "$0.00";
    invoiceSection.style.display = "none";
    orderText.textContent = 'No order selected yet. Click "View".';
    showMessage("");
  }

  // Load confirmed orders from Supabase
  async function loadOrders() {
    resetInvoice();
    showMessage("Loading confirmed orders...");

    var result = await supabase
      .from("orders")
      .select("*")
      .eq("status", "Confirmed")
      .order("created_at", { ascending: false });

    if (result.error) {
      console.log(result.error);
      showMessage("Error loading orders", "error");
      return;
    }

    var data = result.data;
    ordersBody.innerHTML = "";

    if (!data || data.length === 0) {
      noOrdersMsg.style.display = "block";
      showMessage("No confirmed orders.");
      return;
    }

    noOrdersMsg.style.display = "none";

    data.forEach(function (order) {
      var row = document.createElement("tr");

      row.innerHTML =
        "<td>" +
        order.order_id +
        "</td>" +
        "<td>" +
        order.customer_id +
        "</td>" +
        "<td>" +
        order.status +
        "</td>" +
        "<td><button class='btn btn-secondary view-order-btn' " +
        "data-order-id='" +
        order.order_id +
        "' " +
        "data-customer-id='" +
        order.customer_id +
        "' " +
        "data-items='" +
        order.items +
        "' " +
        "data-total='" +
        order.order_total +
        "'>View</button></td>";

      ordersBody.appendChild(row);
    });

    showMessage("");
  }

  
  async function viewOrder(orderId, customerId, itemString, total) {
    resetInvoice();
    invoiceSection.style.display = "block";
    orderText.textContent = "Loading order details...";

    const rawItem = itemString || "";
    const parts = rawItem.split("|"); 
    const productName = (parts[0] || "").trim();
    let qty = null;

    if (parts[1]) {
     0
      const qtyClean = parts[1].replace(/[^0-9.]/g, "");
      qty = Number(qtyClean);
    }

    // Get prodct price 
    var productRes = await supabase
      .from("products")
      .select("unit_price")
      .eq("product_name", productName)
      .limit(1);

    var unitPrice = 0;
    if (productRes.data && productRes.data.length > 0) {
      unitPrice = Number(productRes.data[0].unit_price);
    }

    
    if ((!qty || isNaN(qty)) && unitPrice > 0) {
      qty = total / unitPrice;
    }

    // Get customer info 
    var custRes = await supabase
      .from("customers")
      .select("email, first_name, last_name")
      .eq("id", customerId)
      .limit(1);

    var email = "";
    var name = "";

    if (custRes.data && custRes.data.length > 0) {
      email = custRes.data[0].email;
      name =
        (custRes.data[0].first_name || "") +
        " " +
        (custRes.data[0].last_name || "");
    }

    currentOrder = {
      id: orderId,
      customerId: customerId,
      email: email,
      name: name.trim(),
      product: productName,   
      price: unitPrice,
      qty: qty,
      total: Number(total),
    };

    // Show line item
    invoiceItems.innerHTML =
      "<tr>" +
      "<td>" +
      productName +
      "</td>" +
      "<td>" +
      qty +
      "</td>" +
      "<td>" +
      money(unitPrice) +
      "</td>" +
      "<td>" +
      money(total) +
      "</td>" +
      "</tr>";

    itemsTotal.textContent = money(total);
    invoiceTotal.textContent = money(total);

    orderText.textContent = "Order #" + orderId + " invoice ready.";
  }


async function generateInvoice() {
  if (!currentOrder) {
    showMessage("Select an order first.", "error");
    return;
  }

 
  const { data: existing, error: existingError } = await supabase
    .from("invoices")
    .select("invoice_id")
    .eq("order_id", currentOrder.id);

  if (existingError) {
    console.log("Error checking existing invoice:", existingError);
    showMessage("Could not verify existing invoices.", "error");
    return;
  }

  if (existing && existing.length > 0) {
    showMessage(
      "An invoice already exists for this order. No new invoice was created.",
      "error"
    );
    return;
  }

  const ok = confirm("Send invoice to " + currentOrder.name + "?");
  if (!ok) return;

  const totalNumber = Number(currentOrder.total) || 0;

  // 1. Save invoice to invoices table
  const invoiceData = {
    order_id: currentOrder.id,
    customer_id: currentOrder.customerId,
    items_total: totalNumber.toFixed(2),
    total_amount: totalNumber.toFixed(2),
  };

  const res = await supabase.from("invoices").insert([invoiceData]).select();

  if (res.error) {
    console.log("Invoice insert error:", res.error);
    showMessage("Error creating invoice.", "error");
    return;
  }

  showMessage("Invoice saved. Sending email...");

  //  Call the Edge Function "Send-invoice-via-email"
  const { data, error } = await supabase.functions.invoke(
    "Send-invoice-via--email",
    {
      body: {
        to: currentOrder.email,
        name: currentOrder.name,
        total: totalNumber,
        orderId: currentOrder.id,
      },
    }
  );

  console.log("Function result:", { data, error });

  if (error || (data && data.error)) {
    const errMsg =
      (error && error.message) ||
      (data && data.detail) ||
      "Unknown function error";
    showMessage(
      "Invoice saved, but email failed to send. " + errMsg,
      "error"
    );
    return;
  }

  alert("Invoice email sent to " + currentOrder.email);
  showMessage("Invoice created and email sent!", "success");
}



  // Event listeners
  document
    .getElementById("reloadConfirmedBtn")
    .addEventListener("click", loadOrders);
  document
    .getElementById("clearSelectionBtn")
    .addEventListener("click", resetInvoice);
  document
    .getElementById("generateInvoiceBtn")
    .addEventListener("click", generateInvoice);

  ordersBody.addEventListener("click", function (e) {
    if (e.target.classList.contains("view-order-btn")) {
      var id = Number(e.target.dataset.orderId);
      var cust = e.target.dataset.customerId;
      var item = e.target.dataset.items;  
      var total = Number(e.target.dataset.total);

      viewOrder(id, cust, item, total);
    }
  });

  loadOrders();
});
