package jpcsp.autotests;

final public class AutoTestsOutput {
	protected static StringBuilder output = new StringBuilder();
	
	static public void clearOutput() {
		output = new StringBuilder();
	}
	
	static public String getOutput() {
		return output.toString();
	}
	
	static public void appendString(String text) {
		output.append(text);
	}
}
