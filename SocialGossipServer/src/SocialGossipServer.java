import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Il server di SocialGossip e' composto essenzialmente da un thread acceptor ed
 * un pool di thread che eseguiranno il codice definito da Worker. Il thread
 * acceptor accetta connessioni in arrivo sulla porta nota definita in questa
 * classe per la connessione di controllo. E fornisce i primi task al thread
 * pool. Il thread pool si occupa di aprire invece la connessione TCP dei
 * messaggi e di eseguire le richieste degli utenti.
 * 
 * Questa classe si occuppa dello spawn e della gestione di tutti questi thread
 * e fornisce dei parametri di setting del server stesso che possono essere
 * modificati prima dell'esecuzione (come il numero di thread nel pool, il
 * numero massimo di connessioni..)
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class SocialGossipServer {

	// variabile utilizzata per interrompere l'esecuzione del server, statica
	// cosi puo' essere acceduta senza dover necessariamente avere l'oggetto
	public static boolean stop;

	// variabile che definisce il numero di thread nel pool di worker
	private static int nthreads = 8;

	// variabile contenente il numero di porta del server che gestisce le
	// connessioni di tipo 'controllo'
	public static int CONTROL_SOCKET_PORT = 1898;

	// lista degli utenti attualmente online (sottoinsieme degli utenti
	// registrati), associa ad ogni utente il suo stub ed il suo socket,
	// passato al momento del login
	private static Vector<OnlineUser> onlineUsers;

	// implementazione del grafo che rappresenta la rete di utenti di
	// SocialGossip, statico poiche' verra' inizializzato una sola volta
	// durante l'intera esecuzione del server
	public static SocialGraph graph;

	// vettore delle chatroom attualmente attive nel server
	private static Vector<ChatRoom> chatrooms;

	// il socket utilizzato per l'invio di datagram sui vari gruppi multicast
	// delle chatroom
	private static DatagramSocket chatroomSocket;

	public static void main(String[] args) {
		// ----INIZALIZZAZIONE----

		System.setProperty("preferIPv4Stack", "true");

		System.out.println("Server in fase di inizializzazione");
		// inizializzo le strutture condivise dai thread
		graph = new SocialGraph();
		onlineUsers = new Vector<OnlineUser>();
		chatrooms = new Vector<ChatRoom>();
		try {
			chatroomSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("Errore nella fase di inizializzazione");
			System.exit(1);
		}

		// SocketQueue, queue dei socket che appartengono ai client che si sono
		// connessi al server la dimensione della queue sara' fissata al numero
		// massimo di client connessi contemporaneamente al server
		BlockingQueue<WorkerTask> sQueue = new LinkedBlockingQueue<WorkerTask>();

		// creo la thread pool, che conterra' un numero fissato di thread che
		// condivideranno la BlockingQueue di socket
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(nthreads);

		// avvio il thread acceptor
		Acceptor acceptor = new Acceptor(sQueue);
		acceptor.start();

		// i task sono dei task 'continuativi', vengono dunque assegnati adesso
		// al thread pool che continuera' ad eseguirli per tutto il tempo di
		// vita del server
		for (int i = 0; i < nthreads; i++)
			pool.execute(new Worker(onlineUsers, graph, sQueue, chatrooms, chatroomSocket));

		System.out.println("Server avviato correttamente");

		// ---FASE DI CHIUSURA---
		// join dei thread listener
		try {
			acceptor.join();
		} catch (InterruptedException e1) {
			System.out.println("Interruzione durante la join dei thread listener");
		}

		// aspetto la terminazione di tutti i task attualmente in esecuzione nel
		// thread pool per un tempo fissato
		pool.shutdown();

		try {
			pool.awaitTermination(10, TimeUnit.SECONDS);

			// chiudo tutti i socket attivi ancora presenti nella coda
			while (!sQueue.isEmpty()) {
				WorkerTask s = sQueue.poll();
				if (s != null) {
					s.getControlSocket().close();
					s.getMessageSocket().close();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
