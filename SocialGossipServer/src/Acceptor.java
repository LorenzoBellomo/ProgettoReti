import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/**
 * Thread acceptor, resta fermo in attesa di connessioni sulla porta nota del
 * server. Una volta accettata una connessione da parte di un client, questo
 * thread crea un WorkerTask di tipo accettazione, a cui allega il socket del
 * client con cui e' stata appena aperta la connessione e lo mette nella coda
 * condivisa con i thread worker del thread pool. A loro stara' il compito di
 * accettare la connessione dei messaggi
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class Acceptor extends Thread {

	// coda di task. Questo thread fa solo da produttore di task, in quanto
	// genera task di accettazione che mette nella coda condivisa
	private BlockingQueue<WorkerTask> queue;

	// il socket di controllo che aspetta nuove connessioni
	private ServerSocket controlSocket;

	public Acceptor(BlockingQueue<WorkerTask> sockQueue) {
		// COSTRUTTORE
		this.queue = sockQueue;
	}

	public void run() {

		try {
			// creo il socket del server
			this.controlSocket = new ServerSocket(SocialGossipServer.CONTROL_SOCKET_PORT);

			// conterra' il socket restituito da accept(), il socket assegnato
			// ad uno specifico client per gestire le sue richieste
			Socket clientSocket;

			System.out.println("Server pronto per ricevere  nuove connessioni!");
			
			while (!SocialGossipServer.stop) {
				clientSocket = null;
				
				// aspetto una nuova connessione e la assegno al
				// clientSocket
				clientSocket = (Socket) controlSocket.accept();

				// se il clientSocket non e' null l'accept ha rilevato una nuova
				// connessione
				if (clientSocket != null) {
					// aggiungo il clientSocket alla relativa coda; non utilizzo
					// 'offer()' poiche' controllo nell'if precedente che la
					// coda abbia spazio sufficiente ad ospitare un altro
					// elemento.
					// Lo aggiungo come un task di accettazione.
					this.queue.add(new WorkerTask(WorkerTask.ACCEPT, clientSocket, null));
					System.out.println("Ricevuta nuova connessione!");
					// il thread worker bloccato sulla coda in attesa di un
					// elemento sara' ora in grado di prelevare questo task
					// ed aprire con tale client la connessione dei messaggi
				}
				// altrimenti continuo l'esecuzione e torno a controllare il
				// valore di stop
			}
			// uscito dal while, devo terminare

			System.out.println("Chiudendo il Listener Thread..");

			// chiudo il socket principale del server
			controlSocket.close();

		} catch (IOException e) {
			// ignore
		}

	}

}
