package exceptions;

/**
 * Eccezione che viene lanciata se un utente richiede di registrarsi
 * con una lingua non riconosciuta
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class IllegalLanguageException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public IllegalLanguageException() {
		super();
	}
	
	public IllegalLanguageException(String s) {
		super(s);
	}

}
