package exceptions;

/**
 * Eccezione che viene lanciata lato client quando viene fatta 
 * una richiesta di tipo di invio messaggio verso una chatroom inesistente
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NoSuchChatException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public NoSuchChatException() {
		super();
	}
	
	public NoSuchChatException(String s) {
		super(s);
	}

}
