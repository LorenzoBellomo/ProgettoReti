package exceptions;

/** 
 * Eccezione che viene lanciata se un utente fa richiesta di aggiunta ad una
 * chatroom in cui e' gia' stato precedentemente inserito
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class AlreadyInChatroomException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AlreadyInChatroomException() {
		super();
	}
	
	public AlreadyInChatroomException(String s) {
		super(s);
	}
	
}
