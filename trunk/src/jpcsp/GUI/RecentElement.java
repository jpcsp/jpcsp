package jpcsp.GUI;

public class RecentElement {
	public String path, title;
	
	public RecentElement(String path, String title) {
		this.path = path;
		this.title = title;
	}
	
	@Override
	public String toString() {
		return (title == null ? "" : title) + " - " + path;
	}
}
