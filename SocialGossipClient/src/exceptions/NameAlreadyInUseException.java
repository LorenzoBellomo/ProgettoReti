package exceptions;

/**
 * Eccezione che viene lanciata lato client quando viene fatta 
 * una richiesta di tipo di tipo registrazione utente/chatroom
 * con un nome però occupato
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NameAlreadyInUseException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NameAlreadyInUseException() {
		super();
	}
	
	public NameAlreadyInUseException(String s) {
		super(s);
	}
}
