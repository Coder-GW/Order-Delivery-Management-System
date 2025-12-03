<?php 

$SUPABASE_URL="https://bmqvkxfvljxlgynxruga.supabase.co";
$SUPABASE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

if ($_SERVER['REQUEST_METHOD']==='POST'){
    $username=htmlspecialchars(trim($_POST['Username']?? ''));
    $password=htmlspecialchars(trim($_POST['Password']?? ''));
    $first_name=htmlspecialchars(trim($_POST['FirstName']?? ''));
    $last_name=htmlspecialchars(trim($_POST['LastName']?? ''));
    $email=htmlspecialchars(trim($_POST['Email']?? ''));
    $phone_number=htmlspecialchars(trim($_POST['PhoneNumber']?? ''));
    $street=htmlspecialchars(trim($_POST['Street']?? ''));
    $city=htmlspecialchars(trim($_POST['City']?? ''));
    $parish=htmlspecialchars(trim($_POST['Parish']?? ''));

    $data=[
        'username'=>$username,
        'password'=>$password,
        'first_name'=>$first_name,
        'last_name'=>$last_name,
        'email'=>$email,
        'phone_number'=>$phone_number,
        'street'=>$street,
        'city'=>$city,
        'parish'=>$parish

    ];

    $options=[
        'http' =>[
            'method'=>'POST',
            'header'=>"apikey:$SUPABASE_KEY\r\nAuthorization:Bearer $SUPABASE_KEY\r\nContent-Type:application/json\r\n",
            'content'=>json_encode($data),
        ],
    ];
    $context=stream_context_create($options);
    $result=file_get_contents("$SUPABASE_URL/rest/v1/customers",false,$context);

    if($result===FALSE){
        echo "error inserting ";
    }
    else{
    header("Location:index.html"); 
    }
   
}
?>