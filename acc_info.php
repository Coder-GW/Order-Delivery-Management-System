<?php
session_start();
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>F.C.H. ACCOUNT</title>
    <link rel="stylesheet" href="create_acc.css">
  
</head>
<body>
    <header>
        <div class="container">
            <h1> Account Information </h1>
            
        </div>
    </header>
    <main>
    
        <h2> Welcome <?php echo $_SESSION['first_name'];?>!</h2> 

        <p><strong>First Name:</strong> <?php echo $_SESSION['first_name'];?></p>
        <p><strong>Last Name:</strong> <?php echo $_SESSION['last_name'];?></p>
        <p><strong>Email:</strong> <?php echo $_SESSION['email'];?></p>
        <p><strong>Phone Number:</strong> <?php echo $_SESSION['phone_number'];?></p>
        <p><strong>Street:</strong> <?php echo $_SESSION['street'];?></p>
        <p><strong>City:</strong> <?php echo $_SESSION['city'];?></p>
        <p><strong>Parish:</strong> <?php echo $_SESSION['parish'];?></p>
    


        <div>
            <button class="btn" onclick="window.location.href='edit_acc.html'">Edit Account</button>
            <button class="btn" onclick="window.location.href='edit_acc.html'">Add Order</button>
            <button class="btn" onclick="window.location.href='uc5.html'">View Orders</button>


        </div>
        
        
       
    </main>
    <footer>
        <div class="container">
            <p1>"We treat you like Royalty"</p1>
        </div>
    </footer>
</body>
</html>