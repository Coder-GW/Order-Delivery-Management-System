<?php 

$SUPABASE_URL="https://bmqvkxfvljxlgynxruga.supabase.co";
$SUPABASE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJtcXZreGZ2bGp4bGd5bnhydWdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQyOTQ5ODQsImV4cCI6MjA3OTg3MDk4NH0.qBJNBP7Xger1b6E__yfE93ZaqP7Hp1a0RuJYmEk9_4k";

if ($_SERVER['REQUEST_METHOD']==='POST'){
    $username=htmlspecialchars(trim($_POST['Username']?? ''));
    $password=htmlspecialchars(trim($_POST['Password']?? ''));
    //  getting the username and password form the html form
    // sanatizing data making sure noone can eject malicious data into the input box
    $ctable="customers";


    $url= "$SUPABASE_URL/rest/v1/$ctable?username=eq.$username&select=*";
    // searches the table for a mtching row with the username 


    
    $head=[
        "apikey:$SUPABASE_KEY",
        "Authorization:Bearer $SUPABASE_KEY",
        "Content-Type:application/json\r\n"
            
    ];

    $c=curl_init($url);
    curl_setopt($c,CURLOPT_HTTPHEADER,$head);
    curl_setopt($c,CURLOPT_RETURNTRANSFER,true);
    // returning the matching row 
    $response=curl_exec($c);
    curl_close($c);

    $data=json_decode($response,true);

    if(empty($data)){
        // if theres no such username 
        echo"Re-enter either username or password";
        exit;
    }
    $user=$data[0];

    if ($password===$user['password']){
        //open acc info page 
    
        session_start();

        $_SESSION['user_id']=$user['id'];
        $_SESSION['first_name']=$user['first_name'];
        $_SESSION['last_name']=$user['last_name'];
        $_SESSION['email']=$user['email'];
        $_SESSION['phone_number']=$user['phone_number'];
        $_SESSION['street']=$user['street'];
        $_SESSION['city']=$user['city'];
        $_SESSION['parish']=$user['parish'];


        header("Location:acc_info.php");
        exit();
     // sending user info to account info page 
        
    }
    else{
        echo"Re-enter either username or password";
        // if password is wrong 
        exit;
    }

   
}
?>