<?
$databasehost = "localhost";
$databasename = "speed"; 
$databaseusername ="xxxxxxxxx"; 
$databasepassword = "xxxxxxxxxxx";
$databasetable = "tblSpeedZone3";
$con = mysql_connect($databasehost,$databaseusername,$databasepassword) or die(mysql_error()); 
mysql_select_db($databasename) or die(mysql_error()); mysql_query("SET CHARACTER SET utf8"); 
?>
