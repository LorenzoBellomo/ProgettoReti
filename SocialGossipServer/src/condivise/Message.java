package condivise;

/**
 * Classe che definisce le informazioni base contenute in un messaggio - Sender:
 * Mittente del messaggio - Receiver: Destinatario del messaggio - Tipo: Tipo
 * del messaggio, puo' essere di richiesta, di risposta o di testo
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class Message {

	public static final int REQUEST = 0;
	public static final int RESPONSE = 1;
	public static final int TEXT = 2;
	public static final String SERVERNAME = "Server"; // per il nome del server

	// nickname dell'utente che invia il messaggio o 'Server' eventualmente
	private String sender;

	// nickname del ricevente (eventualmente 'server')
	private String receiver;

	// tipo del messaggio
	private int msgType;

	protected Message(String sender, String receiver, int msgType) {
		// COSTRUTTORE PROTECTED, usato dalle sottoclassi per settare
		// sender e receiver
		this.msgType = msgType;
		this.sender = sender;
		this.receiver = receiver;
	}

	/**
	 * Restituisce il nickname del sender
	 * 
	 * @return il nickname del sender
	 */
	public String getSender() {
		return this.sender;
	}

	/**
	 * Restituisce il nickname del receiver
	 * 
	 * @return il nickname del receiver
	 */
	public String getReceiver() {
		return this.receiver;
	}

	/**
	 * Restituisce il tipo del messaggio definito dalle costanti definite in
	 * questa classe (REQUEST, RESPONSE E TEXT)
	 * 
	 * @return l'intero corrispondente al tipo di messaggio
	 */
	public int getType() {
		return this.msgType;
	}
}
