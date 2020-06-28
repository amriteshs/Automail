package exceptions;

/**
 * An exception throws when robot wrongly handles normal item (i.e., loads it onto special arms)
 */
public class NormalItemException extends Exception {
	public NormalItemException() {
		super("Normal Item!!");
	}
}
