package condivise;

import java.net.InetAddress;

import exceptions.NotAFieldException;

/**
 * Classe che modella i messaggi di risposta del server verso il client. Un
 * messaggio di risposta e' costituito da: - Un codice di risposta, che puo'
 * essere positivo o negativo. I vari codici sono definiti da
 * ResponseCode.java. - Un codice di richiesta, a cui si riferisce la
 * risposta Inoltre, in caso di risposta affermativa da parte del server,
 * possono essere presenti vari campi, tra cui: - La lista, che puo' essere
 * la lista degli amici o delle chatroom in caso di tale richiesta -
 * indirizzo e porta del client che aspetta la connessione P2P per lo
 * scambio di file
 * 
 * Unico caso particolare di questa classe e' quello in cui il client
 * risponde alla richiesta del server (e quindi di un altro client) di
 * aprire una connessione P2P per lo scambio di file. In tal caso il sender
 * e' il client ed il receiver e' il server
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class ResponseMessage extends Message {

	// il codice che definisce l'esito dell'operazione
	private ResponseCode respCode;

	// il codice della richiesta a cui corrisponde tale risposta
	private RequestCode myRequest;

	// la lista degli amici/delle chatroom
	private String[] nameList;

	// l'indirizzo del client ricevente in caso di scambio di file tra pari
	private InetAddress peerAddr;

	// la porta del client ricevente in caso di scambio di file tra peer
	private int peerPort;

	// l'indirizzo della chatroom creata o a cui si e' unito il client
	private InetAddress chatroomAddress;

	private boolean isOnline;

	private ResponseMessage(String sender, String receiver, ResponseCode respCode, RequestCode request, String[] list,
			InetAddress addr, int port, InetAddress chatRoomAddr, boolean isOnline) {
		// COSTRUTTORE PRIVATO, un messaggio di risposta si puo' creare solo
		// con i metodi statici definiti sotto
		super(sender, receiver, Message.RESPONSE);
		this.myRequest = request;
		this.respCode = respCode;
		this.nameList = list;
		this.peerAddr = addr;
		this.peerPort = port;
		this.chatroomAddress = chatRoomAddr;
		this.isOnline = isOnline;
	}

	/*
	 * Adesso una serie di simil costruttori statici per creare messaggi di
	 * risposta
	 */

	/**
	 * Metodo statico per creare un generico messaggio di errore
	 * 
	 * @param respCode
	 *            codice dell'errore
	 * @param request
	 *            la richiesta a cui si riferisce tale risposta
	 * @param receiver
	 *            client a cui devo rimbalzare la richiesta
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static ResponseMessage BuildError(ResponseCode respCode, RequestCode request, String receiver) {
		if (respCode == null || receiver == null || request == null)
			throw new NullPointerException();
		return new ResponseMessage(Message.SERVERNAME, receiver, respCode, request, null, null, 0, null, false);
	}

	/**
	 * Metodo statico per creare un ack ad una richiesta di listing
	 * 
	 * @param receiver
	 *            client a cui devo inviare la risposta
	 * @param request
	 *            la richiesta a cui si riferisce tale risposta
	 * @param list
	 *            lista di nickname/chatroom name della lista da inviare
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static ResponseMessage BuildListAck(String receiver, RequestCode request, String[] list) {
		if (list == null || receiver == null || request == null)
			throw new NullPointerException();
		return new ResponseMessage(Message.SERVERNAME, receiver, ResponseCode.OP_OK, request, list, null, 0, null,
				false);
	}

	/**
	 * Metodo statico per creare un ack ad una richiesta di invio file Questo
	 * messaggio di risposta e' sia quello che il client ricevente invia al
	 * server, che quello che il server invia al primo client che ha fatto
	 * richiesta
	 * 
	 * @param sender
	 * @param receiver
	 *            (client o server) a cui tale messaggio e' indirizzato
	 * @param request
	 *            la richiesta a cui si riferisce tale risposta
	 * @param addr
	 *            l'indirizzo del client per l'invio del file
	 * @param port
	 *            la porta su cui il client sta aspettando una connessione
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static ResponseMessage BuildConnectionP2PAck(String sender, String receiver, RequestCode request,
			InetAddress addr, int port) {
		if (addr == null || receiver == null || request == null)
			throw new NullPointerException();
		return new ResponseMessage(sender, receiver, ResponseCode.OP_OK, request, null, addr, port, null, false);
	}

	/**
	 * Metodo statico per creare una risposta positiva ad una richiesta di tipo
	 * create chatroom oppure join chatroom. Il server risponde al client
	 * richiedente con l'indirizzo multicast della chatroom. Tutti i messaggi
	 * destinati a tale chatroom saranno/sono inviati tramite datagram su questo
	 * socket
	 * 
	 * @param receiver
	 *            il client che ha fatto la richiesta
	 * @param reqCode
	 *            il codice della richiesta corrispondente
	 * @param addr
	 *            il socket multicast della chatroom
	 * @return il messaggio costruito
	 */
	public static ResponseMessage BuildChatroomAck(String receiver, RequestCode reqCode, InetAddress addr) {
		if (addr == null || receiver == null || reqCode == null)
			throw new NullPointerException();
		return new ResponseMessage(Message.SERVERNAME, receiver, ResponseCode.OP_OK, reqCode, null, null, 0, addr,
				false);
	}

	/**
	 * Metodo statico per creare un ack generico, senza parametri aggiuntivi
	 * 
	 * @param receiver
	 *            client a cui devo inviare la risposta
	 * @param request
	 *            la richiesta a cui si riferisce tale risposta
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static ResponseMessage BuildAck(String receiver, RequestCode request) {
		if (receiver == null || request == null)
			throw new NullPointerException();
		return new ResponseMessage(Message.SERVERNAME, receiver, ResponseCode.OP_OK, request, null, null, 0, null,
				false);
	}

	/**
	 * Metodo statico per creare un ack a una richiesta di amicizia o di lookup
	 * di un utente. Di aggiuntivo c'e' lo stato dell'utente
	 * 
	 * @param receiver
	 *            client a cui devo inviare la risposta
	 * @param request
	 *            la richiesta a cui si riferisce tale risposta
	 * @param isOnline
	 *            se l'utente e' online
	 * @return il messaggio costruito
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public static ResponseMessage BuildOnlineAck(String receiver, RequestCode req, boolean isOnline) {
		if (receiver == null || req == null)
			throw new NullPointerException();
		return new ResponseMessage(Message.SERVERNAME, receiver, ResponseCode.OP_OK, req, null, null, 0, null,
				isOnline);
	}

	/* Adesso una serie di metodi getter per reperire i campi del messaggio */

	/**
	 * Restituisce il tipo della risposta di tale messaggio
	 * 
	 * @return il tipo della risposta
	 */
	public ResponseCode TypeOfResponse() {
		return this.respCode;
	}

	/**
	 * Restituisce il tipo della richiesta a cui e' associata tale risposta
	 * 
	 * @return il tipo della richiesta
	 */
	public RequestCode TypeOfRequest() {
		return this.myRequest;
	}

	/**
	 * Restituisce la lista associata al messaggio
	 * 
	 * @return la lista di nickname/ chatroom name
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public String[] getList() throws NotAFieldException {

		if (this.nameList == null)
			throw new NotAFieldException();
		return this.nameList;
	}

	/**
	 * Restituisce l'indirizzo del peer in cui esso e' in ascolto per nuove
	 * connessioni
	 * 
	 * @return l'indirizzo del peer
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public InetAddress getPeerAddress() throws NotAFieldException {

		if (this.peerAddr == null)
			throw new NotAFieldException();
		return this.peerAddr;
	}

	/**
	 * Restituisce la porta su cui il peer sta aspettando nuove connessioni
	 * 
	 * @return la porta nel messaggio
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public int getPeerPort() throws NotAFieldException {

		if (this.peerPort == 0)
			throw new NotAFieldException();
		return this.peerPort;
	}

	/**
	 * Restituisce l'indirizzo IP della chatrooms
	 * 
	 * @return l'indirizzo della chatroom
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public InetAddress getChatroomAddress() throws NotAFieldException {

		if (this.chatroomAddress == null)
			throw new NotAFieldException();
		return this.chatroomAddress;
	}

	/**
	 * Restituisce lo stato di online/offline di un utente
	 * 
	 * @return true se l'utente e' online, false altrimenti
	 * @throws NotAFieldException
	 *             se il campo richiesto non e' significativo nella richiesta di
	 *             questo particolare messaggio di richiesta
	 */
	public boolean isOnline() throws NotAFieldException {

		if (this.myRequest == RequestCode.FRIENDSHIP || this.myRequest == RequestCode.LOOKUP)
			return this.isOnline;
		throw new NotAFieldException();
	}

}
