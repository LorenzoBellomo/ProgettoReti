package exceptions;

/**
 * Eccezione che viene lanciata quando un codice numerico di richiesta o 
 * di risposta richiesto non esiste.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NoSuchCodeException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NoSuchCodeException() {
		super();
	}
	
	public NoSuchCodeException(String s) {
		super(s);
	}

}
