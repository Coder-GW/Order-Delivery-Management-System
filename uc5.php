<?php
session_start();

if(!isset($_SESSION['user_id'])) {
	header('Location: index.html');
	exit;
}

$customerId = $_SESSION['user_id'];
?>

<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
    	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>Use Case 5 - Display Order Information</title>
	<link rel="stylesheet" href="uc5.css">

	<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/dist/umd/supabase.min.js"></script>

	<script>
		const CURRENT_CUSTOMER_ID = <?php echo json_encode($customerId); ?>;
	</script>
</head>

<body>

<header>
	<h1>Order Information</h1>
</header>
<main>
<button id="viewOrdersButton" type="button">View Orders</button>
</main>

<div id= "ordersSection" style="display: none;">

	<h2>Active Orders</h2>

	<table id="ordersTable">
		<caption>Orders in the System</caption>

		<thead>
			<tr>
				<th scope="col">Order Number</th>
				<th scope="col">Customer ID</th>
				<th scope="col">Goods</th>
				<th scope="col">Total Cost</th>
				<th scope="col">Delivery Status</th>
				<th scope="col">Action</th>
			</tr>
		</thead>

		<tbody id="ordersTableBody">
			<!-- Javascript adds the rows -->
		</tbody>
	</table>

	<h2>Order Details</h2>
	<p id="detailsMessage">Select "View Details" to see information.</p>

	<div id="orderDetailsBox"></div>
</div>

	<script src="uc5.js"></script>

<footer>
    <div class="container">
            <p>First Class Hardware Limited </p>
    </div>
</footer>
</body>
</html>