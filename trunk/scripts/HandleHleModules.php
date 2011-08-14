<?php

class HandleHleModules {
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
	}
	
	public function handleModuleFolders() {
		foreach (scandir($path = __DIR__ . '/../src/jpcsp/HLE') as $file) {
			$rfile = "{$path}/{$file}";
			if (substr($file, 0, 7) == 'modules' && strlen($file) > 7) {
				$this->handleDir($rfile);
			}
		}
	}
}
