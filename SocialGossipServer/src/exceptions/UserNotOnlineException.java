package exceptions;

/**
 * Eccezione che viene lanciata lato client quando viene fatta 
 * una richiesta di tipo di invio messaggio/file verso un client che non
 * e' connesso
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class UserNotOnlineException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public UserNotOnlineException() {
		super();
	}
	
	public UserNotOnlineException(String s) {
		super(s);
	}

}
