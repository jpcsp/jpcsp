<?php

class UpdateNids {
	public function __construct() {
	}

	public function handleDir($path) {
		$path = realpath($path);
		foreach (scandir($path) as $file) {
			if ($file[0] == '.') continue;
			$rfile = realpath("{$path}/{$file}");
			//echo "$rfile\n";
			if (is_dir($rfile)) {
				$this->handleDir($rfile);
			} else {
				$this->handleFile($rfile);
			}
		}
	}
	
	public function handleFile($rfile) {
		$data = file_get_contents($rfile);
		if (strpos($data, 'void installModule(HLEModuleManager mm, int version) {') !== false) {
			if (strpos($data, 'installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }') !== false) {
				//echo "Already replaced!\n";
				return;
			}

			echo "$rfile\n";

			$tokens = array_map(function($v) { if (is_array($v)) return $v[1]; return $v; }, token_get_all('<?php ' . $data));
			$tokens_count = count($tokens);
			
			$functions = array();
			$errorCount = 0;
			for ($n = 0; $n < $tokens_count; $n++) {
				$checkToken = $tokens[$n];
				if ($checkToken == 'installModule' || $checkToken == 'uninstallModule') {
					$m = $n;
					$scope = 0;
					for (; $n < $tokens_count; $n++) {
						//echo "{$tokens[$n]}:$scope\n";
						if ($tokens[$n] == '{') $scope++;
						if ($tokens[$n] == '}') {
							$scope--;
							if ($scope <= 0) break;
						}
					}
					if ($n < $tokens_count) {
						$code = implode('', array_slice($tokens, $m, $n - $m + 1));
						$version = 0;
						
						//echo $code;
						
						if (preg_match('@version\\s*>=\\s*(\\d+)@msi', $code, $matches)) {
							$version = (int)$matches[1];
						}
						
						if (preg_match_all('@mm.addFunction\\((\\w+), (\\w+)\\);@Umsi', $code, $matches, PREG_SET_ORDER)) {
							foreach ($matches as $match) {
								list(, $nid, $name) = $match;
							
								if (substr($name, 0, 2) == '0x') {
									list($nid, $name) = array($name, $nid);
								}
							
								$functions[$name] = array(
									'nid'     => $nid,
									'name'    => $name,
									'version' => $version,
									'syscall' => false,
								);
							}
							//print_r($matches);
						}

						if (preg_match_all('@mm.addHLEFunction\\((\\w+)\\);@Umsi', $code, $matches, PREG_SET_ORDER)) {
							foreach ($matches as $match) {
								list(, $name) = $match;
							
								$functions[$name] = array(
									'nid'     => 0,
									'name'    => $name,
									'version' => $version,
									'syscall' => true,
								);
							}
							//print_r($matches);
						}
						
						if ($checkToken == 'installModule') {
							$data = str_replace($code, 'installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }', $data);
						} else {
							$data = str_replace($code, 'uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }', $data);
						}
						
						//echo $data;
						//exit;
						// public final HLEModuleFunction sceKernelCreateLwMutexFunction
					}
				}
			}
			
			//$errorCount++;
			if (preg_match_all('@import\\s+[\\w\\.]+;@', $data, $matches)) {
				if (!in_array('import jpcsp.HLE.HLEFunction;', $matches[0])) {
					$data = preg_replace('@import\\s+[\\w\\.]+;@', "import jpcsp.HLE.HLEFunction;\n\\0", $data, 1);
					//echo $data;
					//exit;
				}
				//print_r($matches);
			}
			//import jpcsp.HLE.HLEFunction;
			
			//print_r($functions);
			
			$data = preg_replace_callback("@[ \t]*\n([ \t]*)(public final HLEModuleFunction (\\w+))@msi", function($matches) use (&$functions, &$errorCount) {
				//print_r($functions);
				$indent   = $matches[1];
				$text     = $matches[2];
				$function = &$functions[$matches[3]];
				if (!isset($function)) {
					echo "Not NID for '{$matches[3]}'\n";
					$errorCount++;
				
					// Untouch
					return $matches[0];
				}
				
				$annotation  = '';
				$annotation .= "@HLEFunction(";
				$annotation .= "nid = {$function['nid']}";
				$annotation .= ", version = {$function['version']}";
				if ($function['syscall']) {
					$annotation .= ", syscall = 1";
				}
				$annotation .= ")";
				
				$ret = "{$indent}{$annotation}\n{$indent}{$text}";
				unset($functions[$matches[3]]);
				return $ret;
			}, $data);
			
			if (!empty($functions)) {
				echo "Not replaced functions:\n";
				$errorCount++;
				print_r(array_keys($functions));
			}
			
			if ($errorCount) {
				echo "Had errors. Not writting!\n";
			} else {
				file_put_contents($rfile, $data);
			}
			
			//echo $data;
			//exit;
			
			//print_r($tokens);
			
			//preg_match_all();
			//'mm.addFunction(0x19CFF145, sceKernelCreateLwMutexFunction);';
		}
		
	}
}

$updateNids = new UpdateNids();
foreach (scandir($path = __DIR__ . '/../src/jpcsp/HLE') as $file) {
	$rfile = "{$path}/{$file}";
	if (substr($file, 0, 7) == 'modules' && strlen($file) > 7) {
		$updateNids->handleDir($rfile);
	}
}
//$updateNids->handleDir(__DIR__ . '/../src/jpcsp/HLE');
//$updateNids->handleFile(__DIR__ . '/../src/jpcsp/HLE/modules150/sceUsb.java');