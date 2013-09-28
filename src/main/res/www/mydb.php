<?


///http://anthonykeane.com/speed/mydb.php?When=2013-09-15+11%3A20%3A31&UUID=test-a003bc68-0e3a-4faa-9411-288e5d578f26&ber=100&bZoneError=0&lon=151.184823200&speed=99&lat=-33.8136518800 


include "mydb_id.php";



 function oneTo1($x) {
	if(ord($x) == 1){
	   return "1";
	}
	else {
	    return "0";
	}
    }

//echo exec('/var/www/speed/kill.sh');


$lat =    trim ((!empty($_GET['lat'])) ?   $_GET['lat'] :  $_POST['lat'] );
$lon =    trim ((!empty($_GET['lon'])) ?   $_GET['lon'] :  $_POST['lon'] );
$ber =    trim ((!empty($_GET['ber'])) ?   $_GET['ber'] :  $_POST['ber'] );
$uuid =    trim ((!empty($_GET['UUID'])) ?   $_GET['UUID'] :  $_POST['UUID'] );
$speed =    trim ((!empty($_GET['speed'])) ?   $_GET['speed'] :  $_POST['speed'] );
$when =    trim ((!empty($_GET['When'])) ?   $_GET['When'] :  $_POST['When'] );
$zoneError= ((!empty($_GET['bZoneError'])) ?   ($_GET['bZoneError']) :  ($_POST['bZoneError']) )+0;
$prescribed = 0;
//print  "lat: $lat; lon: $lon;<br />\n";
$offset = 0.0002;
$offsetB = 7;

$sqlsub1 = "(select * from $databasetable where (reLat between ". ($lat - $offset) ." and ". ($lat + $offset) .") and (reLon between ". ($lon - $offset) ." and ". ($lon + $offset) .") ) as a ";
//print $sqlsub1;



$sql = "select * from " . $sqlsub1 . " where (reBearing  between ". ($ber - $offsetB) . " and  " . ($ber + $offsetB)  ." )  limit 1,20 ";



if ($ber>(360-$offsetB))
{
$sql = "select * from $sqlsub1
   where (reBearing  between ". ($ber - $offsetB) ." and 360)
   or    (reBearing  between 0 and ". ($ber - (360-$offsetB)) .") limit 1,6;  ";
}

if ($ber<$offsetB)
{
$sql = "select * from $sqlsub1 
   where (reBearing  between ". ($ber+(360-$offsetB)) ." and 360)
   or    (reBearing  between 0 and ". ($ber +$offsetB) .") limit 1,6;";
}

$ClosestDistancetoPOI = 100.0; // any BIG number

$sth = mysql_query($sql);
if (mysql_errno()) {
    header("HTTP/1.1 500 Server Select Cockup");
    echo $query.'<br>';
    echo mysql_error().'<br><br>';
    print $sql;
}
else {
    
    while($r = mysql_fetch_assoc($sth)) {
            
       $re = $r['RE'];
       $bernew =  $r['reBearing']+"";
       $prescribed = oneTo1($r['rePrescribed']);
        
       $squis = (($lat-$r["reLat"])*($lat-$r["reLat"]) +($lon-$r["reLon"])*($lon-$r["reLon"]));
          //echo json_encode($r) . "   ". $squis ."<BR>" ;
                     
       if ($ClosestDistancetoPOI >= $squis)
       {
        //echo $ClosestDistancetoPOI ."<BR>"  ;
        $ClosestDistancetoPOI = $squis;
        $closestRecord = $r;

       }
    }

      echo json_encode($closestRecord);
}

//if ($bernew."" != "")

	$sql = "INSERT INTO `tblGPSlog`(`UUID`, `When`, `RE`, `reLat`, `reLon`, `reBearing`, `reSpeed`, `zoneError`,`rePrescribed`) VALUES ('$uuid','$when','$re',$lat,$lon,$ber,$speed,($zoneError),($prescribed)    );";
	//print $sql;

  $sth = mysql_query($sql);

//	if (mysql_errno()) {
//	    header("HTTP/1.1 500 Server Insert Cockup");
//	    echo $query.'<br>';
//	    echo mysql_error().'<br><br>';
//	    print $sql;
//	}






?>
