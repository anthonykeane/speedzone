<?

include "mydb_id.php";
$databasetable = "poi2";

$lat =    trim ((!empty($_GET['lat'])) ?   $_GET['lat'] :  $_POST['lat'] );
$lon =    trim ((!empty($_GET['lon'])) ?   $_GET['lon'] :  $_POST['lon'] );
//$ber =    trim ((!empty($_GET['ber'])) ?   $_GET['ber'] :  $_POST['ber'] );
//$uuid =    trim ((!empty($_GET['UUID'])) ?   $_GET['UUID'] :  $_POST['UUID'] );
//$speed =    trim ((!empty($_GET['speed'])) ?   $_GET['speed'] :  $_POST['speed'] );
//$when =    trim ((!empty($_GET['When'])) ?   $_GET['When'] :  $_POST['When'] );
//$zoneError= trim ((!empty($_GET['bZoneError'])) ?   $_GET['bZoneError'] :  $_POST['bZoneError'] );

//print  "lat: $lat; lon: $lon;<br />\n";
$offset = 0.2;


$sql = "select poiLat, poiLon, poiType, poiWhen from $databasetable where (poiLat between ". ($lat - $offset) ." and ". ($lat + $offset) .") and (poiLon between ". ($lon - $offset) ." and ". ($lon + $offset) .")  ";


$ClosestDistancetoPOI = 10.0;

$sth = mysql_query($sql);
if (mysql_errno()) {
    header("HTTP/1.1 500 Internal Server Error");
    echo $query.'<br>';
    echo mysql_error().'<br><br>';
    print $sql;
}
else {
    //$rows = array();

    while($r = mysql_fetch_assoc($sth)) {
        $squis = (($lat-$r["poiLat"])*($lat-$r["poiLat"]) +($lon-$r["poiLon"])*($lon-$r["poiLon"]));
        //echo json_encode($r) . ", ". $squis. "<br>";
               
     if ($ClosestDistancetoPOI >= $squis)
     {

     	$ClosestDistancetoPOI = $squis;
     	$closestRecord = $r;
        //echo $ClosestDistancetoPOI."<BR>";
     }
   	}

    echo json_encode($closestRecord);
}





?>

