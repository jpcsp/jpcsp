<?php

require_once(__DIR__ . '/HandleHleModules.php');

class HandleHleModules_RemoveFunctionBody extends HandleHleModules {
	public function handleFile($rfile) {
		$data = file_get_contents($rfile);
		
		$errorCount = 0;

		$str = ('#(:TAB_AND_SPACE:)@HLEFunction\\(nid = (\\w+), version = (\\d+)\\)
			public final HLEModuleFunction (\\w+) = new HLEModuleFunction\\("(\\w+)", "(\\w+)"\\) \\{

				@Override
				public final void execute\\(Processor processor\\) \\{
					\\w+\\(processor\\);
				\\}

				@Override
				public final String compiledString\\(\\) \\{
					return "[\\w\\.]+\\(processor\\);";
				\\}
			\\};#msi');
		
		$defaultModuleName = null;
		
		if (preg_match('@class (\\w+)@msi', $data, $matches)) {
			$defaultModuleName = $matches[1];
		}
		
		$str = str_replace(':TAB_AND_SPACE:', '[\\t ]*', $str);
		
		$regex = preg_replace('@\\s+@', '\\s+', $str);
		
		//echo "$regex\n";
		
		$data = preg_replace_callback($regex, function($matches) use ($defaultModuleName) {
			list(,$indent,$nid, $version, $javaField, $moduleName, $functionName) = $matches;
			
			echo "'{$indent}'\n";

			$defaultFunctionName = substr($javaField, 0, -8);
			
			$r = '';
			$r .= $indent;
			$r .= '@HLEFunction(';
			$r .= 'nid = ' . $nid;
			$r .= ', version = ' . $version;
			if ($defaultModuleName != $moduleName) {
				$r .= ', moduleName = "' . $moduleName . '"';
			}
			if ($defaultFunctionName != $functionName) {
				$r .= ', functionName = "' . $functionName . '"';
			}
			$r .= ')';
			$r .= ' public HLEModuleFunction ' . $javaField . ';';
			$r .= "\n";
			echo "::: '$r'\n";
			return $r;
		}, $data);
		
		if ($errorCount > 0) {
			echo "Had errors. No updating!";
		} else {
			file_put_contents($rfile, $data);
		}
		
		//exit;
		
		//echo "$rfile\n";
		//echo $str;
		
		//exit;
	}

}

$handleHleModules = new HandleHleModules_RemoveFunctionBody();
$handleHleModules->handleModuleFolders();
