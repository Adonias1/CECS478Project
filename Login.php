<?php
use \Firebase\JWT\JWT; 
//require_once 'Connect.php';
$servername = "localhost";
$myDBusername = "root";
$myDBpassword = "Kickme531";
	
try {
    $conn = mysqli_connect($servername, $myDBusername, $myDBpassword, "nexusmessenger");
	if (mysqli_connect_errno()) {
		printf("Connect failed: %s\n", mysqli_connect_error());
		exit();
	}
	else{
		echo "Connected successfully <br/>";
	}
     
    }
catch(Exception $e)
    {
    echo "Connection failed: " . $e->getMessage();
    }
require_once 'JWT.php';
define('SECRET_KEY','nexusmessenger');
define('ALGORITHM','HS256');   
  
                // if there is no error below code run
global $username, $password;
$username = $_POST['username'];
$password = $_POST['password'];
$statement = mysqli_prepare($conn, "select u_id, username, password from users where username = ?" );
mysqli_stmt_bind_param($statement, "s", $username);
mysqli_stmt_execute($statement);
mysqli_stmt_bind_result($statement, $id, $username, $passwordHash);
$row = mysqli_stmt_fetch($statement);
if(count($row) > 0 && password_verify($password,$passwordHash)){
	$tokenId    = base64_encode(mcrypt_create_iv(32));
	$issuedAt   = time();
	$notBefore  = $issuedAt + 10;  //Adding 10 seconds
	$expire     = $notBefore + 7200; // Adding 60 seconds
	$serverName = 'localhost'; /// set your domain name 
	/*
	* Create the token as an array
	*/
	$data = [
		'iat'  => $issuedAt,         // Issued at: time when the token was generated
		'jti'  => $tokenId,          // Json Token Id: an unique identifier for the token
		'iss'  => $serverName,       // Issuer
		'nbf'  => $notBefore,        // Not before
		'exp'  => $expire,           // Expire
		'data' => [                  // Data related to the logged user you can set your required data
		// id from the users table
			'id' => $id,
			'name' => $username, //  name
		]	
    ];
	$secretKey = "nexusmessenger";
	/// Here we will transform this array into JWT:
	$jwt = JWT::encode(
	$data, //Data to be encoded in the JWT
	$secretKey, // The signing key
	ALGORITHM 
	); 
	$unencodedArray = ['jwt' => $jwt];
	echo  "{\"status\" : \"success\",\"resp\":".json_encode($unencodedArray)."}";
} else {
	echo  "{\"status\" : \"error\",\"msg\":\"Invalid email or password\"'}";
}
    
?>     