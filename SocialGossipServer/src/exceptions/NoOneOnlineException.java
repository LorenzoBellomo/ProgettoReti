package exceptions;

/**
 * Eccezione che viene lanciata se un utente vuole mandare un messaggio in
 * una chatroom in cui nessuno dei partecipanti eccetto il sender stesso
 * e' online
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NoOneOnlineException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public NoOneOnlineException() {
		super();
	}
	
	public NoOneOnlineException(String s) {
		super(s);
	}

}
