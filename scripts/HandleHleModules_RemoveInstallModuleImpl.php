<?php

require_once(__DIR__ . '/HandleHleModules.php');

class HandleHleModules_RemoveInstallModuleImpl extends HandleHleModules {
	public function handleFile($rfile) {
		$data = file_get_contents($rfile);
		
		if (!preg_match('@public void installModule\\(HLEModuleManager mm, int version\\)@', $data)) return;
		
		$errorCount = 0;

		echo "$rfile\n";
		
		$regex = '# @Override
			public void (un)?installModule\\(HLEModuleManager mm, int version\\) \\{
				\\w+.\\w+\\(\\w+, version\\);
			\\}#msi'
		;
		
		$regex = preg_replace('@\\s+@', '\\s+', $regex);
		
		$data = preg_replace($regex, '', $data);
		
		if ($errorCount) {
			echo "Had errors. Not updating!";
		} else {
			file_put_contents($rfile, $data);
		}
		
		//echo $data;
		//print_r($methodsToReplace);
		//exit;
	}

}

$handleHleModules = new HandleHleModules_RemoveInstallModuleImpl();
$handleHleModules->handleModuleFolders(true);
