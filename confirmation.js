// Supabase setup 
const SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
const SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

const supabaseClient = supabase.createClient(SUPABASE_URL, SUPABASE_KEY);


// Page elements 
const ordersBody  = document.getElementById("ordersBody");
const noOrdersMsg = document.getElementById("noOrdersMsg");
const detailsBox  = document.getElementById("detailsBox");
const btnConfirm  = document.getElementById("btnConfirm");
const btnClear    = document.getElementById("btnClear");
const btnReload   = document.getElementById("btnReload");
const message     = document.getElementById("message");

// active selected order
let selectedOrder = null;

//  Helpers 
function showMessage(text, isError) {
  message.textContent = text;
  message.className = "";

  if (!text) return;

  if (isError) {
    message.classList.add("error");
  } else {
    message.classList.add("success");
  }
}

// remove digits
function cleanItemName(rawItem) {
  if (!rawItem) return "";
  return rawItem.replace(/[^a-zA-Z\s]/g, "").trim();
}

// update details preview
function updateDetailsBox() {
  if (selectedOrder === null) {
    detailsBox.innerHTML =
      '<p>No order selected yet. Click "View" on an order above.</p>';
    btnConfirm.disabled = true;
    return;
  }

  let itemName = cleanItemName(selectedOrder.items);

  let html = "";
  html += "<p><strong>Order ID:</strong> " + selectedOrder.order_id + "</p>";
  html += "<p><strong>Customer ID:</strong> " + selectedOrder.customer_id + "</p>";
  html += "<p><strong>Status:</strong> " + selectedOrder.status + "</p>";
  html += "<p><strong>Items:</strong> " + itemName + "</p>";
  html += "<p><strong>Order Total:</strong> $" + selectedOrder.order_total + "</p>";

  detailsBox.innerHTML = html;

  let statusText = (selectedOrder.status || "").toLowerCase();
  btnConfirm.disabled = statusText.indexOf("pending confirmation") === -1;
}

// Load pending orders 
async function loadPendingOrders() {
  showMessage("");
  selectedOrder = null;
  updateDetailsBox();

  ordersBody.innerHTML = "<tr><td colspan='4'>Loading...</td></tr>";
  noOrdersMsg.classList.add("hidden");

  const result = await supabaseClient
    .from("orders")
    .select("order_id, customer_id, status, items, order_total")
    .ilike("status", "%Pending Confirmation%")
    .order("order_id", { ascending: true });

  const data = result.data;
  const error = result.error;

  if (error) {
    console.log(error);
    ordersBody.innerHTML = "";
    showMessage("Error loading orders.", true);
    return;
  }

  if (!data || data.length === 0) {
    ordersBody.innerHTML = "";
    noOrdersMsg.classList.remove("hidden");
    return;
  }

  ordersBody.innerHTML = "";

  for (let i = 0; i < data.length; i++) {
    const order = data[i];

    const tr = document.createElement("tr");

    const tdOrderId = document.createElement("td");
    tdOrderId.textContent = order.order_id;

    const tdCustomerId = document.createElement("td");
    tdCustomerId.textContent = order.customer_id;

    const tdStatus = document.createElement("td");
    tdStatus.textContent = order.status;

    const tdView = document.createElement("td");
    const viewBtn = document.createElement("button");
    viewBtn.textContent = "View";

    viewBtn.addEventListener("click", function () {
      selectedOrder = order;
      updateDetailsBox();
      showMessage("");
    });

    tdView.appendChild(viewBtn);

    tr.appendChild(tdOrderId);
    tr.appendChild(tdCustomerId);
    tr.appendChild(tdStatus);
    tr.appendChild(tdView);

    ordersBody.appendChild(tr);
  }
}

//Validation
function checkMissingFields(order) {
  let missing = [];

  if (!order.customer_id) {
    missing.push("Customer ID");
  }
  if (!order.items) {
    missing.push("Items");
  }
  if (!order.order_total && order.order_total !== 0) {
    missing.push("Order Total");
  }
  if (!order.status) {
    missing.push("Status");
  }

  return missing;
}

//Confirm order 
async function confirmSelectedOrder() {
  if (selectedOrder === null) {
    showMessage("No order selected.", true);
    return;
  }

  // check missing required fields
  let missingInfo = checkMissingFields(selectedOrder);

  if (missingInfo.length > 0) {
    showMessage(
      "Order cannot be confirmed. Missing: " + missingInfo.join(", "),
      true
    );
    return;
  }

  const result = await supabaseClient
    .from("orders")
    .update({ status: "Confirmed" })
    .eq("order_id", selectedOrder.order_id);

  const error = result.error;

  if (error) {
    console.log(error);
    showMessage("Error confirming order.", true);
    return;
  }

  showMessage("Order confirmed successfully.");
  loadPendingOrders();
}

// Clear selection 
function clearSelection() {
  selectedOrder = null;
  updateDetailsBox();
  showMessage("");
}

// Event listeners
btnReload.addEventListener("click", loadPendingOrders);
btnClear.addEventListener("click", clearSelection);
btnConfirm.addEventListener("click", confirmSelectedOrder);

document.addEventListener("DOMContentLoaded", loadPendingOrders);
