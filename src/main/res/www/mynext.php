<?

include "mydb_id.php";
ini_set('max_execution_time', 3);
set_time_limit(3);
//exec('/var/www/speed/kill.sh', $oute);


$rePrescribed = '0';
//$lat =     ((!empty($_GET['lat'])) ?   $_GET['lat'] :  $_POST['lat'] );
//$lon =     ((!empty($_GET['lon'])) ?   $_GET['lon'] :  $_POST['lon'] );
//$ber =     ((!empty($_GET['ber'])) ?   $_GET['ber'] :  $_POST['ber'] );
$debug =     ((!empty($_GET['debug'])) ?   $_GET['debug'] :  $_POST['debug'] );
$mroad =     ((!empty($_GET['reMainRoad'])) ?   $_GET['reMainRoad'] :  $_POST['reMainRoad'] )+0;	// the +0 is important to make this a number

$RdNo =    ((!empty($_GET['RdNo'])) ?   $_GET['RdNo'] :  $_POST['RdNo'] );
$reSpeedLimit =    ((!empty($_GET['reSpeedLimit'])) ?   $_GET['reSpeedLimit'] :  $_POST['reSpeedLimit']);
$re =    ((!empty($_GET['RE'])) ?   $_GET['RE'] :  $_POST['RE'] );


$rePrescribed =   ((!empty($_GET['rePrescribed'])) ?   $_GET['rePrescribed'] :  $_POST['rePrescribed'] )+0;	// the +0 is important to make this a number
$rows[] = "";

//$rePrescribed 	=   ((!empty($_GET['rePrescribed'])) ?  substr($_GET['rePrescribed'],-1) :  substr($_POST['rePrescribed'],-1) );
//$mroad 		=   ((!empty($_GET['reMainRoad'])) ?    substr($_GET['reMainRoad'],-1) :    substr($_POST['reMainRoad'],-1) );


if ($rePrescribed == 0) {
	$order = 'DESC';
	$lg = '<';
}
else{

	$lg = '>';
} 



$sql = "SELECT * FROM `$databasetable`
		WHERE  `RE` $lg '$re'  and `RdNo` = '$RdNo' and `reMainRoad` = ($mroad) and `rePrescribed` = ($rePrescribed) and `reSpeedLimit` <> '$reSpeedLimit' 
	 ORDER BY `RE` $order  Limit 1,1;";



//print $sql;

$sth = mysql_query($sql); 



if (mysql_errno()) {
 	header("HTTP/1.1 500 Internal Server Error");
    	if($debug = "1") 
	{
		echo $query.'<br><br>';
    		echo mysql_error(); 
    		print "<br><br><br>".$sql;
	}
	
}
else {
    $rows = array();
    while($r = mysql_fetch_assoc($sth)) {
        $rows[] = $r;
    }   
}


/*


			if ($rows[0]."" == ""){

				include "mydb_id.php";

				$sql = "SELECT * FROM `$databasetable` 
						WHERE `RE` $lg '$re'  and `RdNo` = '$RdNo' and `reMainRoad` = ($mroad) and `rePrescribed` = ($rePrescribed) and `reSpeedLimit` = '$reSpeedLimit'
					  ORDER BY 1 $order Limit 1,1;";


				$sth = mysql_query($sql); 



				if (mysql_errno()) {
				    header("HTTP/1.1 500 Internal Server Error");
				    echo $query.'<br>2<br>';
				    echo mysql_error(); 
				    print "<br><br>2<br>".$sql;

				}
				else {
				    $rows = array();
				    while($r = mysql_fetch_assoc($sth)) {
					$rows[] = $r;
				    }	   
				}
			}

*/


    print json_encode($rows[0]);
    print "";

mysql_close($con);



?>
