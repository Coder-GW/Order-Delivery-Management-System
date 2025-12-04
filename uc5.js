
const SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
const SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

const db = supabase.createClient(SUPABASE_URL, SUPABASE_KEY);


var viewBtn = document.getElementById("viewOrdersButton");
var tableBody = document.getElementById("ordersTableBody");
var detailsBox = document.getElementById("orderDetailsBox");
var detailsMsg = document.getElementById("detailsMessage");
var ordersSection = document.getElementById("ordersSection");

var orders = [];

viewBtn.addEventListener("click", toggleOrdersSection);

function toggleOrdersSection() {
	if (ordersSection.style.display === "none" || ordersSection.style.display === "") {
		ordersSection.style.display = "block";
		viewBtn.innerHTML = "Hide Orders";

		loadOrdersFromSupabase();
	} else {
		ordersSection.style.display = "none";
		viewBtn.innerHTML = "View Orders";

		tableBody.innerHTML = "";
		detailsBox.innerHTML = "";
		detailsMsg.innerHTML = 'Select "View Details" to see more information.';
	}
}


async function loadOrdersFromSupabase() {
	tableBody.innerHTML = "<tr><td colspan='6'>Loading orders...</td></tr>";

	const { data, error } = await db
		.from("orders")
		.select("*")
		.eq("customer_id", CURRENT_CUSTOMER_ID)
		.order("created_at", {ascending: false});

	if(error) {
		console.error(error);
		tableBody.innerHTML = "<tr><td colspan='6'>Error loading orders...</td></tr>";
		detailsMsg.innerHTML = "There was an error loading oders.";
		return;
	}

	if (!data || data.length === 0) {
		orders = [];
		tableBody.innerHTML = "<tr><td colspan='6'>No orders found.</td></tr>";
		detailsBox.innerHTML = "";
		detailsMsg.innerHTML = "No orders found. Once orders are created, they will appear here.";
		return;
	}

	orders = data;
	displayOrders();
}


function displayOrders() {
	tableBody.innerHTML = "";
	detailsBox.innerHTML = "";
	detailsMsg.innerHTML = "Select an order to see more details.";

	for (let i = 0; i < orders.length; i++) {
		let order = orders[i];

		let row = document.createElement("tr");

		row.innerHTML =
			"<td>" + (order.order_id ?? "") + "</td>" +
			"<td>" + (order.customer_id ?? "") + "</td>" +
			"<td>" + (order.items ?? "") + "</td>" +
			"<td>" + (order.order_total ?? "") + "</td>" +
			"<td>" + (order.status ?? "") + "</td>" +
			"<td><button onclick='showDetails(" + i + ")'>View Details</button></td>";

		tableBody.appendChild(row);
	}
}


function showDetails(index) {
	var order = orders[index];

	var rows = tableBody.getElementsByTagName("tr");
	for (var i = 0; i < rows.length; i++) {
		rows[i].classList.remove("selected-row");
	}

	rows[index].classList.add("selected-row");

	detailsBox.innerHTML =
		"<p><strong>Order Number:</strong> " + (order.order_id ?? "") + "</p>" +
		"<p><strong>Customer Name:</strong> " + (order.customer_id ?? "") + "</p>" +
		"<p><strong>Goods:</strong> " + (order.items ?? "") + "</p>" +
		"<p><strong>Total Cost:</strong> " + (order.order_total ?? "") + "</p>" +
		"<p><strong>Delivery Status:</strong> " + (order.status ?? "") + "</p>";

	detailsMsg.innerHTML = "Showing details for Order " + (order.order_id ?? "") + ".";
}