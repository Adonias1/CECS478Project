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
		//echo "Connected successfully <br/>";
	}
}
catch(Exception $e){
  echo "Connection failed: " . $e->getMessage();
}
require_once 'JWT.php';
define('SECRET_KEY','nexusmessenger');
define('ALGORITHM','HS256');

// if there is no error below code run
global $token, $obj, $obj_array, $id;
//$token = $_GET["token"];
$httpheaders = apache_request_headers();
$jwt = $httpheaders['Authorization'];
$token = str_replace('Bearer ', '', $jwt);
$tokenPost = $_POST['token'];
if(isset($token) && $token != NULL){
	$keyToken = "nexusmessenger";
	JWT::$leeway = 20;
	try{
		$obj = JWT::decode($token, $keyToken, array('HS256'));
		$obj_array = (array)$obj->{'data'};
		$id =  $obj_array['id'];
	} catch(Exception $e){
		echo "Decoding failure <br/>";
	}
	$statement = mysqli_prepare($conn, "SELECT conversation.c_id, sender, toReceiver, message, timestamp FROM messages NATURAL JOIN conversation where toReceiver = ?" );
	mysqli_stmt_bind_param($statement, "s", $id);
	mysqli_stmt_execute($statement);
	mysqli_stmt_bind_result($statement, $c_id, $sender, $id, $message, $timestamp);
	//fetch messages
	while(mysqli_stmt_fetch($statement)){
    $data = [
  		'ConvoID'  => $c_id,         // Issued at: time when the token was generated
  		'From'  => $sender,          // Json Token Id: an unique identifier for the token
  		'Message'  => $message,       // Issuer
  		'timestamp'  => $timestamp,        // Not before
      ];
    echo json_encode($data);
	}
	mysqli_stmt_close($statement);
}
else if(isset($tokenPost) && $tokenPost != NULL){
	$sendTo = $_POST['to'];
	$sendMessage = $_POST['message'];
	$keyToken = "nexusmessenger";
	if(isset($tokenPost)){
		try{
			$obj = JWT::decode($tokenPost, $keyToken, array('HS256'));
			$obj_array = (array)$obj->{'data'};
			$id =  $obj_array['id'];
		} catch(Exception $e){
			echo "Decoding failure <br/>";
		}
	}
	if(isset($sendTo) && isset($sendMessage) && isset($id)){
		date_default_timezone_set('America/Los_Angeles');
		$date = date('Y-m-d H:i:s', time());
		try{
			$query = "SELECT m_id FROM messages order by m_id desc";
			$result = mysqli_query($conn, $query);
			$row = mysqli_fetch_row($result);
			if($sendStmt = mysqli_prepare($conn, "INSERT INTO messages(sender, toReceiver, message, timestamp) VALUES (?, ?, ?, ?)")){
				echo "SQL statement prepared<br/>";
			}
			else{
				echo "SQL statement unprepared<br/>";
			}
			if(mysqli_stmt_bind_param($sendStmt, "iiss", $id, $sendTo, $sendMessage, $date)){
				echo "Bind successful<br/>";
			}
			else{
				echo "Bind unsuccessful<br/>";
			}
			if(mysqli_stmt_execute($sendStmt)){
				echo "Message Sent<br/>";
			}
			else{
				echo "Message couldn't be sent<br/>";
			}
			mysqli_stmt_close($sendStmt);
			$query = "SELECT m_id FROM messages order by m_id desc";
			$result = mysqli_query($conn, $query);
			$row = mysqli_fetch_row($result);
			if(!($convStmt = mysqli_prepare($conn, "INSERT INTO conversation(m_id, u_id) VALUES (?,?)"))){
				echo "Prepare didn't work <br/>";
			}
			if(!mysqli_stmt_bind_param($convStmt, "ii", $row[0], $sendTo)){
				echo "bind didn't work <br/>";
			}
			if(!mysqli_stmt_execute($convStmt)){
				echo "execute didn't work <br/>";
			}
			mysqli_stmt_close($convStmt);
		} catch (Exception $e){
			echo "Message could not be sent at this time<br/>";
		}
	}
	else{
		echo "no message to send<br/>";
	}
}
else{
	echo "Token was not set<br/>";
}
mysqli_close($conn);

?>
