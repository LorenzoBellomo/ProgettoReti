import java.net.Socket;

import condivise.Notifier;

/**
 * Rappresentazione di un utente online, classe utilizzata per creare un
 * lista di utenti online gestibile dal server in modo da individuare in
 * modo efficiente il destinatario di messaggi o di notifiche RMI, quali il
 * passaggio di un amico da online-offline e viceversa, o di notifiche
 * 'standard' come la creazione di una nuova relazione di amicizia.
 * 
 * Ogni utente online e' rappresentato da un username, uno stub RMI ed una
 * coppia di socket (il socket attraverso il quale il client ricevera' messaggi 
 * dai suoi amici in chat e quello di controllo).
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class OnlineUser {

	// username del client su SocialGossip
	private String username;

	// Stub dell'interfaccia RMI fornita dal client al login
	private Notifier stub;

	// Socket su cui il client ricevera' i messaggi dai suoi amici in chat
	private Socket messageSocket;

	// Socket di controllo del client, in cui si scambia i messaggi di controllo
	// (richiesta e risposta) con il server
	private Socket controlSocket;

	public OnlineUser(String username, Notifier stub, Socket controlSock, Socket messageSock) {
		// COSTRUTTORE
		this.username = username;
		this.stub = stub;
		this.controlSocket = controlSock;
		this.messageSocket = messageSock;
	}

	/**
	 * Metodo getter per il nome utente
	 * 
	 * @return il nome dell'utente
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Metodo getter per il socket per i messaggi dell'utente online
	 * 
	 * @return il socket dell'utente
	 */
	public Socket getMessageSocket() {
		return this.messageSocket;
	}

	/**
	 * Metodo getter per il socket per i messaggi di controllo dell'utente
	 * online
	 * 
	 * @return il socket dell'utente
	 */
	public Socket getControlSocket() {
		return this.controlSocket;
	}

	/**
	 * Metodo getter per lo stub RMI dell'utente
	 * 
	 * @return lo stub dell'utente online
	 */
	public Notifier getStub() {
		return this.stub;
	}
	
	/* SETTER */
	
	/**
	 * Setter per lo stub RMI dell'utente
	 * 
	 * @param stub
	 */
	public void setStub(Notifier stub) {
		if(stub == null) throw new NullPointerException();
		this.stub = stub;
	}
	
	/**
	 * Setter per l'username utente
	 * 
	 * @param name
	 */
	public void setUsername(String name) {
		if(name == null) throw new NullPointerException();
		this.username = name;
	}
	
	/**
	 * Setter per il socket dei messaggi
	 * 
	 * @param socket
	 */
	public void setMessageSocket(Socket socket) {
		if(socket == null) throw new NullPointerException();
		this.messageSocket = socket;
	}


}
