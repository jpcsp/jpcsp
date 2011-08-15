package jpcsp.HLE;

@SuppressWarnings("serial")
public class SceKernelErrorException extends RuntimeException {
	public int errorCode;
	
	public SceKernelErrorException(int errorCode) {
		this.errorCode = errorCode;
	}
}
