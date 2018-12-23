import exceptions.*;

/**
 * SocialOps.java Interfaccia che contiene le segnature dei metodi che il server
 * deve gestire riguardo la SocialNetwork, quindi riguardo la gestione delle
 * registrazioni, dei login e delle amicizie.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public interface SocialOps {

	/**
	 * Registra un client alla social network se il nickname specificato non e'
	 * gia' occupato da un altro client. Lo stato del client e' impostato su
	 * online. Risponde in caso di successo con un positivo al client In caso di
	 * parametri null lancia una NullPointerException, invece in caso di un
	 * nickname gia' presente nella social network o di lingua non riconosciuta
	 * manda al client un codice di errore come definito da AnswerCode.java
	 * 
	 * @param nickname
	 *            il nome utente che vuole registrarsi alla social network
	 * @param language
	 *            la lingua del client che si registra
	 * @throws NullPointerException,
	 *             in caso uno dei due parametri sia null
	 * @throws NameAlreadyInUseException,
	 *             in caso il nickname inserito sia gia' presente nel social
	 *             network
	 * @throws IllegalArgumentException,
	 *             se il linguaggio passato come parametro non e' riconosciuto
	 */
	public void Register(String nickname, String language)
			throws NullPointerException, NameAlreadyInUseException, IllegalArgumentException;

	/**
	 * Fa login dell'utente inserito come parametro e informa il client con un
	 * ACK in caso di avvenuto login, altrimenti in caso di nickname non
	 * riconosciuto o di nickname gia' online risponde al client con un codice
	 * errore come definito da AnswerCode.java In caso di parametro null lancia
	 * una NullPointerException
	 * 
	 * @param nickname
	 *            il nome utente che vuole fare login
	 * @throws UnknownUserException,
	 *             in caso il nickname non corrisponda a nessun utente
	 *             registrato
	 * @throws NullPointerException,
	 *             in caso nickname sia null
	 */
	public void Login(String nickname) throws UnknownUserException, NullPointerException;

	/**
	 * Il server cerca il nickname richiesto dal parametro ed informa il client
	 * dell'esito della ricerca. In caso di parametro null lancia una
	 * NullPointerException
	 * 
	 * @param nickname
	 *            il nome utente da ricercare
	 * @throws NullPointerException,
	 *             se il nickname e' null
	 */
	public void LookUp(String nickname) throws NullPointerException, UnknownUserException;

	/**
	 * Il server crea una relazione di amicizia fra il client che l'ha richiesta
	 * e quello specificato dal parametro ed informa il client dell'esito , in
	 * caso di parametro null lancia una NullPointerException. Se tale utente
	 * non viene riconosciuto come presente nella social network o una relazione
	 * di amicizia esiste gia' viene inviato al client un codice errore coerente
	 * con quelli in AnswerCode.java
	 * 
	 * @param nickname
	 *            nome utente di cui voglio verificare l'esistenza
	 * @throws NullPointerException,
	 *             se il parametro e' null
	 * @throws UnknownUserException,
	 *             se il nickname non corrisponde ad alcun utente registrato
	 */
	public void Friendship(String nickname) throws NullPointerException;

	/**
	 * Invia al client che ha fatto richiesta la lista dei suoi amici
	 */
	public void Listfriend();

}
