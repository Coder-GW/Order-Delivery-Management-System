
<?php 
session_start();

if(!isset($_SESSION['user_id'])) {
	header('Location: index.html');
	exit;
}

$customerId = $_SESSION['user_id'];

$SUPABASE_URL="https://bmqvkxfvljxlgynxruga.supabase.co";
$SUPABASE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

if ($_SERVER['REQUEST_METHOD']==='POST'){
    $quantityRaw=htmlspecialchars(trim($_POST['Quantity']?? ''));
    $itemRaw=htmlspecialchars(trim($_POST['item']?? ''));
   
    $quantity=(int)$quantityRaw;

    $itemlst=[
        "Beef"=>600,
        "Chicken"=>800,
        "Bread"=>400

    ];

    $price=$itemlst[$itemRaw]*$quantityRaw;







    $data=[
        'customer_id'=>$customerId,
        'items'=>$itemRaw,
        'status'=>'Pending Confirmation',
        'order_total'=>$price
        
    ];

    $options=[
        'http' =>[
            'method'=>'POST',
            'header'=>"apikey:$SUPABASE_KEY\r\nAuthorization:Bearer $SUPABASE_KEY\r\nContent-Type:application/json\r\n",
            'content'=>json_encode($data),
        ],
    ];
    $context=stream_context_create($options);
    $result=file_get_contents("$SUPABASE_URL/rest/v1/orders",false,$context);

    if($result===FALSE){
        echo "error inserting ";
    }
    else{
    header("Location:acc_info.php"); 
    }
   
}
?>