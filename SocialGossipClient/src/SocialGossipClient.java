
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe principale del client, si occupa di inizializzare tutte le strutture
 * dati condivise e far partire il thread listener e la HOME della GUI
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 *
 */
public class SocialGossipClient {

	// elenco delle chatroom
	private static ConcurrentHashMap<String, InetAddress> chatrooms;
	
	private static final int serverPort = 1898;
	
	private static final int chatroomPort = 7080;

	// elenco dei nickname degli amici online
	private static Vector<String> online;

	// elenco dei nickname degli amici offline
	private static Vector<String> offline;

	// il socket della connessione di controllo
	private static Socket controlSocket;

	// il "ServerSocket" per l'apertura passiva della connessione dei messaggi,
	// sara' il server a fare l'apertura attiva vera e propria
	// inoltre con questo socket apriro' le connessioni P2P dei
	private static ServerSocket acceptor;

	// il socket della connessione dei messaggi
	private static Socket messageSocket;

	// l'indirizzo IP del server
	private static InetAddress serverAddress;

	// il socket Multicast in cui ricevere pacchetti delle chatroom
	private static MulticastSocket chatroomSocket;

	public static void main(String[] args) {

		System.setProperty("preferIPv4Stack", "true");

		// usage check
		if (args.length != 1) {
			System.out.println("Error USAGE: ./SocialGossipClient serverAddress");
			System.exit(1);
		}

		try {
			// prendo l'indirizzo del server da args[0]
			serverAddress = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e2) {
			System.out.println("Invalid serverAddress");
			System.exit(1);
		}
		// ----INIZALIZZAZIONE----
		System.out.println("Client in fase di inizializzazione");
		// inizializzo le strutture dati principali
		chatrooms = new ConcurrentHashMap<>();
		online = new Vector<>();
		offline = new Vector<>();

		int port = 0;
		// apro le connessioni di controllo e dei messaggi
		try {
			// apro una connessione di controllo con il server
			controlSocket = new Socket(serverAddress, serverPort);
			// preparo un acceptor su una porta a caso per accettare la
			// connessione dei messaggi che verra' aperta attivamente dal server
			acceptor = new ServerSocket(0);
			// apro anche il socket multicast per le chatroom
			chatroomSocket = new MulticastSocket(chatroomPort);
			// ottengo la porta effimera del socket acceptor
			port = acceptor.getLocalPort();
			// ottengo l'output stream della connessione di controllo per
			// inviare la porta
			BufferedWriter output = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
			// invio la porta
			output.write(port + "\n");
			output.flush();
			// accetto la connessione del server, ho aperto la connessione
			// dei messaggi
			messageSocket = acceptor.accept();
			// l'acceptor ha finito la sua utilita'
			acceptor.close();
		} catch (IOException e) {
			System.out.println("Connessione con il server fallita");
			System.exit(1);
		}

		SG_Home home = new SG_Home();

		// creo e faccio partire il listener della connessione dei messaggi e
		// delle chatroom
		ClientListener listener = new ClientListener(messageSocket, chatroomSocket, home);
		ClientOps client = new ClientOps(online, offline, controlSocket, chatroomSocket, chatrooms, home);
		listener.start();

		// home grafica
		home.setClientHandler(client);
		new AccessWindow(home, client);

		System.out.println("Client avviato correttamente");

		// ---FASE DI CHIUSURA---
		// join dei thread listener
		try {
			listener.join();
		} catch (InterruptedException e) {
			System.out.println("Interruzione durante la join dei thread listener");
		}
		// chiudo i socket
		try {
			messageSocket.close();
			controlSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
