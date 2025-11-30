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
		.from("delivery_jobs")
		.select("*")
		.order("job_id", {ascending: true});

	if(error) {
		console.error(error);
		tableBody.innerHTML = "<tr><td colspan='6'>Error loading orders...</td></tr>";
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
			"<td>" + order.job_id + "</td>" +
			"<td>" + order.customer_name + "</td>" +
			"<td>" + order.goods_description + "</td>" +
			"<td>" + order.total_amount + "</td>" +
			"<td>" + order.status + "</td>" +
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
		"<p><strong>Order Number:</strong> " + order.job_id + "</p>" +
		"<p><strong>Customer Name:</strong> " + order.customer_name + "</p>" +
		"<p><strong>Goods:</strong> " + order.goods_description + "</p>" +
		"<p><strong>Total Cost:</strong> " + order.total_amount + "</p>" +
		"<p><strong>Delivery Status:</strong> " + order.status + "</p>";

	detailsMsg.innerHTML = "Showing details for Order " + order.job_id + ".";
}