
import condivise.TextMessage;
import exceptions.NoSuchChatException;
import exceptions.NoSuchFriendException;
import exceptions.UnknownUserException;
import exceptions.UserNotOnlineException;


/**
 * ChatOps.java Interfaccia che contiene le segnature dei metodi che definscono
 * il comportamento delle operazioni eseguibili da un client per l'interazione
 * con altri client: invio messaggio ad un altro utente o ad un gruppo ed invio
 * di file.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public interface ChatOps {

	/**
	 * Indica al server la volonta' del client chiamante di inviare un file
	 * all'user con nickname passato come parametro
	 * 
	 * @param nickname,
	 *            il nickname dell'utente a cui si vuole inviare un file
	 * @throws UnknownUserException,
	 *             eccezione sollevata in caso il nickname specificato non
	 *             corrisponda a nessun utente registrato
	 * @throws UserNotOnlineException,
	 *             eccezione sollevata in caso il nickname specificato
	 *             corrisponda ad un utente attualmente offline
	 * @throws NoSuchFriendExceptio,
	 *             eccezione sollevata nel caso il nickname specificato non
	 *             corrisponda ad un amico del client mittente
	 */
	public void File2Friend(String nickname) throws UnknownUserException, UserNotOnlineException, NoSuchFriendException;

	/**
	 * Indica al server la volonta' di spedire un messaggio testuale all'amico
	 * individuato dal nickname, il server si occupera' di recapitare il
	 * messaggio al destinatario (traducendo il messaggio se necessario); il
	 * messaggio inviato viaggera' sulla rete in formato JSON
	 * 
	 * @param nickname,
	 *            il nickname dell'amico con cui si vuole chattare
	 * @param m,
	 *            il messaggio da inviare, al suo interno sara' specificato
	 *            anche il sender ed il messaggio testuale
	 * @throws UnknownUserException,
	 *             eccezione sollevata in caso il nickname specificato non
	 *             corrisponda a nessun utente registrato
	 * @throws UserNotOnlineException,
	 *             eccezione sollevata in caso il nickname specificato
	 *             corrisponda ad un utente attualmente offline
	 * @throws NoSuchFriendException,
	 *             eccezione sollevata nel caso il nickname specificato non
	 *             corrisponda ad un amico del client mittente
	 */
	public void Msg2Friend(String nickname, TextMessage m)
			throws UnknownUserException, UserNotOnlineException, NoSuchFriendException;

	/**
	 * Indica al server la volonta' di inviare un messaggio ai membri della
	 * chatroom indicata (amici e non)
	 * 
	 * @param chatId,
	 *            il nome della chat room(identificativo univoco) ai cui membri
	 *            vogliamo inviare il messaggio
	 * @param m,
	 *            il messaggio da inviare
	 * @throws NoSuchChatException,
	 *             eccezione sollevata se il nome della chatroom specificato non
	 *             corrisponde ad alcuna chatroom registrata
	 */
	public void ChatroomMessage(String chatId, TextMessage m) throws NoSuchChatException;
}
