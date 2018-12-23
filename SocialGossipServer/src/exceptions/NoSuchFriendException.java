package exceptions;

/**
 * Eccezione che viene lanciata lato client quando viene fatta 
 * una richiesta di tipo di invio messaggio/file verso un client che non
 * e' nella mia lista amici
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NoSuchFriendException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public NoSuchFriendException() {
		super();
	}
	
	public NoSuchFriendException(String s) {
		super(s);
	}

}
