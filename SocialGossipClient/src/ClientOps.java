import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import condivise.MessageHandler;
import condivise.Notifier;
import condivise.RequestMessage;
import condivise.ResponseMessage;
import condivise.TextMessage;
import exceptions.MalformedMessageException;
import exceptions.NotAFieldException;

/**
 * Questa classe implementa le operazioni di richiesta che il client effettua
 * verso il server (operazioni di Login, Registrazione, Invio messaggio..).
 * 
 * Tutte le operazioni di richiesta sono svolte in questo modo: questa classe
 * crea il messaggio dai parametri passati, invia il messaggio in formato JSON,
 * ed aspetta la richiesta per un tempo limitato, se non riceve la richiesta nel
 * lasso di tempo impostato assume che ci sia stato un problema, altrimenti
 * interpreta la risposta e consegna alla GUI (che utilizza questa classe) il
 * risultato dell'operazione in un formato noto a priori.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class ClientOps {

	// array degli username degli amici attualmente online del client
	private Vector<String> onlineFriends;

	// array degli username degli amici attualmente offline del client
	private Vector<String> offlineFriends;

	// timeout richiesta
	private int timeout = 2000;

	// socket di connessione al server
	private Socket controlSocket = null;

	// dimensione buffer per l'invio/ricezione messaggi
	private int buffDim = 2048;

	// username del client, aggiornato dopo una richiesta di login/registrazione
	public String myUsername = null;

	// il socket multicast delle chatroom
	private MulticastSocket chatroomSocket;

	// la map di chatroom
	private ConcurrentHashMap<String, InetAddress> chatrooms;

	// gui utente
	private SG_Home home;

	public ClientOps(Vector<String> online, Vector<String> offline, Socket controlSocket,
			MulticastSocket chatroomSocket, ConcurrentHashMap<String, InetAddress> chatrooms, SG_Home gui) {
		// COSTRUTTORE
		onlineFriends = online;
		offlineFriends = offline;
		this.chatroomSocket = chatroomSocket;
		this.chatrooms = chatrooms;
		home = gui;
		// effettuo la connessione al server
		try {
			this.controlSocket = controlSocket;
			controlSocket.setSoTimeout(timeout);
		} catch (IOException e) {
			// errore nel setting del timeout
			System.out.println("Errore connessione al server");
		}

	}

	// UTILITY PRIVATE DELLA CLASSE

	/**
	 * Invia un messaggio di tipo richiesta sul socket di controllo, occupandosi
	 * anche della traduzione da Message a JSON
	 * 
	 * @param m,
	 *            il messaggio da inviare
	 * @return 0 in caso di succeso, -1 altrimenti
	 */
	private int sendRequest(RequestMessage m, Notifier stub) {
		// 'traduco' il messaggio in JSON
		MessageHandler msgHandler = new MessageHandler();
		String msg = "";

		try {
			// preparo il messaggio in formato JSON
			JSONObject obj = msgHandler.Message2JSONObject(m);
			msg = obj.toJSONString();

			// preparo un ObjectOutputStream, inviero' per prima cosa la size
			// del messaggio, poi il messaggio ed infine eventualmente lo stub,
			// se e' una richiesta di registrazione o di login
			ObjectOutputStream objOut = new ObjectOutputStream(this.controlSocket.getOutputStream());
			// devo mandare la size del messaggio se non devo inviare anche lo
			// stub, altrimenti la size + 1
			if (stub == null) // stub non da inviare
				objOut.writeInt(msg.length());
			else // stub da inviare
				objOut.writeInt(msg.length() + 1);
			// mando il messaggio adesso
			objOut.writeObject(msg);

			// ed eventualmente lo stub
			if (stub != null)
				objOut.writeObject(stub);

		} catch (MalformedMessageException e) {
			return -1;
		} catch (IOException e) {
			// errore invio messaggio
			return -1;
		}
		// successo
		return 0;

	}

	/**
	 * Utility privata: Ottiene la risposta del server al messaggio di richiesta
	 * inviato
	 * 
	 * @return il messaggio di risposta
	 */
	private ResponseMessage getResponse() {
		// la risposta del server viaggera' su un BufferedWriter
		InputStreamReader in;
		String response;
		long size;
		try {
			// mi preparo il BufferedReader che leggera' le richieste in entrata
			in = new InputStreamReader(controlSocket.getInputStream());
			BufferedReader inbuff = new BufferedReader(in);
			// leggo la size del messaggio di risposta
			size = Long.parseLong(inbuff.readLine());
			// leggo il messaggio vero e proprio
			response = inbuff.readLine();
			if (response.length() != size)
				System.out.println("Lunghezza messaggio e size non corrispondono");
			// ignoro questo errore, e vedo se il messaggio e' comunque sensato
		} catch (SocketTimeoutException e) {
			// timeout scattato
			return null;
		} catch (IOException e) {
			// errore inaspettato nella lettura
			return null;
		}
		// parso il messaggio e lo restituisco
		MessageHandler handler = new MessageHandler();
		ResponseMessage m = null;
		try {
			// provo il parsing
			m = (ResponseMessage) handler.JSONString2Message(response);
		} catch (MalformedMessageException e) {
			return null;
		}
		System.out.println(response);
		// successo, il messaggio di risposta e' arrivato ed e' sensato
		return m; // ritorno il messaggio parsato
	}

	/**
	 * Funzione che si occupa di creare una richiesta di registrazione, che poi
	 * spedisce al server; il messaggio di ritorno sara' poi interpretato e il
	 * valore di ritorno dipendera' da esso
	 * 
	 * @param username,
	 *            username da registrare
	 * @param language,
	 *            linguaggio preferito dal nuovo utente
	 * @return 0 in caso la richiesta di registrazione sia andata a buon fine, 1
	 *         se l'username e'gia' stato registrato, -1 altrimenti
	 */
	public int RegisterRequest(String username, String language) {
		if (username == "" || language == "")
			return -1;

		// creo lo stub da inviare al server
		Notifier stub;
		try {
			RMINotifier callback = new RMINotifier(onlineFriends, offlineFriends, home);
			stub = (Notifier) UnicastRemoteObject.exportObject(callback, 0);
		} catch (RemoteException e) {
			e.printStackTrace();
			return -1;
		}

		// creo il messaggio di richiesta
		RequestMessage sendM = RequestMessage.BuildRegister(username, language);
		if (sendRequest(sendM, stub) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// setto myUsername
			myUsername = username;
			return 0;
		case NICK_ALREADY_TAKEN:
			// username gia' in uso
			return 1;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}

	}

	/**
	 * Funzione che si occupa della richiesta di login dell'utente, creando la
	 * richiesta che spedisce al server, per poi aspettare ed interpretare una
	 * risposta
	 * 
	 * @param username,
	 *            unico parametro richiesto per effettuare il login
	 * @return 0 in caso di successo, 1 se l'username non e' registrato, -1
	 *         altrimenti
	 */
	public int LoginUser(String username) {
		if (username == "")
			return -1;

		// creo lo stub da inviare al server
		Notifier stub;
		try {
			RMINotifier callback = new RMINotifier(onlineFriends, offlineFriends, home);
			stub = (Notifier) UnicastRemoteObject.exportObject(callback, 0);
		} catch (RemoteException e) {
			return -1;
		}

		// creo il messaggio di richiesta
		RequestMessage sendM = RequestMessage.BuildLogin(username);
		if (sendRequest(sendM, stub) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// setto myUsername
			myUsername = username;
			return 0;
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return 1;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}
	}

	/**
	 * Funzione che si occupa di richiedere al server la lista degli amici del
	 * client
	 * 
	 * @return la lista degli amici del server in caso di successo, null se il
	 *         client non ha amici o in caso di errore
	 * 
	 */
	public String[] getFriendsList() {
		// controllo che il client abbia un username, ergo sia loggato
		if (myUsername == null)
			return null;

		RequestMessage msg = RequestMessage.BuildFriendList(myUsername);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return null;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return null; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// estraggo la lista di username
			String[] friends;

			try {
				friends = recvM.getList();
			} catch (NotAFieldException e) {
				return null; // errore inaspettato nel formato
			}

			return friends;
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return null;
		case OP_FAIL: // errore generico server
			return null;
		default:
			return null;
		}

	}

	/**
	 * Funzione che si occupa di generare la richiesta per il lookup dell'
	 * esistenza di un utente
	 * 
	 * @param targetName,
	 *            username dell'utente che vogliamo vedere
	 * @return 15 in caso di esistenza dell'utente ed esso e' online, 16 se
	 *         l'utente esiste ma e' offline, 1 se l'utente e' sconosciuto, -1
	 *         altrimenti
	 */
	public int NewLookup(String targetName) {
		// controllo che il client abbia un username
		if (myUsername == null || targetName == null) {
			System.out.println("non sono online");
			return -1;
		}

		// creo il messaggio di richiesta di lookup
		RequestMessage msg = RequestMessage.BuildLookUp(myUsername, targetName);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK: {
			// utente esiste
			try {
				if (recvM.isOnline())
					return 15;
				else
					return 16;
			} catch (NotAFieldException e) {
				return -1;
			}
		}
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return 1;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}

	}

	/**
	 * Funzione che si occupa di generare la richiesta per l'aggiunta di un
	 * nuovo amico fra quelli del client richiedente
	 * 
	 * @param targetName,
	 *            username dell'utente che vogliamo aggiungere
	 * @return 0 in caso di creazione corretta della relazione di amicizia, 1 se
	 *         l'utente e' sconosciuto, -1 altrimenti
	 */
	public int NewFriendship(String targetName) {
		// controllo che il client abbia un username, ergo sia loggato
		if (myUsername == null)
			return -1;

		// creo il messaggio di richiesta di amicizia
		RequestMessage msg = RequestMessage.BuildFriendship(myUsername, targetName);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// relazione di amicizia aggiunta
			return 0;
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return 1;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}

	}

	/**
	 * Funzione che si occupa di inviare un generico messaggio testuale ad un
	 * amico, attraverso la chat presente nell'interfaccia utente; per fare cio'
	 * facciamo una richiesta al server, che si occupera' di recapitare il
	 * messaggio per nostro conto al destinatario. Dopodiche aspetteremo il
	 * riscontro positivo dal server
	 * 
	 * @param friend,
	 *            username del destinatario del messaggio
	 * @param text,
	 *            il testo del messaggio da inviare
	 * @return 0 in caso il messaggio sia stato inviato correttamente, 1 se
	 *         friend non e' un utente registrato, 2 se il destinatario non e'
	 *         un nostro amico, 3 se il destinatario non e' online al momento,
	 *         -1 altrimenti
	 */
	public int Message2Friend(String friend, String text) {
		// controllo che il client abbia un username, e che i parametri non
		// siano null
		if (myUsername == null || friend == null || text == null)
			return -1;

		// creo il messaggio testuale da inviare all'amico
		TextMessage txtmsg = TextMessage.BuildTextMessage(myUsername, friend, text);

		// creo il messaggio di richiesta di invio al server
		RequestMessage msg = RequestMessage.BuildMessageToFriend(myUsername, friend, txtmsg);

		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// messaggio recapitato correttamente
			return 0;
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return 1;
		case NOT_A_FRIEND:
			// destinatario non e' un amico
			return 2;
		case USER_OFFLINE:
			// utente offline
			return 3;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}
	}

	/**
	 * Funzione che si occupa dell'invio di un file ad un amico, l'operazione si
	 * svolge nel seguente modo: prima viene generata ed inviata una richiesta
	 * al server per l'invio di un file ad un amico, dopodiche attendiamo per
	 * l'ack dal server (in modo analogo alle altre richieste); sara' compito
	 * del server informare il client ricevente dell'arrivo di un nuovo file.
	 * Dopo aver ricevuto la risposta possiamo dunque procedere all'invio del
	 * file creando una connessione direttamente con il client destinatario,
	 * secondo il modello p2p; l'invio sara' effettuato utilizzando la libreria
	 * java NIO
	 * 
	 * @param friend
	 * @param filename
	 * @return 0 in caso di successo, 1 in caso l'amico non sia registrato, 2
	 *         nel caso il destinatario non sia un amico, 3 se il destinatario
	 *         e' offline, 5 se il file non esiste -1 altrimenti
	 */
	public int File2Friend(String friend, String filename) {
		// controllo che il client abbia un username, e che i parametri non
		// siano null
		if (myUsername == null || friend == null || filename == null)
			return -1;

		// apro subito il file channel per controllare se il file esiste
		FileChannel fc = null;
		try {
			fc = FileChannel.open(Paths.get(filename), StandardOpenOption.READ);
		} catch (NoSuchFileException e) {
			// file non esiste
			return 5;
		} catch (IOException e) {
			// errore
			return -1;
		}

		// creo il messaggio di richiesta di invio di un file
		RequestMessage msg = RequestMessage.BuildFileToFriend(myUsername, friend);

		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// il server ha informato il destinatario correttamente
			break;
		case NICKNAME_UNKNOWN:
			// username non riconosciuto
			return 1;
		case NOT_A_FRIEND:
			// destinatario non e' un amico
			return 2;
		case USER_OFFLINE:
			// utente offline
			return 3;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}

		// procedo alla connessione con il peer

		SocketChannel peerSocket;
		ByteBuffer buff = ByteBuffer.allocate(buffDim); // alloco il buffer NIO

		try {
			// il primo parametro conterra' l'ip del destinatario, il secondo la
			// porta remota su cui e' in ascolto il client
			SocketAddress peerAddress = new InetSocketAddress(recvM.getPeerAddress(), recvM.getPeerPort());
			peerSocket = SocketChannel.open(peerAddress); // mi connetto

		} catch (NotAFieldException e) {
			return -1;
		} catch (IOException e) {
			// errore creazione channel/invio
			return -1;
		}
		// connessione effettuata, invio prima la size del filename

		buff.putLong(filename.length());
		buff.flip(); // 0(position) - filename.length(limit)

		int byteswritten = 0;
		// safe-write, invio la size
		try {
			while ((byteswritten += peerSocket.write(buff)) != buff.limit())
				;
			buff.clear();

			// adesso invio il filename
			buff.put(filename.getBytes());
			buff.flip();
			byteswritten = 0;
			while ((byteswritten += peerSocket.write(buff)) != buff.limit())
				;
			buff.clear();

			// procedo all'invio della size del file
			long filedim = fc.size();
			buff.putLong(filedim);
			buff.flip();
			byteswritten = 0;
			while ((byteswritten += peerSocket.write(buff)) != buff.limit())
				;
			buff.clear();

			// adesso scrivo nel buffer il file vero e proprio,
			// leggendo allo stesso tempo dal FileChannel
			byteswritten = 0;
			while (byteswritten < filedim) {
				int bytesread = fc.read(buff);
				if (bytesread == -1) {
					return -1; // connessione chiusa dall'altro peer
				}
				buff.flip(); // 0 - bytesread(limit)

				// invio il contenuto del buffer in modo safe, scrivendo
				// esattamente bytesread bytes
				for (int i = 0; i < bytesread; i += peerSocket.write(buff))
					;

				byteswritten += bytesread; // aggiorno numero di bytes scritti

				buff.clear();
			}

			// fine, posso chiudere il socket ed il channel del file
			fc.close();
			peerSocket.close();
		} catch (IOException e) {
			return -1; // errore invio
		}

		// a questo punto ho inviato correttamente il file
		return 0;
	}

	/**
	 * Funzione che si occupa dell'invio di un messaggio ad una chatroom;
	 * l'utente che vuole inviare il messaggio deve essere un membro del gruppo,
	 * ed inviare dunque una normale richiesta al server, che si occupera' poi
	 * di procedere all'invio in Multicast del messaggio contenuto nella
	 * richiesta. Anche in questo caso aspetteremo la risposta del server
	 * 
	 * @param idchat,
	 *            nome della chatroom a cui inviare il messaggio
	 * @param text,
	 *            testo del messaggio
	 * @return 0 in caso di successo, 6 se idchat non corrisponde ad una chat
	 *         aperta, 7 se non siamo registrati alla chat room, 8 se non c'e'
	 *         nessun altro utente online nella chatroom -1 altrimenti
	 */
	public int MessageToChatRoom(String idchat, String text) {
		// controllo che il client abbia un username e che i parametri non siano
		// null
		if (myUsername == null || idchat == null || text == null)
			return -1;

		// creo il messaggio testuale da inviare ai membri del gruppo
		TextMessage txtmsg = TextMessage.BuildTextMessage(myUsername, idchat, text);

		// creo il messaggio di richiesta
		RequestMessage msg = RequestMessage.BuildMessageToChatroom(myUsername, idchat, txtmsg);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK:
			// messaggio inviato
			return 0;
		case CHATROOM_UNKNOWN:
			return 6;
		case NICKNAME_UNKNOWN:
			// non siamo registrati alla chatroom
			return 7;
		case NO_ONE_ONLINE:
			return 8;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}

	}

	/**
	 * Funzione che invia la richiesta di creazione di una chatroom e ne attende
	 * la risposta (che comprende fra i suoi campi anche l'indirizzo multicast
	 * della chatroom)
	 * 
	 * @param idchat
	 *            il nome della chat da creare
	 * @return 0 se la creazione della chat va a buon fine, 1 se l'utente
	 *         richiedente non esistesse, 4 se il nome della chat e' occupato,
	 *         -1 altrimenti
	 */
	public int CreateChatroom(String idchat) {
		// controllo che il client abbia un username e che i parametri non siano
		// null
		if (myUsername == null || idchat == null)
			return -1;

		// creo il messaggio di richiesta
		RequestMessage msg = RequestMessage.BuildCreateChatroom(myUsername, idchat);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK: {
			// devo aggiungermi al gruppo multicast di tale chatroom
			try {
				this.chatroomSocket.joinGroup(recvM.getChatroomAddress());
				this.chatrooms.put(idchat, recvM.getChatroomAddress());
			} catch (IOException e) {
				return -1;
			} catch (NotAFieldException e) {
				return -1;
			}
			return 0;
		}
		case NICKNAME_UNKNOWN:
			return 1;
		case NICK_ALREADY_TAKEN:
			return 4;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}
	}

	/**
	 * Funzione usata per fare una richiesta di join ad una chatroom, attendere
	 * una risposta che contenga l'indirizzo multicast di tale chat per poi
	 * unirvisi
	 * 
	 * @param chatId
	 *            Il nome della chat a cui unirsi
	 * @return 0 se tutto va a buon fine, 1 se l'utente richiedente non esiste,
	 *         6 se la chatroom non esiste, 9 se l'utente fa gia' parte della
	 *         chatroom a cui richiede l'aggiunta, -1 in caso di errore generico
	 */
	public int newJoinChatroom(String chatId) {
		// controllo che il client abbia un username e che i parametri non siano
		// null
		if (myUsername == null || chatId == null)
			return -1;

		// creo il messaggio di richiesta
		RequestMessage msg = RequestMessage.BuildJoinChatroom(myUsername, chatId);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK: {
			// devo aggiungermi al gruppo multicast di tale chatroom
			try {
				this.chatroomSocket.joinGroup(recvM.getChatroomAddress());
				this.chatrooms.put(chatId, recvM.getChatroomAddress());
			} catch (IOException e) {
				return -1;
			} catch (NotAFieldException e) {
				return -1;
			}
			return 0;
		}
		case NICKNAME_UNKNOWN: // se l'utente che ha fatto richiesta non esiste
			return 1;
		case CHATROOM_UNKNOWN: // se la chatroom non esiste
			return 6;
		case ALREADY_IN_CHATROOM:// se l'utente e' gia' nella chatroom
			return 9;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}
	}

	/**
	 * Funzione usata per fare una richiesta di listing di chatroom, ed
	 * attenderne la risposta
	 * 
	 * @return la lista delle chatroom con indicazione dell'appartenenza
	 *         dell'utente richiedente, o null in caso di errore
	 */
	public String[] listChatroom() {
		// controllo che il client abbia un username
		if (myUsername == null)
			return null;

		// creo il messaggio di richiesta
		RequestMessage msg = RequestMessage.BuildChatroomList(myUsername);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return null;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return null; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK: {
			// restituisco la lista
			try {
				return recvM.getList();
			} catch (NotAFieldException e) {
				return null;
			}
		}
		default:
			return null;
		}
	}

	/**
	 * Funzione usata per fare una richiesta di chiusura di una chatroom, e
	 * attendere la risposta del server
	 * 
	 * @param chatId
	 *            Il nome della chat da cancellare
	 * @return 0 se tutto va a buon fine, 1 se l'utente richiedente non esiste o
	 *         non fa parte della chatroom, 6 se la chatroom non esistesse, -1
	 *         altrimenti
	 */
	public int newCloseChatroom(String chatId) {
		// controllo che il client abbia un username e che i parametri non siano
		// null
		if (myUsername == null || chatId == null)
			return -1;

		// creo il messaggio di richiesta
		RequestMessage msg = RequestMessage.BuildDeleteChatroom(myUsername, chatId);
		// invio il messaggio di richiesta
		if (sendRequest(msg, null) == -1)
			return -1;

		// aspetto il messaggio di risposta
		ResponseMessage recvM = getResponse();

		if (recvM == null)
			return -1; // timeout scattato, oppure errore in lettura

		switch (recvM.TypeOfResponse()) {
		case OP_OK: {
			// devo eliminarmi dal gruppo multicast di tale chatroom
			InetAddress addr = this.chatrooms.remove(chatId);
			if (addr != null) {
				try {
					this.chatroomSocket.leaveGroup(addr);
				} catch (IOException e) {
					return -1;
				}
			}
			return 0;
		}
		case NICKNAME_UNKNOWN: // se l'utente che ha fatto richiesta non esiste
			return 1;
		case CHATROOM_UNKNOWN: // se la chatroom non esiste
			return 6;
		case OP_FAIL: // errore generico server
			return -1;
		default:
			return -1;
		}
	}

}
