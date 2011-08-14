<?php

require_once(__DIR__ . '/HandleHleModules.php');

class HandleHleModules_ChangeNIDSFromFieldToMethod extends HandleHleModules {
	public function handleFile($rfile) {
		$data = file_get_contents($rfile);
		
		if (!preg_match('@public\\s+HLEModuleFunction@', $data)) return;

		echo "$rfile\n";
		
		$errorCount = 0;
		
		$methodsToReplace = array();
		
		$data = preg_replace_callback('#\\s+(@HLEFunction\\([^\\)]*\\))\\s+public\\s+HLEModuleFunction\\s+(\\w+);#msi', function($matches) use (&$methodsToReplace) {
			list(,$annotation, $fieldName) = $matches;
			if (substr($fieldName, -8) != 'Function') throw(new Exception("Invalid method name '{$fieldName}'"));
			$methodName = substr($fieldName, 0, -8);
			$methodsToReplace[$methodName] = array(
				'name'       => $methodName,
				'annotation' => $annotation,
			);
		}, $data);
		
		$data = preg_replace_callback('#([\\t ]*)public\\s+void\\s+(\\w+)\\(Processor\\s+processor\\)#msi', function($matches) use (&$methodsToReplace) {
			list($all, $indent, $methodName) = $matches;
			
			$method = &$methodsToReplace[$methodName];
			//print_r($method);
			//echo "{$indent}{$method['annotation']}\n{$all}";
			if (isset($method)) {
				return "{$indent}{$method['annotation']}\n{$all}";
			} else {
				echo "'$methodName' not found!";
				return $all;
			}
		}, $data);
		
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

$handleHleModules = new HandleHleModules_ChangeNIDSFromFieldToMethod();
$handleHleModules->handleModuleFolders();
