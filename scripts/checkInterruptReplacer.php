<?php

function fixInsideInterruptString($subject) {
	$pattern = '#@HLEFunction\\(nid = .*, version = .*()\\).*(if \\(IntrManager\\.getInstance\\(\\)\\.isInsideInterrupt\\(\\)\\) \\{.*\\})#Umsi';

	$result = preg_replace_callback($pattern, function($matches) {
		$result = $matches[0];
		{
			$result = preg_replace('#(if \\(IntrManager\\.getInstance\\(\\)\\.isInsideInterrupt\\(\\)\\) \\{.*\\})#Umsi', '', $result);
			$result = preg_replace('#(@HLEFunction\\(nid = .*, version = .*)(\\))#Umsi', '$1, checkInsideInterrupt = true$2', $result);
			
		}
		//echo $result;
		return $result;
	}, $subject);
	
	//var_dump($result);
	if (is_string($result)) {
		return $result;
	} else {
		return $subject;
	}
}

function fixInsideInterruptFile($file) {
	$original = file_get_contents($file);
	$replaced = fixInsideInterruptString($original);
	//var_dump($replaced);
	//exit;
	if ($original != $replaced) {
		if ($replaced != '') {
			file_put_contents($file, $replaced);
		} else {
			echo "Warning!\n";
		}
	}
}

function fixInsideInterruptDirectoryRecursively($path) {
	foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($path), RecursiveIteratorIterator::CHILD_FIRST) as $file) {
		if (substr($file, -5) != '.java') continue;
		fixInsideInterruptFile($file);
	}
}

fixInsideInterruptDirectoryRecursively(__DIR__ . '/../src');
