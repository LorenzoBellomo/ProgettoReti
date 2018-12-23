package exceptions;

/**
 * Eccezione checked per gestire richiesta di tipo get in una classe, quando 
 * il campo richiesto non è significativo per un determinato tipo.
 * (Ad esempio se faccio una richiesta del tipo getLanguage ad un messaggio 
 * di richiesta di tipo msg2friend, il campo language non è significativo)
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class NotAFieldException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public NotAFieldException() {
		super();
	}
	
	public NotAFieldException(String s) {
		super(s);
	}

}
