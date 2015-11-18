 <?php

  $heartbeat = file_get_contents('/home/ahern/realtime/dbheartbeat.txt');
  $mainbeat  = file_get_contents('/home/ahern/realtime/mainheartbeat.txt');
  $milliseconds = round(microtime(true) * 1000);

  $heartFloat = floatval($heartbeat);
  $mainFloat  = floatval($mainbeat);
  $nowFloat   = floatval($milliseconds);

  if ($nowFloat - $heartFloat > 1200000.0 || $nowFloat - $mainFloat > 1200000.0) {
        echo "<html><head><title>Monitor Test</title></head><body><p>Problem</p></body></html>";
  } else {
        echo "<html><head><title>Monitor Test</title></head><body><p>All is normal.</p></body></html>";
  }
 ?>
