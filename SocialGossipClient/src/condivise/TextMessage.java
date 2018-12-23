package condivise;

/**
 * Classe che modella un messaggio testuale, il sender e il receiver sono quelli
 * del messaggio testuale. Come unica informazione aggiuntiva portano la stringa
 * testuale inviata
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class TextMessage extends Message {

	// il messaggio da inviare
	private String message;

	private TextMessage(String sender, String receiver, String message) {
		// COSTRUTTORE PRIVATO, per creare un messaggio testuale usare metodo
		// statico
		super(sender, receiver, Message.TEXT);
		this.message = message;
	}

	/**
	 * Metodo statico per creare un messaggio testuale
	 * 
	 * @param sender
	 *            utente che invia il messaggio
	 * @param receiver
	 *            utente/chatroom su cui inviare il messaggio
	 * @param message
	 *            il messaggio da creare
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static TextMessage BuildTextMessage(String sender, String receiver, String message) {
		if (sender == null || receiver == null || message == null)
			throw new NullPointerException();
		return new TextMessage(sender, receiver, message);
	}

	/**
	 * Restituisce il messaggio testuale
	 * 
	 * @return il messaggio di testo
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Setta la proprieta' messaggio, serve per inserire il nuovo messaggio
	 * tradotto
	 * 
	 * @throws NullPointerException
	 *             se il messaggio e' null
	 */
	public void setMessage(String translated) {
		if (translated == null)
			throw new NullPointerException();

		this.message = translated;
	}

}
