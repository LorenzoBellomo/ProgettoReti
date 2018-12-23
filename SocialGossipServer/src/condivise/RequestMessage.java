package condivise;

import exceptions.NotAFieldException;

/**
 * Classe che modella i messaggi di richiesta del client per il server. Un
 * messaggio di richiesta e' costituito da: - Codice di richiesta che identifica
 * il tipo di richiesta - Opzionale, messaggio di testo, se la richiesta vuole
 * inviare un messaggio - Opzionale, nickname se si richiede un interazione con
 * tale utente - Opzionale, lingua se ci si vuole registrare con una determinata
 * lingua - Opzionale, stub per registrarsi alle callback
 * 
 * In particolare ogni messaggio di Richiesta avra' receiver = Server, mentre
 * gli altri campi saranno variabili. Il costruttore di questa classe e'
 * privato, e l'unico modo per creare messaggi di tipi disparati e' quello di
 * utilizzare i metodi statici di tipo build sotto definiti. Questa scelta e'
 * stata fatta per evitare costruttori con troppi parametri di cui la maggior
 * parte inutili
 * 
 * Unico caso speciale della classe e' il messaggio di richiesta relativo alla
 * connessione P2P fra due client. In tal caso, il server deve informare il
 * client ricevente che quest ultimo deve aprire una connessione su cui
 * aspettare un client mittente, e quindi questo messaggio in particolare di
 * richiesta non avra' come ricevente il server ma lo avra' come mittente.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class RequestMessage extends Message {

	// codice di richiesta
	private RequestCode reqCode;

	// se la richiesta era di tipo Msg2Friend/ChatRoom contiene il
	// messaggio testuale da inviare
	private TextMessage text;

	// per le operazioni che necessitano di un nickname (register,
	// friendship...)
	private String nickname;

	// per la registrazione, specifica il linguaggio dell'utente che si registra
	private String language;

	private RequestMessage(RequestCode reqCode, String sender, String receiver, String nickname, String language,
			TextMessage text) {
		// COSTRUTTORE PRIVATO, viene invocato dai metodi statici sottostanti
		super(sender, receiver, Message.REQUEST);
		this.language = language;
		this.nickname = nickname;
		this.text = text;
		this.reqCode = reqCode;
	}

	/*
	 * Iniziano adesso una serie di metodi statici per generare tutti i tipi di
	 * messaggi di richiesta, come definito in RequestCode.java. Dopo di che vi
	 * saranno una serie di metodi getter
	 */

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Richiesta di
	 * Registrazione".
	 *
	 * @param nickname
	 *            nickname che si vuole registrare
	 * @param language
	 *            lingua dell'utente
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei due parametri passati e' null
	 */
	public static RequestMessage BuildRegister(String nickname, String language) {

		if (nickname == null || language == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.REGISTER, nickname, Message.SERVERNAME, nickname, language, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Richiesta di
	 * Accesso".
	 * 
	 * @param nickname
	 *            nickname dell'utente che vuole loggare
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se il parametro e' null
	 */
	public static RequestMessage BuildLogin(String nickname) {
		if (nickname == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.LOGIN, nickname, Message.SERVERNAME, nickname, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Verifica di
	 * esistenza di un utente".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param nickname
	 *            il nick che voglio verificare se esiste
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildLookUp(String sender, String nickname) {
		if (nickname == null || sender == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.LOOKUP, sender, Message.SERVERNAME, nickname, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Aggiunta amico".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param nickname
	 *            il nick che voglio aggiungere agli amici
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildFriendship(String sender, String nickname) {
		if (nickname == null || sender == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.FRIENDSHIP, sender, Message.SERVERNAME, nickname, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Lista amici".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se il parametro e' null
	 */
	public static RequestMessage BuildFriendList(String sender) {
		if (sender == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.FRIEND_LIST, sender, Message.SERVERNAME, null, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Invia il file ad
	 * un amico".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param receiver
	 *            l'utente a cui voglio inviare il file
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildFileToFriend(String sender, String receiver) {
		if (sender == null || receiver == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.FILE2FRIEND, sender, Message.SERVERNAME, receiver, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Invia un
	 * messaggio ad un amico".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param receiver
	 *            l'utente a cui voglio inviare il messaggio
	 * @param message
	 *            il messaggio da inviare
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildMessageToFriend(String sender, String receiver, TextMessage message) {
		if (sender == null || receiver == null || message == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.MSG2FRIEND, sender, Message.SERVERNAME, receiver, null, message);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Invia un
	 * messaggio ad una group chat".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param chatName
	 *            il nome della chat a cui inviare il messaggio
	 * @param message
	 *            il messaggio da inviare
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildMessageToChatroom(String sender, String chatName, TextMessage message) {
		if (sender == null || chatName == null || message == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.CHATROOM_MSG, sender, Message.SERVERNAME, chatName, null, message);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Crea chatroom".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param chatName
	 *            nome della chatroom che si vuole creare
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildCreateChatroom(String sender, String chatName) {
		if (sender == null || chatName == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.CREATE_CHATROOM, sender, Message.SERVERNAME, chatName, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Aggiungi utente
	 * alla chatroom".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param chatName
	 *            il nome della chat a cui mi voglio unire
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildJoinChatroom(String sender, String chatName) {
		if (sender == null || chatName == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.ADD_TO_CHATROOM, sender, Message.SERVERNAME, chatName, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Richiedi lista
	 * delle chatroom".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildChatroomList(String sender) {
		if (sender == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.CHATROOM_LIST, sender, Message.SERVERNAME, null, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Elimina
	 * chatroom".
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param chatName
	 *            il nome della chat che vuol essere eliminata
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildDeleteChatroom(String sender, String chatRoom) {
		if (sender == null || chatRoom == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.CLOSE_CHAT, sender, Message.SERVERNAME, chatRoom, null, null);
	}

	/**
	 * Metodo statico per la creazione di un messaggio di tipo "Richiedi ad un
	 * client l'apertura di una connessione P2P per lo scambio di file"
	 * 
	 * @param sender
	 *            l'utente che fa richiesta
	 * @param receiver
	 *            l'utente che deve aprire la connessione
	 * @return il messaggio di richiesta costruito
	 * @throws NullPointerException
	 *             se uno dei parametri e' null
	 */
	public static RequestMessage BuildOpenConnection(String sender, String receiver) {
		if (sender == null || receiver == null)
			throw new NullPointerException();

		return new RequestMessage(RequestCode.OPEN_P2PCONN, Message.SERVERNAME, receiver, sender, null, null);
	}

	/* fine dei metodi statici per creare messaggi, adesso metodi getter */

	/**
	 * Restituisce il tipo della richiesta di tale messaggio
	 * 
	 * @return il tipo della richiesta
	 */
	public RequestCode TypeOfRequest() {
		return this.reqCode;
	}

	/**
	 * Restituisce il messaggio di testo da inviare
	 * 
	 * @return il messaggio di testo
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public TextMessage getTextMessage() throws NotAFieldException {
		switch (this.reqCode) {
		case MSG2FRIEND:
		case CHATROOM_MSG:
			return this.text;
		default:
			throw new NotAFieldException();
		}
	}

	/**
	 * Restituisce il target di tale richiesta, che e' sempre un campo
	 * significativo tranne nel caso in cui la richiesta sia di tipo lista
	 * amici/chatroom. Il target e' il campo a cui e' rivolta la richiesta,
	 * quindi per esempio il nickname a cui voglio inviare un messaggio, il nome
	 * della chat che voglio creare...
	 * 
	 * @return il target della richiesta
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public String getTarget() throws NotAFieldException {
		switch (this.reqCode) {
		case FRIEND_LIST:
		case CHATROOM_LIST:
			throw new NotAFieldException();
		default:
			return this.nickname;
		}
	}

	/**
	 * Restituisce la lingua del client da registrare
	 * 
	 * @return la lingua del client
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public String getLanguage() throws NotAFieldException {
		switch (this.reqCode) {
		case REGISTER:
			return this.language;
		default:
			throw new NotAFieldException();
		}
	}

}
