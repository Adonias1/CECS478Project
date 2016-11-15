<?php
    
    $servername = "localhost";
	$myDBusername = "root";
	$myDBpassword = "Kickme531";

try {
    $conn = mysqli_connect($servername, $myDBusername, $myDBpassword, "nexusmessenger");
	if (mysqli_connect_errno()) {
		echo "Connection failed<br/>";
	}
	else{
		echo "Connected successfully<br/>";
	}	 
} catch(Exception $e){
    echo "Connection failed: " . $e->getMessage();
}
	
function registerUser() {
	global $conn, $username, $password;
	$username = $_POST['username'];
	$password = $_POST['password'];
	$passwordHash = password_hash($password, PASSWORD_BCRYPT);
	try{
        $statement = mysqli_prepare($conn, "INSERT INTO users (username, password) VALUES (?, ?)");
        mysqli_stmt_bind_param($statement, "ss", $username, $passwordHash);
        mysqli_stmt_execute($statement);
		echo mysqli_stmt_error($statement);
        mysqli_stmt_close($statement);
	}catch(Exception $e){
		echo "could not run statement<br/>";
	}
		
}
function usernameAvailable() {
	global $conn, $username;
	$username = $_POST['username'];
    $statement = mysqli_prepare($conn, "SELECT username FROM users WHERE username = ?");
	mysqli_stmt_bind_param($statement, "s", $username);
	mysqli_stmt_execute($statement);
	mysqli_stmt_bind_result($statement, $username);
	$count = mysqli_stmt_fetch($statement);
	if (!(count($count) > 0)){
		return true;
    }else {
		return false;
    }
}
$response = array();
$response["success"] = false;
if (usernameAvailable()){
	registerUser();
	$response["success"] = true;
}
else{
	echo "username already exists<br/>";
}
echo json_encode($response);
?>
