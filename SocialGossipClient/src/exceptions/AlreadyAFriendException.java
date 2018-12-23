package exceptions;

/** 
 * Eccezione che viene lanciata se un utente fa richiesta di friendship verso
 * un utente di cui e' gia' amico
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class AlreadyAFriendException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AlreadyAFriendException() {
		super();
	}
	
	public AlreadyAFriendException(String s) {
		super(s);
	}

}
