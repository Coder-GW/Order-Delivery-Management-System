<?php
session_start();

if (!isset($_SESSION['user_id'])) {
	header('Location: index.html');
	exit;
}

$SUPABASE_URL = "https://bmqvkxfvljxlgynxruga.supabase.co";
$SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

if($_SERVER['REQUEST_METHOD'] ==='POST'){
	$field = $_POST['field']?? '';
	$userId = (int)($_SESSION['user_id']);

	$updateData = [];

	switch($field) {
		case'first_name':
		case'last_name':
			$value = trim($_POST['new_value'] ?? '');
			if(!preg_match('/^[A-Za-z\-]+$/',$value)){
				die("Invalid name format.");
			}
			$updateData[$field] = $value;
			$_SESSION[$field] = $value;
			break;

		case 'street':
			$value = trim($_POST['new_value'] ?? '');
			if ($value === ''){
				die("Street cannot be empty.");
			}
			$updateData['street'] = $value;
			$_SESSION['street'] = $value;
			break;

		case 'city':
			$value = trim($_POST['new_value'] ?? '');
			if (!preg_match('/^[A-Za-z ]+$/', $value)){
				die("City must contain letters and spaces only.");
			}
			$updateData['city'] = $value;
			$_SESSION['city'] = $value;
			break;

		case 'email':
			$email = trim($_POST['new_value'] ?? '');
			if (!preg_match('/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/', $email)){
				die("Invalid email format.");
			}
			$updateData['email'] = $email;
			$_SESSION['email'] = $email;
			break;

		case 'phone_number':
			$phone = trim($_POST['new_value'] ?? '');
			if (!preg_match('/^[0-9]{10}$/',$phone)){
				die("Phone number must be exactly 10 digits.");
			}
			$updateData['phone_number'] = $phone;
			$_SESSION['phone_number'] = $phone;
			break;

		case 'parish':
			$parish = trim($_POST['new_parish'] ?? '');
			$validParishes = [
				"Kingston", "St. Andrew", "St. Thomas","Portland",
				"St. Mary","St. Ann","Trelawny","St. James",
				"Hanover","Westmoreland","St. Elizabeth","Manchester",
				"Clarendon","St. Catherine"
			];
			if (!in_array($parish, $validParishes, true)){
				die("Invalid parish selected.");
			}
			$updateData['parish'] = $parish;
			$_SESSION['parish'] = $parish;
			break;

		case 'password':
			$newPassword = $_POST['new_password'] ?? '';
			$confirm = $_POST['confirm_password'] ?? '';

			if($newPassword !== $confirm){
				die("Passwords do not match.");
			}
			if (!preg_match('/^(?=.*[0-9]).{8,}$/', $newPassword)) {
				die("Password must be at least 8 characters and contain at least one number.");
			}
			$updateData['password'] = $newPassword;
			break;

		default:
			die("Invalid field.");
	}

	if(empty($updateData)){
		die("Nothing to update.");
	}

	$options = [
		'http'=> [
		'method' => 'PATCH',
		'header'=>
			"apikey:$SUPABASE_KEY\r\n".
			"Authorization: Bearer $SUPABASE_KEY\r\n".
			"Content-Type: application/json\r\n".
			"Prefer: return=minimal\r\n",
		'content'=>json_encode($updateData),
		],
	];

	$context = stream_context_create($options);
	$url = $SUPABASE_URL."/rest/v1/customers?id=eq.$userId";

	$result = file_get_contents($url,false,$context);

	if($result === FALSE){
		die("Error updating account information.");
	} else {
		header('Location:acc_info.php');
		exit;
	}
} else {
	header('Location: acc_info.php');
	exit;
}
?>