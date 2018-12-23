import exceptions.NameAlreadyInUseException;
import exceptions.NoSuchChatException;

/**
 * ChatRoomOps.java Interfaccia che contiene le segnature dei metodi di gestione
 * delle chat rooms, richiesti da un client generico per creare una nuova chat
 * room, essere aggiunto ad una chat presistente, richiedere la lista delle chat
 * rooms o chiuderne una(nel caso il client sia il founder della chat room).
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public interface ChatRoomOps {

	/**
	 * Crea una chat room con identificativo chatId, il client che chiama questa
	 * operazione verra' identificato come founder della chat room (se
	 * l'operazione termina con esito positivo)
	 * 
	 * @param chatId,
	 *            stringa che rappresenta il titolo/argomento della chat da
	 *            creare
	 * @throws NameAlreadyInUseException,
	 *             eccezione sollevata nel caso
	 */
	public void CreateChatRoom(String chatId) throws NameAlreadyInUseException;

	/**
	 * Richiede al server di essere aggiunto alla chat room con titolo/argomento
	 * chatId
	 * 
	 * @param chatId,
	 *            stringa che identifica univocamente una chat room giá
	 *            esistente
	 * @throws NoSuchChatException,
	 *             eccezione sollevata nel caso il chatId specificato non
	 *             corrisponda ad alcuna chat registrata
	 */
	public void AddMe(String chatId) throws NoSuchChatException;

	/**
	 * @returns Restituisce una lista di tutte le chat-rooms, con indicazione di
	 *          quelle a cui l’utente e' iscritto
	 */
	public String[] ChatList();

	/**
	 * Richiede al server la chiusura di una chat room creata dall'utente stesso
	 * 
	 * @param chatId,
	 *            stringa che rappresenta il titolo/argomento della chat da
	 *            eliminare
	 * @throws NoSuchChatException,
	 *             eccezione sollevata nel caso il chatId specificato non
	 *             corrisponda ad alcuna chat registrata con founder uguale al
	 *             client richiedente l'eliminazione
	 */
	public void CloseChat(String chatId) throws NoSuchChatException;
}
