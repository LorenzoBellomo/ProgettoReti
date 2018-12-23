
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import condivise.Message;
import condivise.MessageHandler;
import condivise.Notifier;
import condivise.RequestCode;
import condivise.RequestMessage;
import condivise.ResponseCode;
import condivise.ResponseMessage;
import condivise.TextMessage;
import exceptions.*;

/**
 * Classe che implementa il task da passare alla thread pool per essere
 * eseguito. Il job consistera' nell'estrarre dalla coda condivisa taskQueue il
 * task (vedi classe WorkerTask) relativo ad un client(se presente) per poi
 * eseguire la richiesta del client; al termine il socket relativo verra'
 * reinserito nella coda.
 * 
 * In caso di task di tipo "ACCEPT", significa che il server ha accettato
 * soltanto la connessione di controllo del client, ma non ha ancora aperto la
 * connessione per i messaggi. Il client quindi invia nel socket di controllo la
 * porta su cui aprira' una connessione, ed il server allora
 * 
 * In caso il socket venga chiuso dal client, questo non verra' reinserito nella
 * coda(connessione terminata), mentre in caso il client non abbia alcuna
 * richiesta da eseguire, ossia la read sul socket ritorna 0, reinseriremo il
 * socket nella coda e continueremo il ciclo del worker thread.
 * 
 * Il ciclo del worker si sviluppa nel seguente modo: estratto il socket di un
 * client, leggiamo la size del messaggio che il client ci vuole inviare e
 * successivamente il messaggio vero e proprio; quest'ultimo corrispondera' ad
 * una stringa nel formato JSON, che verra' parsata in un oggetto Message grazie
 * alla classe JSONHandler. Dopo di che procederemo ad identificare il tipo di
 * messaggio(Request, Response o Text) e svolgeremo l'operazione richiesta a
 * seconda del tipo di richiesta. Al termine di ogni richiesta il thread
 * risponde al mittente con un messaggio appropriato (di conferma o di errore).
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class Worker implements Runnable {

	// rappresentazione della rete di utenti di SocialGraph
	private SocialGraph graph;

	// lista degli utenti online con associazione Username-Stub utente
	private Vector<OnlineUser> onlineUsers;

	// lista delle chatroom attive
	private Vector<ChatRoom> chatrooms;

	// coda da cui ogni worker estrarra' il task da eseguire
	private BlockingQueue<WorkerTask> taskQueue;

	// socket estratto dalla coda, conterra' il socket per i messaggi di
	// controllo del client
	private Socket clientSocket;

	// socket che invece conterra' i socket dei messaggi del client
	private Socket messageSocket;

	// parametro di setSoTimeout, utilizzato per effettuare la read sul socket
	// di un canale per un tempo limitato, cosi da non bloccare il thread in
	// caso il client non abbia alcuna richiesta
	private int readTimeout = 50;

	// l'handler delle richieste. Vedi classe RequestHandler
	private RequestHandler handler;

	public Worker(Vector<OnlineUser> list, SocialGraph g, BlockingQueue<WorkerTask> squeue, Vector<ChatRoom> chatrooms,
			DatagramSocket chatSock) {
		// COSTRUTTORE
		onlineUsers = list;
		graph = g;
		taskQueue = squeue;
		this.chatrooms = chatrooms;
		handler = new RequestHandler(graph, onlineUsers, this.chatrooms, chatSock);
	}

	/**
	 * Funzione di utilita' privata, chiude i socket di un utente e notifica
	 * tutti i suoi amici online che e' passato offline
	 * 
	 * @param u
	 *            utente da disconnettere
	 */
	private void disconnectUserAndNotifyFriends(OnlineUser u) {

		// devo notificare gli amici
		try {
			// ottengo l'utente
			User user = this.graph.getUser(u.getUsername());
			// ne ottengo gli amici
			String friends[] = this.graph.getFriends(user.getUsername());
			for (String friend : friends) {
				// controllo se tale amico e' online
				OnlineUser onlineFriend = this.findOnlineUserByName(friend);
				if (onlineFriend != null) // e' online
					onlineFriend.getStub().NotifyOfflineFriend(user.getUsername());
			}
		} catch (UnknownUserException e) {
			// ??
			return;
		} catch (RemoteException e) {
			// ignore
		}

	}

	/**
	 * Funzione di utilita' privata, cerca un utente fra quelli online. Se lo
	 * trova lo ritorna altrimenti ritorna null
	 * 
	 * @param nickname
	 *            l'utente che cerco online
	 * @return l'OnlineUser, null se esso non esiste
	 */
	private OnlineUser findOnlineUserByName(String nickname) {
		for (OnlineUser online : this.onlineUsers) {
			if (online.getUsername().equals(nickname))
				return online;
		}
		return null;
	}

	public void run() {
		// inizializzo il worker, mi preparo un handler della relazione
		// JSON<->Message
		MessageHandler messageHandler = new MessageHandler();

		// ciclo eseguito per l'intero periodo di vita del server
		while (!SocialGossipServer.stop) {
			// alloco un buffer per la lettura
			WorkerTask task;
			try {
				// prendo un task dalla coda
				task = taskQueue.take();
			} catch (InterruptedException e) {
				// nella take dalla coda, aspetto ancora risposte
				continue;
			}

			// in task ho i socket di controllo ed eventualmente quello
			// dei messaggi del client che devo servire, adesso devo verificare
			// se e' un task di accettazione, e quindi devo aprire la
			// connessione per i messaggi, oppure un task di servizio normale
			if (task.getType() == WorkerTask.ACCEPT) {
				// l'unico socket valido e' quello di controllo: devo aprire
				// la connessione dei messaggi
				// il client in teoria mi ha inviato la porta su cui aspetta
				// una connessione
				clientSocket = task.getControlSocket();
				try {
					clientSocket.setSoTimeout(1000);
					InputStreamReader in = new InputStreamReader(clientSocket.getInputStream());
					BufferedReader bufferin = new BufferedReader(in);
					// mi appresto a leggere la porta del client
					String s = null;
					try {
						s = bufferin.readLine();
					} catch (SocketTimeoutException e) {
						// il client non mi ha inviato la porta
						clientSocket.close();
						continue;
					}
					// parsing della porta
					int port = 0;
					try {
						port = Integer.parseInt(s);
					} catch (NumberFormatException e) {
						System.out.println("Errore con " + s);
						clientSocket.close();
						continue;
					}
					// adesso in port ho la porta, apro una connessione per i
					// messaggi verso quella porta
					Socket messageSocket = new Socket(clientSocket.getInetAddress(), port);

					// adesso so per certo che clientSocket di controllo e
					// messageSocket sono relativi allo stesso client: creo un
					// task di servizio con questi due socket e tengo traccia
					// di questa corrispondenza
					clientSocket.setSoTimeout(readTimeout);
					this.taskQueue.add(new WorkerTask(WorkerTask.SERVE, clientSocket, messageSocket));

				} catch (IOException e) {
					continue;
				}
			} else {
				String request = null;
				Notifier stub = null;
				try {
					// worker task di tipo SERVE
					// sono valide entrambe le connessioni
					// devo vedere se il client ha una richiesta ed
					// eventualmente servirla
					clientSocket = task.getControlSocket();
					messageSocket = task.getMessageSocket();

					// le richieste arriveranno in un ObjectStream
					ObjectInputStream stubStream = new ObjectInputStream(clientSocket.getInputStream());
					try {
						// prima mi arriva la size
						int size = stubStream.readInt();
						// poi la richiesta
						request = (String) stubStream.readObject();
						// se la lunghezza del messaggio non corrisponde allora
						// mi devo aspettare un Notifier
						if (size > request.length())
							stub = (Notifier) stubStream.readObject();
					} catch (ClassNotFoundException e) {
						System.out.println("Classe non trovata");
					} catch (EOFException e) {
						System.out.println("un client ha chiuso");
						// socket chiuso
						for (OnlineUser u : onlineUsers) {
							// controllo se il socket chiuso appartiene ad un
							// utente online
							if (u.getControlSocket() == clientSocket) {
								System.out.println(u.getUsername());
								// rimuovo l'utente da quelli online
								this.disconnectUserAndNotifyFriends(u);
								// trovato l'utente
								break;
							}
						}
						// chiudo le due connessioni
						try {
							clientSocket.close();
							messageSocket.close();
						} catch (IOException e1) {
						}
						// itero e passo alla prossima richiesta
						continue;

					}
				} catch (SocketTimeoutException e) {
					// il timeout sulla read e'scattato, non c'e' alcuna
					// richiesta sul socket del client. In questo momento
					// posso reinserire il socket nella coda per controllarlo
					// nuovamente in un secondo momento.
					// la dimensione della coda non puo' essere superata poiche'
					// e' controllata dal 'produttore'(Listener Thread)
					taskQueue.offer(task);
					continue;
				} catch (IOException e) {
					// eccezione inaspettata da un'operazione sul socket
					taskQueue.offer(task);
					continue;
				}

				// adesso in request ho un messaggio, che devo servire

				// ottengo cosi un oggetto messaggio corrispondente a quello
				// inviato dal client
				Message msg;
				try {
					msg = (Message) messageHandler.JSONString2Message(request);
				} catch (MalformedMessageException e) {
					// problema nel parsing del messaggio, ignoriamo la
					// richiesta
					taskQueue.offer(task);
					continue;
				}

				int msgtype = msg.getType();
				// adesso identifico il tipo di messaggio inviato dal client
				if (msgtype == Message.REQUEST) {
					// re-cast del messaggio al sottotipo appropriato
					RequestMessage reqMsg = (RequestMessage) msg;
					// messaggio che conterra' la risposta del server al client
					ResponseMessage reply = null;

					// se il messaggio ricevuto e' un RequestMessage, leggo il
					// tipo di richiesta ricevuto
					switch (reqMsg.TypeOfRequest()) {
					case REGISTER: {
						// gestisco la richiesta di registrazione
						try {
							// creo un oggetto OnlineUser, che sara' inserito
							// nella lista solo se il client si registra con
							// successo
							OnlineUser newUser = new OnlineUser(reqMsg.getSender(), stub, clientSocket, messageSocket);
							handler.RegisterUser(reqMsg.getSender(), reqMsg.getLanguage(), newUser);
							// richiesta di registrazione a buon fine, rispondo
							// con ack positivo
							reply = ResponseMessage.BuildAck(reqMsg.getSender(), RequestCode.REGISTER);
						} catch (NameAlreadyInUseException e) {
							// il nome utente che il client sta tentando di
							// usare e' gia' in uso rispondo con un appropriato
							// messaggio di errore
							reply = ResponseMessage.BuildError(ResponseCode.NICK_ALREADY_TAKEN, RequestCode.REGISTER,
									"");
						} catch (NotAFieldException e) {
							// il messaggio ricevuto non presenta uno o piu'
							// campi necessari ad eseguire la richiesta rispondo
							// con un messaggio di errore generico
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.REGISTER, "");
						}
						break;

					}
					case LOGIN: {
						// gestisco la richiesta di login dell'utente
						try {
							// creo un oggetto OnlineUser, che sara' inserito
							// nella lista solo se il client logga con successo
							OnlineUser newUser = new OnlineUser(reqMsg.getSender(), stub, clientSocket, messageSocket);
							handler.LogInUser(newUser);
							// richiesta a buon fine, genero un messaggio di ack
							// positivo
							reply = ResponseMessage.BuildAck(reqMsg.getSender(), RequestCode.LOGIN);
						} catch (UnknownUserException e) {
							// il nome utente inserito non e' presente fra
							// quelli registrati
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.LOGIN,
									reqMsg.getSender());
						}
						break;
					}
					case LOOKUP: {
						// gestisco la richiesta di lookup
						try {
							boolean isOnline;
							isOnline = handler.LookUpUser(reqMsg.getTarget());
							// user trovato
							reply = ResponseMessage.BuildOnlineAck(reqMsg.getSender(), RequestCode.LOOKUP, isOnline);
						} catch (UnknownUserException e) {
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.LOOKUP,
									reqMsg.getSender());
						} catch (NotAFieldException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.LOOKUP,
									reqMsg.getSender());
						}
						break;
					}
					case FRIENDSHIP: {
						// gestisco la richiesta di aggiunta di un amico
						try {
							// tento di aggiungere la nuova relazione di
							// amicizia
							boolean isOnline = handler.NewFriendship(reqMsg.getSender(), reqMsg.getTarget());
							// amicizia aggiunta correttamente
							reply = ResponseMessage.BuildOnlineAck(reqMsg.getSender(), RequestCode.FRIENDSHIP,
									isOnline);
						} catch (UnknownUserException e) {
							// uno dei due utenti non esiste
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.FRIENDSHIP,
									reqMsg.getSender());
						} catch (AlreadyAFriendException e) {
							// la relazione di amicizia e' gia' presente
							reply = ResponseMessage.BuildError(ResponseCode.ALREADY_A_FRIEND, RequestCode.FRIENDSHIP,
									reqMsg.getSender());
						} catch (NotAFieldException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.FRIENDSHIP,
									reqMsg.getSender());
						}
						break;
					}
					case FRIEND_LIST: {
						// gestisco la richiesta dell'invio della lista di amici
						try {
							String friends[] = handler.ListFriends(reqMsg.getSender());
							// operazione eseguita, preparo il messaggio da
							// inviare indietro all'utente, contente la lista
							// dei suoi amici
							reply = ResponseMessage.BuildListAck(reqMsg.getSender(), RequestCode.FRIEND_LIST, friends);

						} catch (UnknownUserException e) {
							// utente sconosciuto
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.FRIEND_LIST,
									reqMsg.getSender());
						}
						break;
					}
					case FILE2FRIEND: {
						// gestisco la richiesta dell'invio di un file ad un
						// amico
						try {
							// il metodo mi restituisce l'ack da inviare
							reply = handler.SendFileToFriend(reqMsg.getSender(), reqMsg.getTarget());
						} catch (UserNotOnlineException e) {
							// destinatario offline
							reply = ResponseMessage.BuildError(ResponseCode.USER_OFFLINE, RequestCode.FILE2FRIEND,
									reqMsg.getSender());
						} catch (NoSuchFriendException e) {
							// il destinatario non e' amico del mittente
							reply = ResponseMessage.BuildError(ResponseCode.NOT_A_FRIEND, RequestCode.FILE2FRIEND,
									reqMsg.getSender());
						} catch (UnknownUserException e) {
							// utente sconosciuto
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.FILE2FRIEND,
									reqMsg.getSender());
						} catch (MalformedMessageException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.FILE2FRIEND,
									reqMsg.getSender());
						} catch (NotAFieldException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.FILE2FRIEND,
									reqMsg.getSender());
						}
						break;
					}
					case MSG2FRIEND: {
						// gestione della richiesta di invio messaggio
						TextMessage txtMsg = null;

						// provo ad inviare il messaggio al destinatario
						reply = null;
						try {
							txtMsg = reqMsg.getTextMessage();
							handler.SendMessageToFriend(txtMsg);
							// operazione a buon fine
							reply = ResponseMessage.BuildAck(txtMsg.getSender(), RequestCode.MSG2FRIEND);
						} catch (UnknownUserException e) {
							// uno dei due utenti non esiste
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.MSG2FRIEND,
									txtMsg.getSender());
						} catch (NoSuchFriendException e) {
							// i due utenti non sono amici
							reply = ResponseMessage.BuildError(ResponseCode.NOT_A_FRIEND, RequestCode.MSG2FRIEND,
									txtMsg.getSender());
						} catch (UserNotOnlineException e) {
							// l'utente con cui vogliamo chattare non e' online
							reply = ResponseMessage.BuildError(ResponseCode.USER_OFFLINE, RequestCode.MSG2FRIEND,
									txtMsg.getSender());
						} catch (MalformedMessageException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.MSG2FRIEND,
									txtMsg.getSender());
						} catch (NotAFieldException e) {
							// l'utente mi ha inviato una richiesta mal formata
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.MSG2FRIEND,
									msg.getSender());
						}
						break;
					}
					case CHATROOM_MSG: {
						// i messaggi destinati ad una chatroom devono essere
						// inviati con UDP
						try {
							// ottengo il messaggio testuale da inviare, fra i
							// suoi campi esso conterra' il nome della chatroom
							// a cui inviare i messaggi
							TextMessage textMessage = reqMsg.getTextMessage();
							// invio il messaggio alla chatroom (puo' lanciare
							// eccezioni)
							handler.SendMessageToChatroom(textMessage);
							// invio andato a buon fine, creo un ack positivo
							reply = ResponseMessage.BuildAck(msg.getSender(), RequestCode.CHATROOM_MSG);
						} catch (NotAFieldException e) {
							// il campo textmessage non era valido nel messaggio
							// di richiesta, che non e' sensato per il server
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.CHATROOM_MSG,
									msg.getSender());
						} catch (NoSuchChatException e) {
							// se la chatroom richiesta e' inesistente
							reply = ResponseMessage.BuildError(ResponseCode.CHATROOM_UNKNOWN, RequestCode.CHATROOM_MSG,
									msg.getSender());
						} catch (UnknownUserException e) {
							// se l'utente che ha fatto richiesta non e' nella
							// chatroom
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.CHATROOM_MSG,
									msg.getSender());
						} catch (NoOneOnlineException e) {
							// se l'unico utente online della chatroom e' il
							// mittente
							reply = ResponseMessage.BuildError(ResponseCode.NO_ONE_ONLINE, RequestCode.CHATROOM_MSG,
									msg.getSender());
						} catch (IOException e) {
							// se avviene un errore nell'invio del datagram o
							// nell'apertura del socket
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.CHATROOM_MSG,
									msg.getSender());
							e.printStackTrace();
						} catch (MalformedMessageException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.CHATROOM_MSG,
									msg.getSender());
						}
						break;
					}
					case CREATE_CHATROOM: {
						// gestisco creazione di una chatroom
						try {
							// provo a creare una chatroom
							InetAddress address = handler.NewChatroom(reqMsg.getTarget(), reqMsg.getSender());
							// operazione eseguita con successo
							reply = ResponseMessage.BuildChatroomAck(reqMsg.getSender(), RequestCode.CREATE_CHATROOM,
									address);
						} catch (NameAlreadyInUseException e) {
							// nome della chatroom occupato
							reply = ResponseMessage.BuildError(ResponseCode.NICK_ALREADY_TAKEN,
									RequestCode.CREATE_CHATROOM, reqMsg.getSender());
						} catch (NotAFieldException e) {
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.CREATE_CHATROOM,
									reqMsg.getSender());
						} catch (UnknownUserException e) {
							// l'utente richiedente non esiste
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN,
									RequestCode.CREATE_CHATROOM, reqMsg.getSender());
						}
						break;
					}
					case ADD_TO_CHATROOM: {
						try {
							// gestisco l'aggiunta di un utente ad una chatroom
							InetAddress address = handler.AddUserToChatroom(reqMsg.getSender(), reqMsg.getTarget());
							// operazione eseguita con successo
							reply = ResponseMessage.BuildChatroomAck(reqMsg.getSender(), RequestCode.ADD_TO_CHATROOM,
									address);
						} catch (NotAFieldException e) {
							// il campo target non era significativo, chissa'
							// cosa mi e' stato inviato
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.ADD_TO_CHATROOM,
									reqMsg.getSender());
						} catch (UnknownUserException e) {
							// se l'utente che fa richiesta non esiste
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN,
									RequestCode.ADD_TO_CHATROOM, reqMsg.getSender());
						} catch (NoSuchChatException e) {
							// se la chat richiesta non esiste
							reply = ResponseMessage.BuildError(ResponseCode.CHATROOM_UNKNOWN,
									RequestCode.ADD_TO_CHATROOM, reqMsg.getSender());
						} catch (AlreadyInChatroomException e) {
							// se l'utente era gia' nella chatroom richiesta
							reply = ResponseMessage.BuildError(ResponseCode.ALREADY_IN_CHATROOM,
									RequestCode.ADD_TO_CHATROOM, reqMsg.getSender());
						}
						break;
					}
					case CHATROOM_LIST: {
						// gestisco l'invio della lista di chatroom attive
						try {
							String[] chatList = handler.GetChatRoomList(reqMsg.getSender());
							// operazione eseguita con successo
							reply = ResponseMessage.BuildListAck(reqMsg.getSender(), RequestCode.CHATROOM_LIST,
									chatList);
						} catch (UnknownUserException e) {
							// se l'utente che ha fatto richiesta non esiste
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.CHATROOM_LIST,
									reqMsg.getSender());
						}
						break;
					}
					case CLOSE_CHAT: {
						// gestisco la richiesta di chiusura di una chatroom
						try {
							// provo ad eliminare la chatroom
							handler.CloseChatRoom(reqMsg.getSender(), reqMsg.getTarget());
							// operazione eseguita con successo
							reply = ResponseMessage.BuildAck(reqMsg.getSender(), RequestCode.CLOSE_CHAT);
						} catch (UnknownUserException e) {
							// se l'utente non fosse esistente o parte della
							// chat
							reply = ResponseMessage.BuildError(ResponseCode.NICKNAME_UNKNOWN, RequestCode.CLOSE_CHAT,
									reqMsg.getSender());
						} catch (NoSuchChatException e) {
							// se la chatroom non esistesse
							reply = ResponseMessage.BuildError(ResponseCode.CHATROOM_UNKNOWN, RequestCode.CLOSE_CHAT,
									reqMsg.getSender());
						} catch (NotAFieldException e) {
							// se il campo target non fosse stato valido nella
							// richiesta
							reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, RequestCode.CLOSE_CHAT,
									reqMsg.getSender());
						}
						break;
					}
					case OPEN_P2PCONN: {
						// questa richiesta e' solo per i client, la ignoro
						break;
					}
					default: {
						// tipo di richiesta non valido
						reply = ResponseMessage.BuildError(ResponseCode.OP_FAIL, null, reqMsg.getSender());
					}
					}
					// finito lo switch sul tipo di richiesta

					try {
						// invio la risposta
						messageHandler.sendMessage(clientSocket, reply);
					} catch (Exception e) {
						// errore imprevisto nell'invio di un messaggio,
						// il socket verra' reinserito nella coda al termine del
						// ciclo
						e.printStackTrace();
						System.out.println("Errore nell'invio di un messaggio di risposta");
					}

				} else {
					// richiesta non accettata, le richieste di invio di
					// messaggio devono essere inviate con un RequestMessage, e
					// non con un TextMessage ed i response message devono
					// necessariamente viaggiare sull'altro canale
					System.out.println("Ottenuto un Messaggio non valido");
					// non mando alcun codice d'errore al client che ha inviato
					// mi limito ad ignorare la richiesta malformata
				}

				// reinserisco il socket del client nella queue condivisa; la
				// dimensione non puo' essere superata poiche' e'
				// controllata dal 'produttore' (Listener Thread)
				taskQueue.offer(task);
			}
		}
	}

}
