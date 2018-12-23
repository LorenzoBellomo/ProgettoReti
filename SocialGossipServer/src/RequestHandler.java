
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

import condivise.Message;
import condivise.MessageHandler;
import condivise.RequestCode;
import condivise.RequestMessage;
import condivise.ResponseMessage;
import condivise.TextMessage;
import exceptions.AlreadyAFriendException;
import exceptions.AlreadyInChatroomException;
import exceptions.IllegalLanguageException;
import exceptions.MalformedMessageException;
import exceptions.NameAlreadyInUseException;
import exceptions.NoOneOnlineException;
import exceptions.NoSuchChatException;
import exceptions.NoSuchFriendException;
import exceptions.NotAFieldException;
import exceptions.UnknownUserException;
import exceptions.UserNotOnlineException;

/**
 * Classe incaricata di implementare le operazioni legate alla social network,
 * alle chat e alle chatroom lato server. Non si occupa dunque della
 * lettura/scrittura da e verso il client, ma solo dell'implementazione delle
 * sue richiesta. Se la richiesta e' di tipo sendMsg/fileTo allora in tal caso
 * questa classe gestisce l'invio del messaggio/file al destinatario, ma non
 * gestisce l'ACK verso il client
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class RequestHandler {

	// il grafo del server
	private SocialGraph graph;

	// il vettore con gli utenti online
	private Vector<OnlineUser> online;

	// il vettore delle chatroom
	private Vector<ChatRoom> chatrooms;

	// per il parsing dei messaggi da inviare
	private MessageHandler messageHandler;

	// il traduttore per l'invio di messaggi
	private Translator translator;

	// socket per l'invio dei messaggi in multicast
	private DatagramSocket datagramSocket;

	public RequestHandler(SocialGraph graph, Vector<OnlineUser> online, Vector<ChatRoom> chatrooms,
			DatagramSocket chatroomSocket) {
		// COSTRUTTORE
		this.graph = graph;
		this.online = online;
		this.messageHandler = new MessageHandler();
		this.translator = new Translator();
		this.chatrooms = chatrooms;
		this.datagramSocket = chatroomSocket;
	}

	/* ALCUNE FUNZIONI DI UTILITA' PRIVATE */

	/**
	 * Funzione di utilita' privata, cerca un utente fra quelli online. Se lo
	 * trova lo ritorna altrimenti ritorna null
	 * 
	 * @param nickname
	 *            l'utente che cerco online
	 * @return l'OnlineUser, null se esso non esiste
	 */
	private OnlineUser findOnlineUserByName(String nickname) {
		for (OnlineUser online : this.online) {
			if (online.getUsername().equals(nickname))
				return online;
		}
		return null;
	}

	/**
	 * Funzione di utilita' privata, cerca una chatroom fra le varie chatroom.
	 * Se la trova restituisce tale chatroom, altrimenti restituisce null
	 * 
	 * @param nickname
	 *            il nome della chat che sto cercando
	 * @return la chatRoom, null se essa non esiste
	 */
	private ChatRoom findChatroomByName(String chatName) {
		for (ChatRoom chat : this.chatrooms) {
			if (chat.getChatName().equals(chatName))
				return chat;
		}
		return null;
	}

	/* INIZIO SOCIAL OPS */

	/**
	 * Prova ad inserire un utente con nickname e lingua specificati nel grafo
	 * sociale Se riesce a registrare l'utente, allora lo inserisce anche fra
	 * quelli online
	 * 
	 * @param nickname
	 *            nome dell'utente da aggiungere
	 * @param language
	 *            lingua con cui si registra l'utente
	 * @param onUser
	 *            OnlineUser contenente RMI stub e socket
	 * @throws NameAlreadyInUseException
	 *             se il nickname fosse preso
	 */
	public void RegisterUser(String nickname, String language, OnlineUser onUser) throws NameAlreadyInUseException {

		// controllo la correttezza dei parametri
		if (nickname == null || language == null || onUser == null)
			throw new NullPointerException();

		// se prende il nome del server
		if (nickname.equals(Message.SERVERNAME))
			throw new NameAlreadyInUseException();

		User user = new User(nickname, language);
		// provo ad inserire, se l'utente fosse gia' presente lancia una
		// NameAlreadyInUseException
		this.graph.addNode(user);
		// se sono qua l'eccezione non e' stata lanciata, quindi la
		// registrazione e' avvenuta con successo.
		// non devo notificare gli amici perche' ancora non ne ha
		this.online.add(onUser);
	}

	/**
	 * Fa login di un utente e notifica tutti i client online di lui amici
	 * 
	 * @param user
	 *            la struttura di utente online da aggiungere a this.online
	 * @throws UnknownUserException
	 *             se l'utente che fa richiesta non esiste
	 */
	public void LogInUser(OnlineUser user) throws UnknownUserException {
		// controllo la correttezza del parametro
		if (user == null)
			throw new NullPointerException();

		// ottengo il nome dell'utente
		String nickname = user.getUsername();
		// ottengo tutti i suoi amici (lancia UserUnknown)
		String[] friends = this.graph.getFriends(nickname);
		System.out.println("number of friends = " + friends.length);

		// adesso semplicemente aggiungo l'utente a quelli online, se esso
		// fosse gia' stato online semplicemente aggiorno lo stub, altrimenti
		// inserisco un nuovo utente online.
		// la notifica a tutti i suoi amici pero' la voglio fare solo se
		// l'utente non era online prima
		boolean toNotify = true;
		OnlineUser prev;
		if ((prev = this.findOnlineUserByName(nickname)) != null) {
			// l'utente era gia' online
			toNotify = false;
			this.online.remove(prev);
		}
		// aggiungo l'utente a quelli online
		this.online.add(user);

		// adesso forse devo notificare ogni amico dell'utente
		if (toNotify) {
			// devo notificare
			for (String friend : friends) {
				// guardo quali suoi amici sono online
				OnlineUser online = this.findOnlineUserByName(friend);
				if (online != null) {
					// era online
					try {
						online.getStub().NotifyOnlineFriend(nickname);
					} catch (RemoteException e) {
						// ignora
					}
				}
			}
		}
	}

	/**
	 * Restituisce true se l'utente richiesto esiste ed e' online, false se e'
	 * offline
	 * 
	 * @param nickname
	 *            il nickname da ricercare
	 * @return il valore di verita' di "l'utente e' online"
	 * @throws UnknownUserException
	 *             se l'utente non esiste
	 */
	public boolean LookUpUser(String nickname) throws UnknownUserException {
		if (nickname == null)
			throw new NullPointerException();

		if (this.graph.isUser(nickname)) {
			// l'utente esiste, guardo se e' online
			OnlineUser found = this.findOnlineUserByName(nickname);
			if (found == null) // non e' online
				return false;
			return true;
		}
		// non e' un utente
		throw new UnknownUserException();
	}

	/**
	 * Aggiunge una relazione di amicizia fra i due utenti, e se il secondo
	 * utente e' online gli notifica la nuova amicizia
	 * 
	 * @param nick1
	 *            utente 1, quello che fa richeista
	 * @param nick2
	 *            utente 2, da notificare
	 * @return true se l'utente e' online, false altrimenti
	 * @throws UnknownUserException
	 *             se uno dei due utenti non esiste
	 * @throws AlreadyAFriendException
	 *             se la relazione di amicizia era gia' presente
	 */
	public boolean NewFriendship(String nick1, String nick2) throws UnknownUserException, AlreadyAFriendException {
		// controllo la correttezza dei parametri
		if (nick1 == null || nick2 == null)
			throw new NullPointerException();

		// aggiunge una relazione di amicizia, prima verificando l'esistenza
		// degli utenti richiesti
		this.graph.newFriendship(nick1, nick2);
		// se sono qua allora l'amicizia e' stata creata con successo
		// devo notificare il secondo utente
		OnlineUser online = this.findOnlineUserByName(nick2);
		if (online != null) {
			// lo voglio notificare del suo nuovo amico online
			try {
				online.getStub().NotifyOnlineFriend(nick1);
			} catch (RemoteException e) {
				// ignoro, non informo l'utente nick2 della ricezione di una
				// nuova amicizia
			}
			return true;
		}
		return false;
	}

	/**
	 * Restituisce la lista degli amici di un utente specifico
	 * 
	 * @param nickname
	 *            il nick dell'utente di cui voglio gli amici
	 * @return l'array contenente i nickname degli amici
	 * @throws UnknownUserException
	 *             se l'utente non esiste
	 */
	public String[] ListFriends(String nickname) throws UnknownUserException {
		if (nickname == null)
			throw new NullPointerException();

		// graph.getFriends lancia una NullPointerException se il nickname
		// passato come parametro non e' noto
		return this.graph.getFriends(nickname);
	}

	/* FINE SOCIAL OPS, INIZIO CHAT OPS */

	/**
	 * Gestisce l'invio di un messaggio richiesto. Controlla che sia l'utente
	 * mittente che quello destinatario esistano, oltre a controllare che il
	 * receiver siano online. Se l'esecuzione dell'invio va a buon fine non
	 * viene lanciata alcuna eccezione
	 * 
	 * @param message
	 *            il TextMessage che vuol essere inviato
	 * @throws UnknownUserException
	 *             se mittente o destinatario non esistessero
	 * @throws NoSuchFriendException
	 *             se mittente e destinatario non sono amici
	 * @throws UserNotOnlineException
	 *             se destinatario non e' online
	 * @throws MalformedMessageException
	 *             se il textMessage e' scritto male
	 */
	public void SendMessageToFriend(TextMessage message)
			throws UnknownUserException, NoSuchFriendException, UserNotOnlineException, MalformedMessageException {
		// controllo che il messaggio sia non null
		if (message == null)
			throw new NullPointerException();

		// mi prendo il nick del receiver che mi serve spesso
		String sendTo = message.getReceiver();

		// controllo che siano amici mittente e receiver
		if (!this.graph.areFriends(message.getSender(), sendTo))
			throw new NoSuchFriendException();

		OnlineUser receiver = this.findOnlineUserByName(sendTo);

		// mi preparo le lingue del mittente e del destinatario
		String mittLanguage = this.graph.getUser(message.getSender()).getLanguage();
		String destLanguage = this.graph.getUser(sendTo).getLanguage();
		if (receiver == null)
			throw new UserNotOnlineException();
		// ho controllato che receiver e sender esistano, che l'utente receiver
		// sia online e che i parametri non siano null, adesso posso provare ad
		// inviare il messaggio al receiver

		// prima pero' devo tradurre il messaggio
		try {
			message.setMessage(translator.Translate(message.getMessage(), mittLanguage, destLanguage));
		} catch (IllegalLanguageException e1) {
			// ignoro e invio il messaggio non tradotto
		}
		try {
			this.messageHandler.sendMessage(receiver.getMessageSocket(), message);
		} catch (IOException e) {
			// problema nell'invio all'utente receiver, lancio UserNotOnline,
			// mentre la disconnessione vera e propria del receiver sara'
			// compito del worker
			throw new UserNotOnlineException();
		}
	}

	/**
	 * Gestione della richiesta di scambio di file. La gestione di default e'
	 * (caso di client1 che invia file a client2): client1 manda una richiesta
	 * di invio file verso client2 al server; server invia una openP2PConnection
	 * a client 2; client 2 invia un ACK al server con inserito il suo
	 * indirizzo; server invia un ACK con l'indirizzo di client2 al client1.
	 * Questo metodo si occupa dei messaggi server -> client2 e client2 ->
	 * server. Ritorna il messaggio di risposta che il worker inviera' al client
	 * richiedente, contenente l'indirizzo del secondo client e la porta su cui
	 * esso sara' in attesa
	 *
	 * @param sender
	 *            il nickname dell'utente che invia il file
	 * @param friend
	 *            il nickname dell'amico ricevente il file
	 * @return l'indirizzo del receiver
	 * @throws UserNotOnlineException
	 *             se friend non e' online
	 * @throws NoSuchFriendException
	 *             se friend non e' amico di sender
	 * @throws UnknownUserException
	 *             se uno dei due utenti non esiste
	 * @throws MalformedMessageException
	 *             se receiver ha risposto in un modo non definito
	 */
	public ResponseMessage SendFileToFriend(String sender, String friend)
			throws UserNotOnlineException, NoSuchFriendException, UnknownUserException, MalformedMessageException {
		// controllo la correttezza dei parametri
		if (friend == null || sender == null)
			throw new NullPointerException();

		// controllo se sender e receiver sono amici
		if (!this.graph.areFriends(sender, friend))
			throw new NoSuchFriendException();

		// controllo se il receiver e' online
		OnlineUser receiver = this.findOnlineUserByName(friend);
		if (receiver == null)
			throw new UserNotOnlineException();

		RequestMessage req = RequestMessage.BuildOpenConnection(sender, friend);
		try {
			// per la size
			long messageSize;
			// prendo il socket del receiver, e gli invio il messaggio di
			// richiesta di tipo OpenP2PConnection
			Socket clientSocket = receiver.getMessageSocket();
			// server aspettera' risposta per massimo 3 secondi
			clientSocket.setSoTimeout(3000);
			this.messageHandler.sendMessage(clientSocket, req);
			// inviato la richiesta con successo, aspetto la risposta del client
			InputStreamReader in = new InputStreamReader(clientSocket.getInputStream());
			BufferedReader bufferin = new BufferedReader(in);

			// leggo per prima cosa la size del messaggio
			String size = bufferin.readLine();
			if (size == null) {
				// socket disconnesso, la disconnessione vera e propria sara'
				// compito del worker
				throw new UserNotOnlineException();
			}

			// parso la size
			messageSize = Long.parseLong(size);

			// ottengo il messaggio vero e proprio
			String strMessage = bufferin.readLine();
			if (messageSize != strMessage.length())
				System.out.println("size del messaggio e dichiarata non corrispondono");
			// ignoro questo errore e controllo se comunque il messaggio avesse
			// senso

			Message message = this.messageHandler.JSONString2Message(strMessage);
			// adesso ho il messaggio, controllo che sia un ack
			if (message.getType() == Message.RESPONSE) {
				ResponseMessage ack = (ResponseMessage) message;
				ResponseMessage toReturn = ResponseMessage.BuildConnectionP2PAck(Message.SERVERNAME, sender,
						RequestCode.FILE2FRIEND, ack.getPeerAddress(), ack.getPeerPort());
				// ho l'indirizzo adesso, devo costruire l'ack e inviarlo
				return toReturn;
			} else // messaggio non di risposta, non so cosa mi abbia mandato
				throw new MalformedMessageException();

		} catch (IOException e) {
			// problemi nella connessione con il receiver
			throw new UserNotOnlineException();
		} catch (NotAFieldException e) {
			// il client non mi ha risposto con un ack, chissa' con cosa mi ha
			// risposto (lanciata da ack.getAddress())
			throw new MalformedMessageException();
		}
	}

	/**
	 * Metodo che gestisce l'invio di un messaggio sul gruppo multicast relavito
	 * alla chatroom richiesta. Tale invio non viene eseguito se l'unico utente
	 * online appartenente alla chat e' il sender stesso
	 * 
	 * @param message
	 *            il messaggio da inviare sulla chatroom
	 * @throws NoSuchChatException
	 *             se la chatroom richiesta non esiste
	 * @throws UnknownUserException
	 *             se l'utente mittente non esistesse
	 * @throws NoOneOnlineException
	 *             se l'unica persona online e' il mittente
	 * @throws IOException
	 *             in caso di errore di I/O
	 * @throws MalformedMessageException
	 *             se il messaggio di testo e' fatto male
	 */
	public void SendMessageToChatroom(TextMessage message) throws NoSuchChatException, UnknownUserException,
			NoOneOnlineException, IOException, MalformedMessageException {

		// controllo che il parametro non sia null
		if (message == null)
			throw new NullPointerException();

		// lancia MalformedMessage se il messaggio di testo fosse errato
		String text = this.messageHandler.Message2JSONObject(message).toJSONString();

		// ottengo i riferimenti al nome della chat, al nome del mittente e alla
		// chatroom
		String chatName = message.getReceiver();
		String sender = message.getSender();
		ChatRoom chat = this.findChatroomByName(chatName);

		// non esiste la chatroom richiesta
		if (chat == null)
			throw new NoSuchChatException();

		// mittente non fa parte della chatroom
		if (!chat.isInChatroom(sender))
			throw new UnknownUserException();

		// ottengo l'elenco degli utenti della chatroom
		String[] party = chat.getUserList();
		// variabile che mi serve per controllare se l'unico utente online
		// e' il mittente
		boolean noOneOnline = true;

		// ottengo il messaggio come stringa dal TextMessage

		byte[] data = text.getBytes();
		// preparo il pacchetto da spedire
		// indirizzo della chatroom e porta nota ChatRoom.MULTICAST_PORT
		DatagramPacket datagram = new DatagramPacket(data, data.length, chat.getAddress(), ChatRoom.MULTICAST_PORT);

		// verifico se almeno un membro e' online
		for (String member : party) {
			// verifico che non sia il mittente
			if (!member.equals(sender)) {
				// utente trovato non e' il mittente
				OnlineUser online = this.findOnlineUserByName(member);
				if (online != null) {
					// ho trovato almeno un utente diverso dal mittente online
					noOneOnline = false;
					// posso proseguire con l'invio
					break;
				}
			}
		}

		// devo controllare se ho trovato almeno un utente online adesso
		if (noOneOnline) // non ho trovato alcun utente online
			throw new NoOneOnlineException();
		// ho trovato almeno un utente online, invio il datagram

		this.datagramSocket.send(datagram);
		System.out.println("SPEDITO MESSAGGIO AL GRUPPO! "+text);
	}

	/* FINE CHAT OPS, INIZIO CHATROOM OPS */

	/**
	 * Gestione di una richiesta di tipo "creazione di una chatroom".
	 * Specificati il nome della chatroom da creare ed il nome utente del
	 * creatore di tale chatroom, questo metodo verifica se e' presente un'altra
	 * chatroom con lo stesso nome. In caso negativo, crea una nuova chatroom,
	 * unisce a tale chatroom il creator, e ritorna l'indirizzo multicast di
	 * tale chatroom
	 * 
	 * @param chatName
	 *            il nome della chat che si vuole creare
	 * @param creator
	 *            il nome dell'utente che ha creato la chat
	 * @return l'indirizzo della chat creata
	 * @throws NameAlreadyInUseException
	 *             se il nome della chat e' gia' preso
	 * @throws UnknownUserException
	 *             se l'utente creatore non esiste
	 */
	public InetAddress NewChatroom(String chatName, String creator)
			throws NameAlreadyInUseException, UnknownUserException {
		// controllo che il parametro non sia null
		if (chatName == null || creator == null)
			throw new NullPointerException();

		// se l'utente non esiste non posso aggiungerlo ad una chatroom
		if (!this.graph.isUser(creator))
			throw new UnknownUserException();

		// vedo se esistesse gia' una chatroom con questo nome
		ChatRoom chatRoom = this.findChatroomByName(chatName);
		if (chatRoom != null) // chatroom gia' esistente
			throw new NameAlreadyInUseException();
		// chatroom non esistente, la creo
		chatRoom = new ChatRoom(chatName, creator);
		this.chatrooms.add(chatRoom);
		return chatRoom.getAddress();
	}

	/**
	 * Gestisce la richiesta di tipo "Aggiungi utente a una chatroom".
	 * Restituisce l'indirizzo multicast della chatroom a cui vuol essere unito
	 * 
	 * @param user
	 *            il nome dell'utente che si vuole unire alla chatroom
	 * @param chatroom
	 *            il nome della chatroom a cui si vuole unire
	 * @return l'indirizzo multicast della chatroom a cui si e' unito
	 * @throws UnknownUserException
	 *             se l'utente non esistesse
	 * @throws NoSuchChatException
	 *             se la chatroom non esistesse
	 * @throws AlreadyInChatroomException
	 *             se l'utente fosse gia' nella chatroom
	 */
	public InetAddress AddUserToChatroom(String user, String chatroom)
			throws UnknownUserException, NoSuchChatException, AlreadyInChatroomException {
		// controllo che i parametri non siano null
		if (user == null || chatroom == null)
			throw new NullPointerException();

		// controllo che l'utente esista
		if (!this.graph.isUser(user))
			throw new UnknownUserException();

		// controllo che una chatroom con quel nome esista e ne
		// ottengo il riferimento
		ChatRoom chat = this.findChatroomByName(chatroom);
		if (chat == null)
			throw new NoSuchChatException();

		// aggiungo l'utente a tale chatroom, se tale utente vi fosse
		// gia' presente lancia AlreadyInChatroom
		chat.addUser(user);

		// restituisco l'indirizzo
		return chat.getAddress();
	}

	/**
	 * Restituisce la lista delle chatroom attive fino a questo momento. Il
	 * contenuto della lista e' del tipo: "ChatRoomName/Y" se l'utente
	 * e'registrato alla chatroom; "ChatRoomName/N" se l'utente non e'
	 * registrato a tale chatroom
	 * 
	 * @param user
	 *            il nome dell'utente che fa fatto richiesta
	 * @return la lista dei nomi delle chatroom presenti con indicazione sulla
	 *         presenza o meno dell'utente nella chatroom
	 * @throws UnknownUserException
	 *             se l'utente che ha fatto richiesta non esiste
	 */
	public String[] GetChatRoomList(String user) throws UnknownUserException {
		// controllo che il parametro non sia null
		if (user == null)
			throw new NullPointerException();

		// controllo se l'utente richiedente esiste
		if (!this.graph.isUser(user))
			throw new UnknownUserException();

		// mi preparo un arraylist in cui metto tutti i nomi delle chatroom
		ArrayList<String> toReturn = new ArrayList<>();

		// per ogni chatroom
		for (ChatRoom chat : this.chatrooms) {
			if (chat.isInChatroom(user)) // utente nella chatroom
				toReturn.add(chat.getChatName() + "/Y");
			else // utente non nella chatroom
				toReturn.add(chat.getChatName() + "/N");
		}

		return toReturn.toArray(new String[0]);
	}

	/**
	 * Elimina una chatroom se l'utente che ha fatto richiesta e' un utente
	 * registrato a tale chatroom
	 * 
	 * @param user
	 *            l'utente che fa la richiesta
	 * @param chatroom
	 *            il nome della chatroom da eliminare
	 * @throws UnknownUserException
	 *             se l'utente non e' registrato alla chatroom
	 * @throws NoSuchChatException
	 *             se la chat non esiste
	 */
	public void CloseChatRoom(String user, String chatroom) throws UnknownUserException, NoSuchChatException {
		// controllo che i parametri non siano null
		if (chatroom == null || user == null)
			throw new NullPointerException();

		// ottengo un riferimento alla chatroom
		ChatRoom chat = this.findChatroomByName(chatroom);
		if (chat == null) // non esiste tale chatroom
			throw new NoSuchChatException();

		// controllo che l'utente sia effettivamente nella chatroom per
		// proseguire con l'eliminazione
		if (chat.isInChatroom(user)) // era nella chatroom
			this.chatrooms.remove(chat);
		else // non era nella chatroom
			throw new UnknownUserException();
	}

}
