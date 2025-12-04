const SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
const SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

const db = supabase.createClient(SUPABASE_URL, SUPABASE_KEY);

// var reloadOrders = document.getElementById("reloadConfirmedBtn");
var tableBody = document.getElementById("ordersListBody");
var detailsBox = document.getElementById("orderDetailsBox");
var detailsMsg = document.getElementById("detailsMessage");
var statusSection = document.getElementById("orderStatusSection");
	
var orders = [];

const statusMessages = new Map();
statusMessages.set("Pending", "Please wait as your order is being processed &#9203");
statusMessages.set("Confirmed", "Your order has been confirmed for delivery &#127919");
statusMessages.set("Assigned", "A driver has been assigned to deliver your goods &#128221");
statusMessages.set("In Transit", "Your goods are on their way to their destination &#128284");
statusMessages.set("Delivred", "Your goods have been delivered to the location &#128236");
statusMessages.set("Cancelled", "Your delivery has been cancelled &#10060");

// statusMessages.set("PENDING", "Please wait as your order is being processed &#9203");
// statusMessages.set("CONFIRMED", "Your order has been confirmed for delivery &#127919");
// statusMessages.set("ASSIGNED", "A driver has been assigned to your delivery &#128221");
// statusMessages.set("IN TRANSIT", "Your goods are on their way to their destination &#128284");
// statusMessages.set("DELIVERED", "Your goods have been delivered to the location &#128236");
// statusMessages.set("CANCELLED", "Your delivery has been cancelled &#10060");

document.addEventListener("DOMContentLoaded", () =>{
    loadOrdersFromSupabase();
});
// reloadOrders.addEventListener("click", loadOrdersFromSupabase());

async function loadOrdersFromSupabase() {
	tableBody.innerHTML = "<tr><td colspan='5'>Loading orders...</td></tr>";

	/// TODO: use '.eq()' to filter the orders from the customer that's logged in 
	// Add delivery driver's name and contact to data
	const { data, error } = await db
		.from("delivery_jobs")
		.select(`
			job_id,
			customer_name,
			goods_description,
			status,
			delivery_address,
			delivery_date,
			total_amount
		`)
		// .eq('customer_name', 'Patricia Pill')
		.order("job_id", {ascending: true});
        

	if(error) {
		console.error(error);
		tableBody.innerHTML = "<tr><td colspan='5'>Error loading orders...</td></tr>";
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
		let goods = splitGoods(order);

		row.innerHTML =
			"<td>" + order.job_id + "</td>" +
			"<td>" + order.customer_name + "</td>" +
			// "<td>" + order.goods_description + "</td>" +
			"<td>" + goods.toString() + "</td>" +
			"<td>" + order.status + "</td>" +
			"<td><button class='btn' onclick='showDetails(" + i + ")'>View</button></td>";

		tableBody.appendChild(row);
	}
}


function splitGoods(order) {
	let goods = [];
	split_info = order.goods_description.split(";");
	let count = 0;

	while (count < split_info.length) {
		goods.push(split_info[count]);
		count++;
	}

	for (let i = 0; i < goods.length; i++) {
		goods[i] = String(goods[i]).split('|');

		if (goods[i].length > 1) {
			temp = goods[i][0];
			temp2 = goods[i][1];
			goods[i] = String(temp + " (" + temp2 + ")");
		}
	}
	return goods;
}

function showDetails(index) {
	var order = orders[index];
	let goods = splitGoods(order);
	var rows = tableBody.getElementsByTagName("tr");
	for (var i = 0; i < rows.length; i++) {
		rows[i].classList.remove("selected-row");
	}

	rows[index].classList.add("selected-row");

	detailsBox.innerHTML =
		"<h3> Status: " + statusMessages.get(order.status) + "</h3>" +
		// "<hr>" +
		// "<p><strong>Order Number:</strong> " + order.job_id + "</p>" +
		"<p><strong>Customer Name:</strong> " + order.customer_name + "</p>" +
		"<p><strong>Goods:</strong> " + goods + "</p>" +
		"<p><strong>Total Cost:</strong> $" + Number.parseFloat(order.total_amount).toFixed(2) + "</p>" +
		"<p><strong>Address:</strong> " + order.delivery_address + "</p>" +
		"<p><strong>Delivery Date:</strong> " + order.delivery_date + "</p>" +
		// "<p><strong>Driver:</strong> " + order.name + order.contact + "</p>" +
		"<td><button class='btn btn-primary' onclick='hideDetails()'>Hide</button></td>";


	detailsMsg.innerHTML = "Showing details for Order <strong>" + order.job_id + "</strong>.";
}

function hideDetails(){
	detailsBox.innerHTML = "";
	detailsMsg.innerHTML = "";
}