package jpcsp.HLE;

public @interface ISerializeString {
	public int size();
	public byte padWith() default '\0'; 
	public String charset() default "UTF-8";
}
