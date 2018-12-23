package condivise;

import java.io.BufferedWriter; 
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import exceptions.MalformedMessageException;
import exceptions.NoSuchCodeException;
import exceptions.NotAFieldException;

/**
 * Classe che si occupa di trasformare i messaggi in oggetti JSON come da
 * formato definito. Inoltre sempre questa classe esegue il processo inverso,
 * quindi quello di ricostruire un messaggio a partire da un messaggio JSON
 * Inoltre contiene una funzione che invia un messaggio su un determinato socket
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class MessageHandler {

	// definiscono le chiavi con cui ottenere i campi corrispettivi nel
	// JSONObject
	public final String TYPE = "Type";
	public final String SENDER = "Sender";
	public final String RECEIVER = "Receiver";
	public final String RESPCODE = "RespCode";
	public final String REQCODE = "ReqCode";
	public final String TEXTMESSAGE = "textMessage";
	public final String MESSAGE = "Message";
	public final String CHATROOM = "ChatName";
	public final String TARGET = "TargetUser";
	public final String LIST = "List";
	public final String ADDRESS = "Address";
	public final String PORT = "Port";
	public final String LANGUAGE = "Language";
	public final String STUB = "Stub";
	public final String ISONLINE = "IsOnline";

	// il parser per le operazioni di parsing di stringhe JSON
	JSONParser parser;

	public MessageHandler() {
		// COSTRUTTORE
		this.parser = new JSONParser();
	}

	// Per evitare warning sulle put su JSONObject, per via del fatto che
	// JSONObject di json.simple non supporta i generics
	@SuppressWarnings("unchecked")

	/**
	 * Metodo che dato un messaggio di tipo Richiesta/Risposta/Testo, crea il
	 * corrispondente JSONObject il cui formato standard e' definito dalla
	 * classe
	 * 
	 * @param message
	 *            il messaggio di cui voglio il JSONObject
	 * @return Il JSONobject corrispondente
	 * @throws NotAFieldException
	 *             Se il messaggio passato come parametro e' malformato
	 * @throws NullPointerException
	 *             se il messaggio passato come parametro e' null
	 * @throws UnsupportedOperationException
	 *             se il messaggio non e' di tipo riconosciuto
	 */
	public JSONObject Message2JSONObject(Message message) throws MalformedMessageException {

		// controllo che il parametro non sia null
		if (message == null)
			throw new NullPointerException();

		// il JSONObject che ritornero' a fine processo
		JSONObject msgJSON = new JSONObject();
		// inserisco alcuni campi che sicuramente dovro' mettere
		msgJSON.put(this.TYPE, message.getType()); // richiesta/risposta/testo
		msgJSON.put(this.SENDER, message.getSender());
		msgJSON.put(this.RECEIVER, message.getReceiver());
		try {
			// adesso devo controllare il tipo del messaggio per riempire i vari
			// campi
			switch (message.getType()) {
			case (Message.REQUEST): {
				// messaggio di richiesta, faccio un cast per ottenere un
				// oggetto di tipo RequestMessage
				RequestMessage msg = (RequestMessage) message;
				// sicuramente dovro' inserire il codice numerico di richiesta
				msgJSON.put(this.REQCODE, msg.TypeOfRequest().getCode());
				// adesso a seconda della richiesta, devo fare:
				switch (msg.TypeOfRequest()) {
				// aggiungo un campo per il messaggio, dopo di che non faccio
				// break, quindi aggiungo anche la chat su cui inviare il
				// messaggio
				case CHATROOM_MSG:
					JSONObject textMess = this.Message2JSONObject(msg.getTextMessage());
					msgJSON.put(this.TEXTMESSAGE, textMess.toJSONString());
				case CLOSE_CHAT:
				case CREATE_CHATROOM:
				case ADD_TO_CHATROOM:
					// in tutti questi casi precedenti ho una chatroom da
					// aggiungere
					msgJSON.put(this.CHATROOM, msg.getTarget());
					break;
				// finiti i casi di chatroom, adesso prima il messaggio testuale
				// ad un amico, per cui devo aggiungere messaggio testuale e
				// receiver
				case MSG2FRIEND:
					msgJSON.put(this.TEXTMESSAGE, this.Message2JSONObject(msg.getTextMessage()).toJSONString());
				case FILE2FRIEND:
				case FRIENDSHIP:
				case LOOKUP:
				case OPEN_P2PCONN:
					msgJSON.put(this.TARGET, msg.getTarget());
					break;
				// register e' l'unico con il campo lingua
				case REGISTER:
					msgJSON.put(this.LANGUAGE, msg.getLanguage());
					break;
				// tutti le seguenti richieste non necessitano di alcun nuovo
				// campo
				case LOGIN:
				case FRIEND_LIST:
				case CHATROOM_LIST:
					break;
				default:
					throw new MalformedMessageException();
				}
				break;
			}
			case (Message.RESPONSE): {
				// messaggio di risposta, faccio un cast per ottenere un oggetto
				// di tipo ResponseMessage
				ResponseMessage msg = (ResponseMessage) message;
				// sicuramente devo aggiungere il codice di risposta e quello di
				// richiesta corrispondente
				msgJSON.put(this.RESPCODE, msg.TypeOfResponse().getCode());
				msgJSON.put(this.REQCODE, msg.TypeOfRequest().getCode());
				// adesso se la risposta e' positiva potrei dover aggiungere un
				// campo
				if (msg.TypeOfResponse() == ResponseCode.OP_OK) {
					if (msg.TypeOfRequest() == RequestCode.CHATROOM_LIST
							|| msg.TypeOfRequest() == RequestCode.FRIEND_LIST) {
						// messaggio di listing, devo aggiungere un JSONArray
						// relativo alle stringhe di nomi di amici/chatroom
						JSONArray list = new JSONArray();
						for (String s : msg.getList()) {
							list.add(s);
						}
						msgJSON.put(this.LIST, list);
					} else if (msg.TypeOfRequest() == RequestCode.OPEN_P2PCONN
							|| msg.TypeOfRequest() == RequestCode.FILE2FRIEND) {
						// richiesta di scambio di file, devo comunicare
						// il'indirizzo in cui il client sta aspettando una
						// connessione
						String addr = msg.getPeerAddress().toString();
						// il to string dell'inetAddress mette l'indirizzo
						// in una stringa del tipo "NomeHost/IP", a me
						// interessa solo l'IP
						String[] realaddr = addr.split("/");
						msgJSON.put(this.ADDRESS, realaddr[realaddr.length - 1]);
						msgJSON.put(this.PORT, msg.getPeerPort());
					} else if (msg.TypeOfRequest() == RequestCode.FRIENDSHIP
							|| msg.TypeOfRequest() == RequestCode.LOOKUP) {
						// devo aggiungere lo stato (online o offline) dell'utente
						msgJSON.put(this.ISONLINE, msg.isOnline());
					} else if (msg.TypeOfRequest() == RequestCode.CREATE_CHATROOM
							|| msg.TypeOfRequest() == RequestCode.ADD_TO_CHATROOM) {
						// devo aggiungere l'inetaddress della chatroom
						String addr = msg.getChatroomAddress().toString();
						// il to string dell'inetAddress mette l'indirizzo
						// in una stringa del tipo "NomeHost/IP", a me
						// interessa solo l'IP
						String[] realaddr = addr.split("/");
						msgJSON.put(this.ADDRESS, realaddr[realaddr.length - 1]);
					}
				}
				break;
			}
			case (Message.TEXT): {
				// messaggio di testo, faccio un cast per ottenere un oggetto
				// di tipo TextMessage
				TextMessage msg = (TextMessage) message;
				// devo solo aggiungere il testo del messaggio
				msgJSON.put(this.MESSAGE, msg.getMessage());
				break;
			}
			default:
				// non so bene cosa mi abbia passato come parametro
				throw new UnsupportedOperationException();
			}
		} catch (NotAFieldException e) {
			// Il messaggio era fatto male
			throw new MalformedMessageException();
		}
		// fine dello switch, adesso in msgJSON ho il JSONObject nel formato
		// definito
		return msgJSON;
	}

	/**
	 * Metodo che data una stringa in formato JSON, ricostruisce il messaggio
	 * corrispondente.
	 * 
	 * @param stringJSON
	 *            La stringa di cui voglio fare parsing
	 * @return Il messaggio generato
	 * @throws MalformedMessageException
	 *             Se il messaggio non era ben strutturato
	 * @throws NullPointerException
	 *             se la stringa e' null
	 */
	public Message JSONString2Message(String stringJSON) throws MalformedMessageException {

		// controllo la correttezza del parametro
		if (stringJSON == null)
			throw new NullPointerException();
		// msg e' la variabile che ritornero'
		Message msg = null;
		// obj e' il JSON object su cui lavorero' e recuperero' i campi
		JSONObject obj = null;
		try {
			obj = (JSONObject) this.parser.parse(stringJSON);
		} catch (ParseException e) {
			// se fallisce il parsing allora la stringa non era valida
			throw new MalformedMessageException();
		}

		// recupero alcuni campi che in ogni caso dovro' utilizzare
		int msgType = ((Long) obj.get(this.TYPE)).intValue();
		String sender = (String) obj.get(this.SENDER);
		String receiver = (String) obj.get(this.RECEIVER);

		// adesso lavoro diversamente se e' richiesta, risposta o testo
		switch (msgType) {
		case (Message.REQUEST): {
			// MESSAGGIO DI RICHIESTA
			// scopro quale era la richiesta
			int reqCode = ((Long) obj.get(this.REQCODE)).intValue();
			RequestCode request = null;
			try {
				// ottengo dal codice numerico la richiesta effettiva
				request = RequestCode.DescriptionOf(reqCode);
			} catch (NoSuchCodeException e) {
				throw new MalformedMessageException();
			}
			// adesso ottengo tutti i campi che potrebbero servirmi, usero' nei
			// prossimi passi solo quelli utili, gli altri probabilmente saranno
			// null ma poco importa
			String chatName = (String) obj.get(this.CHATROOM);
			String textJSON = (String) obj.get(this.TEXTMESSAGE);
			String target = (String) obj.get(this.TARGET);
			String language = (String) obj.get(this.LANGUAGE);
			// se ho un messaggio di testo all'interno devo fare il parsing pure
			// di esso, la gestione dei messaggi di testo e' in fondo a questo
			// metodo
			TextMessage textMessage = null;
			if (textJSON != null)
				textMessage = (TextMessage) JSONString2Message(textJSON);
			// adesso costruisco i messaggi diversi a seconda del tipo di
			// richiesta con i relativi metodi statici
			switch (request) {
			case ADD_TO_CHATROOM:
				msg = RequestMessage.BuildJoinChatroom(sender, chatName);
				break;
			case CHATROOM_LIST:
				msg = RequestMessage.BuildChatroomList(sender);
				break;
			case CHATROOM_MSG:
				msg = RequestMessage.BuildMessageToChatroom(sender, chatName, textMessage);
				break;
			case CLOSE_CHAT:
				msg = RequestMessage.BuildDeleteChatroom(sender, chatName);
				break;
			case CREATE_CHATROOM:
				msg = RequestMessage.BuildCreateChatroom(sender, chatName);
				break;
			case FILE2FRIEND:
				msg = RequestMessage.BuildFileToFriend(sender, target);
				break;
			case FRIENDSHIP:
				msg = RequestMessage.BuildFriendship(sender, target);
				break;
			case FRIEND_LIST:
				msg = RequestMessage.BuildFriendList(sender);
				break;
			case LOGIN:
				msg = RequestMessage.BuildLogin(sender);
				break;
			case LOOKUP:
				msg = RequestMessage.BuildLookUp(sender, target);
				break;
			case MSG2FRIEND:
				msg = RequestMessage.BuildMessageToFriend(sender, target, textMessage);
				break;
			case OPEN_P2PCONN:
				msg = RequestMessage.BuildOpenConnection(target, receiver);
				break;
			case REGISTER:
				msg = RequestMessage.BuildRegister(sender, language);
				break;
			default: // non dovrei mai arrivare qua
				throw new MalformedMessageException();
			}
			// fine gestione richiesta
			break;
		}
		case (Message.RESPONSE): {
			// MESSAGGI DI RISPOSTA
			// ottengo alcune cose che mi saranno sicuramente utili
			int respCode = ((Long) obj.get(this.RESPCODE)).intValue();
			int reqCode = ((Long) obj.get(this.REQCODE)).intValue();
			RequestCode request = null;
			ResponseCode response = null;
			try {
				// ricavo sia richiesta che risposta dai relativi codici
				request = RequestCode.DescriptionOf(reqCode);
				response = ResponseCode.DescriptionOf(respCode);
			} catch (NoSuchCodeException e) {
				throw new MalformedMessageException();
			}
			// 2 casi: ACK positivo o negativo
			if (response == ResponseCode.OP_OK) {
				// ACK POSITIVO
				// 5 sottocasi: ack per lista, ack per open conn, ack online,
				// ack per chatroom ed ack generico
				if (request == RequestCode.CHATROOM_LIST || request == RequestCode.FRIEND_LIST) {
					// ack per lista, devo ottenere la lista di nomi
					JSONArray listJSON = (JSONArray) obj.get(this.LIST);
					String[] list = new String[listJSON.size()];
					// per ogni elemento nel JSONArray, prendo la sua
					// controparte String
					for (int i = 0; i < listJSON.size(); i++)
						list[i] = (String) listJSON.get(i);
					// adesso creo l'ack vero e proprio col metoto statico
					msg = ResponseMessage.BuildListAck(receiver, request, list);
				} else if (request == RequestCode.OPEN_P2PCONN || request == RequestCode.FILE2FRIEND) {
					// ack per apertura connessione P2P
					// ottengo l'indirizzo del socket del secondo client
					String addr = (String) obj.get(this.ADDRESS);
					InetAddress address;
					try {
						address = InetAddress.getByName(addr);
					} catch (UnknownHostException e) {
						e.printStackTrace();
						throw new MalformedMessageException();
					}
					int port = ((Long) obj.get(this.PORT)).intValue();
					// creo il messaggio con il metodo statico
					msg = ResponseMessage.BuildConnectionP2PAck(sender, receiver, request, address, port);
				} else if (request == RequestCode.FRIENDSHIP || request == RequestCode.LOOKUP) {
					boolean isOnline = (boolean) obj.get(this.ISONLINE);
					msg = ResponseMessage.BuildOnlineAck(receiver, request, isOnline);
				} else if (request == RequestCode.ADD_TO_CHATROOM || request == RequestCode.CREATE_CHATROOM) {
					String addr = (String) obj.get(this.ADDRESS);
					InetAddress address;
					try {
						address = InetAddress.getByName(addr);
					} catch (UnknownHostException e) {
						e.printStackTrace();
						throw new MalformedMessageException();
					}
					msg = ResponseMessage.BuildChatroomAck(receiver, request, address);
					// ack generico, mi limito a creare il messaggio
				} else {
					msg = ResponseMessage.BuildAck(receiver, request);
				}
			} else {
				// NACK, errore nella richiesta
				// mi limito a creare un messaggio di errore
				msg = ResponseMessage.BuildError(response, request, receiver);
			}
			// fine gestione risposte
			break;
		}
		case (Message.TEXT): {
			// MESSAGGI DI TESTO
			// per questi messaggi basta ottenere la stringa del messaggio
			String text = (String) obj.get(this.MESSAGE);
			// e creare il messaggio
			msg = TextMessage.BuildTextMessage(sender, receiver, text);
			break;
		}
		default: // cosa mi e' stato passato?
			throw new MalformedMessageException();
		}

		// a questo punto dovrei aver inizializzato msg, per sicurezza verifico
		// che sia stato inizializzato, dopodiche' lo ritorno
		if (msg == null)
			throw new MalformedMessageException();
		;

		return msg;
	}

	/**
	 * Invia un messaggio ad un client individuato dal suo socket; la funzione
	 * si occupera' del parsing del tipo di messaggio, cosi come dell'invio
	 * utilizzando le funzioni di JAVA IO, ma non si occupera' del controllo dei
	 * campi del messaggio, che viene invece effettuato nella costruzione del
	 * messaggio stesso, prima della chiamata di questa funzione
	 * 
	 * @param clientSocke,
	 *            il socket del client a cui vogliamo inviare un messaggio
	 * @param m,
	 *            il messaggio da inviare
	 * @throws NullPointerException,
	 *             se uno dei parametri e' null
	 * @throws IOException,
	 *             se si verifica un errore inaspettato nell'estrazione del
	 *             OutputStream o durante la write
	 * @throws MalformedMessageException,
	 *             se il messaggio passato come parametro non contiene i campi
	 *             corretti per il suo tipo di messaggio, oppure si verifica un
	 *             errore imprevisto nella traduzione Messaggio->Json
	 */
	public void sendMessage(Socket clientSocket, Message m)
			throws NullPointerException, IOException, MalformedMessageException {
		if (clientSocket == null || m == null)
			throw new NullPointerException();

		// invio al client il messaggio
		OutputStreamWriter out;
		// prendo il writer dal socket e lo bufferizzo
		out = new OutputStreamWriter(clientSocket.getOutputStream());

		BufferedWriter buff = new BufferedWriter(out);

		// converto il messaggio nel formato inviabile, JSON
		JSONObject jsonMsg = Message2JSONObject(m);
		String jsonString = jsonMsg.toJSONString();

		// prima invio la size del messaggio di risposta
		long size = jsonString.length();

		buff.write(size + "\n");
		buff.flush();
		System.out.println("invio " + jsonString + " to " + clientSocket);
		buff.write(jsonString + "\n");
		buff.flush();
	}

}
