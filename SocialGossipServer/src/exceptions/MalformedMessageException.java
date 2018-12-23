package exceptions;

/**
 * Eccezione che viene lanciata se un messaggio e' malformato, quindi se ha dei 
 * campi non coerenti con il messaggio in generale.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class MalformedMessageException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public MalformedMessageException() {
		super();
	}
	
	public MalformedMessageException(String s) {
		super(s);
	}

}
