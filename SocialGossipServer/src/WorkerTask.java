import java.net.Socket;

/**
 * Classe che tiene traccia dei doppi socket degli utenti. Un worker task puo'
 * essere di due tipo: Tipo Accept che mantiene il socket di controllo. La
 * connessione messaggi di tale utente non esiste, in quanto il thread Acceptor,
 * dopo aver accettato la connessione di controllo, genera un task di
 * accettazione, in cui il worker deve ottenere dal canale la porta del client
 * in attesa ed aprire la connessione attivamente per i messaggi. Il tipo Serve
 * invece e' il classico task di lettura delle richieste di un client che pero'
 * ha gia' aperto entrambe le connessioni
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class WorkerTask {

	public static final int ACCEPT = 0;
	public static final int SERVE = 1;

	// il tipo accept o serve del task
	private int type;

	// il socket di controllo, sempre valido
	private Socket controlConn;

	// il socket dei messaggi, null se tipo e' accept (devo ancora aprire tale
	// connessione)
	private Socket messageConn;

	public WorkerTask(int type, Socket control, Socket message) {
		// COSTRUTTORE
		this.type = type;
		this.controlConn = control;
		this.messageConn = message;
	}

	/**
	 * Restituisce l'intero relativo al tipo del task (0 se accept, 1 se serve)
	 * 
	 * @return 0 se task e' di accettazione, 1 altrimenti
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * restituisce il socket di controllo di un client
	 * 
	 * @return il control Socket
	 */
	public Socket getControlSocket() {
		return this.controlConn;
	}

	/**
	 * restituisce il socket dei messaggi di un client
	 * 
	 * @return il message Socket
	 */
	public Socket getMessageSocket() {
		return this.messageConn;
	}

}
