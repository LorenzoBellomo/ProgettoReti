package exceptions;

/**
 * Eccezione che viene lanciata lato client quando viene fatta 
 * una richiesta che riguarda un user non esistente
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class UnknownUserException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public UnknownUserException() {
		super();
	}
	
	public UnknownUserException(String s) {
		super(s);
	}

}
