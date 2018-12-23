import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import exceptions.AlreadyAFriendException;
import exceptions.NameAlreadyInUseException;
import exceptions.UnknownUserException;

/**
 * Implementazione del grafo(Graph) definito nelle interfacce, che
 * rappresentera' l'intera rete degli utenti e le relazioni di amicizia fra
 * essi. Il cuore dell'implementazione del network sociale e' rappresentato da
 * un'hash map che conterra' tutti gli utenti registrati a Social Gossip: questa
 * struttura permette l'associazione fra l'username di un utente(identificativo
 * unico) ed il suo oggetto User(la sua rappresentazione sul server); la scelta
 * dell'hash map come struttura dati e' conseguenza della ricerca di
 * un'efficienza nelle operazioni che il server deve svolgere piu' comunemente,
 * ossia la ricerca di un utente fra tutti quelli registrati, per le piu' varie
 * ragioni, e l'aggiunta efficiente di un nuovo User. La seconda struttura dati
 * al centro dell'implementazione del network e' la rappresentazione delle
 * relazioni fra gli utenti, le amicizie: anche in questo caso si tratta di
 * un'hash map, che associa ad ogni nickname la sua lista di amici, lista
 * rappresentata come un'ulteriore hash map che lega il nome utente dell'amico
 * alla sua rappresentazione; anche in questo caso la scelta della struttura
 * dati e' stata effettuata per ragioni di efficienza nel matching utente-amici
 * dell'utente, nella ricerca veloce di un amico fra una lista di amici, e per
 * privilegiare l'efficienza anche nelle operazioni di aggiunta di una nuova
 * relazione di amicizia. Entrambe le strutture sono concurrent, poiche' abbiamo
 * previsto la possibilita' di rimuovere una relazione di amicizia cosi come di
 * effettuare la deregistrazione da Social Gossip
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class SocialGraph implements Graph<User> {

	// rappresentazione utenti
	private ConcurrentHashMap<String, User> Nodes;
	// rappresentazione relazioni fra utenti
	private ConcurrentHashMap<String, ConcurrentHashMap<String, User>> Edges;

	/**
	 * Creazione di un grafo inizializzando la struttura dati contenente i nodi
	 * e gli archi.
	 */
	public SocialGraph() {
		Nodes = new ConcurrentHashMap<String, User>();
		Edges = new ConcurrentHashMap<String, ConcurrentHashMap<String, User>>();
	}

	/**
	 * Aggiunge un utente alla rete, operazione svolta dal server a seguito di
	 * una richiesta di registrazione andata a buon fine
	 * 
	 * @throws NameAlreadyInUseException,
	 *             in caso l'utente sia gia' presente
	 */
	public void addNode(User user) throws NullPointerException, NameAlreadyInUseException {
		if (user == null)
			throw new NullPointerException();
		// Controllo che l'utente inserito non sia gia' presente
		if (Nodes.containsKey(user.getUsername()))
			throw new NameAlreadyInUseException("L'utente inserito e' giá presente");

		Nodes.put(user.getUsername(), user);
		Edges.put(user.getUsername(), new ConcurrentHashMap<>());

	}

	/**
	 * aggiunge un arco linkando i due nodi inseriti (arco non orientato), se
	 * presenti / aggiunge la relazione di amicizia fra nodo1 e nodo2
	 * 
	 * @param user1
	 * @param user2
	 * @throws IllegalArgumentException,
	 *             se la relazione di amicizia fra i due nodi e' gia' presente
	 *             oppure i due nodi sono uguali
	 * @throws NullPointerException,
	 *             se uno dei due nodi e' null
	 * @throws UnknownUserException,
	 *             se uno dei due utenti non appartiene al grafo (non e'
	 *             iscritto)
	 */
	public void addEdge(User user1, User user2) throws IllegalArgumentException, NullPointerException {
		if (user1 == null || user2 == null)
			throw new NullPointerException();
		if (user1.equals(user2))
			throw new IllegalArgumentException("I due user inseriti coincidono");
		// controlla che i nodi inseriti siano presenti nel grafo
		if (!Nodes.containsKey(user1.getUsername()) || !Nodes.containsKey(user2.getUsername()))
			throw new IllegalArgumentException("Almeno uno dei due nodi inseriti non fa parte del grafo");
		// controlla che l'arco non sia gia presente
		if (Edges.get(user1.getUsername()).containsKey(user2.getUsername()))
			throw new IllegalArgumentException("I due nodi sono giá connessi da un arco");

		// essendo la relazione di amcicizia un arco non orientato, aggiungo
		// l'user2 agli amici di user1 e viceversa
		Edges.get(user1.getUsername()).put(user2.getUsername(), user2);
		Edges.get(user2.getUsername()).put(user1.getUsername(), user1);

	}

	/**
	 * Elimina un utente da quelli registrati, a seguito di una richiesta di
	 * deregistrazione; l'eliminazione da Social Gossip comprende l'eliminazione
	 * dell'utente e di tutte le sue relazioni di amicizia dalla sua lista e da
	 * quella dei suoi amici
	 * 
	 * @param user,
	 *            l'utente da eliminare
	 */
	public void removeNode(User user) throws IllegalArgumentException, NullPointerException, UnknownUserException {
		if (user == null)
			throw new NullPointerException();
		// controllo se l'utente inserito e' presente fra quelli registrati
		if (Nodes.containsKey(user.getUsername()))
			throw new UnknownUserException("Utente inserito non e' presente!");

		// rimuovo tutte le relazioni di amicizia relative all'utente inserito,
		// andando a modificare la struttura dati degli utenti che
		// sono amici di 'user'

		// per eliminare l'utente dalle relazioni d'amicizia dei suoi amici, ho
		// bisogno della lista degli amici dell'utente da eliminare;
		// la lista degli amici di 'user' e' rappresentata da un Set di nickname
		Set<String> friends = Edges.get(user.getUsername()).keySet();
		Iterator<String> iter = friends.iterator();
		// adesso posso iterare sugli amici di 'user', andando ad eliminare
		// 'user' dalla lista degli amici di ogni amico di 'user'
		while (iter.hasNext()) {
			// prendo l'username di un amico di 'user'
			String friend = iter.next();
			// prendo la lista degli amici di 'friend' e rimuovo 'user' dagli
			// amici di 'friend'
			Edges.get(friend).remove(user.getUsername());
		}

		// rimuovo la lista degli amici dell'utente da eliminare
		Edges.remove(user.getUsername());

		// rimuovo l'utente dalla struttura dati
		Nodes.remove(user.getUsername());

	}

	/**
	 * Rimuove una relazione di amicizia fra due utenti; la lista degli amici di
	 * entrambi verra' modificata, in quanto la relazione di amicizia e' un arco
	 * non orientato
	 * 
	 * @param user1
	 * @param user2
	 * @throws IllegalArgumentException,
	 *             se i due user inseriti coincidono
	 * @throws NullPointerException,
	 *             se uno dei due nodi e' null
	 * @throws UnknownUserException,
	 *             se uno dei due nodi non e' presente nel grafo
	 */
	public void removeEdge(User user1, User user2)
			throws IllegalArgumentException, NullPointerException, UnknownUserException {
		if (user1 == null || user2 == null)
			throw new NullPointerException();
		if (user1.equals(user2))
			throw new IllegalArgumentException("I due User inseriti sono uguali!");

		// Cerco node1 e node2 fra i nodi contenuti nel grafo
		// controllo che entrambi i nodi siano presenti
		if (Nodes.get(user1.getUsername()) == null || Nodes.get(user2.getUsername()) == null)
			throw new UnknownUserException("Almeno uno dei due nodi inseriti non fa parte del grafo!");

		// rimuovo la relazione di amicizia fra i due nodi, eliminando gli
		// utenti dalla lista degli amici di entrambi
		Edges.get(user1.getUsername()).remove(user2.getUsername());
		Edges.get(user2.getUsername()).remove(user1.getUsername());
	}

	/**
	 * Verifica se un utente e' presente nella rete sociale
	 * 
	 * @param nickname
	 *            l'utente che voglio verificare se e' presente
	 * @return true se e' presente, false altrimenti
	 * @throws NullPointerException
	 *             se il parametro e' null
	 */
	public boolean isUser(String nickname) throws NullPointerException {
		if (nickname == null)
			throw new NullPointerException();

		// controllo semplicemente se l'utente richiesto e' presente
		if (Nodes.get(nickname) == null)
			return false;
		return true;
	}

	/**
	 * Restituisce l'utente richiesto attraverso il suo nickname
	 * 
	 * @param nickname
	 *            il nome dell'utente
	 * @return l'utente ricercato
	 * @throws UnknownUserException
	 *             se l'utente non fosse stato trovato
	 */
	public User getUser(String nickname) throws UnknownUserException {
		if (nickname == null)
			throw new NullPointerException();

		User user;
		// controllo semplicemente se l'utente richiesto e' presente
		if ((user = Nodes.get(nickname)) == null)
			throw new UnknownUserException();
		return user;
	}

	/**
	 * Verifica che due utenti siano amici fra loro
	 * 
	 * @param u1
	 *            il primo utente
	 * @param u2
	 *            il secondo utente
	 * @return true se sono amici, false altrimenti
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 * @throws UnknownUserException
	 *             se un utente non esistesse
	 */
	public boolean areFriends(String u1, String u2) throws NullPointerException, UnknownUserException {

		// controllo la validita' dei parametri
		if (u1 == null || u2 == null)
			throw new NullPointerException();
		if (Nodes.get(u1) == null || Nodes.get(u2) == null)
			throw new UnknownUserException();

		if (Edges.get(u1).containsKey(u2) || Edges.get(u2).containsKey(u1))
			return true;
		return false;
	}

	/**
	 * Ritorna il numero degli utenti registrati, approssimativamente poiche' la
	 * struttura potrebbe venire modificata poco dopo da un altro thread
	 * 
	 * @return numero degli utenti registrati
	 */
	public int numberOfNodes() {
		return Nodes.size();
	}

	/**
	 * Ritorna la lista contente gli username degli amici di un utente
	 * 
	 * @return la lista dei nickname degli amici di un utente
	 * @throws NullPointerException,
	 *             se node e' null
	 * @throws UnknownUserException,
	 *             se node non e' presente nel grafo
	 */
	public List<String> getEdges(User user) throws NullPointerException, UnknownUserException {
		if (user == null)
			throw new NullPointerException();
		if (!Nodes.containsKey(user.getUsername()))
			throw new UnknownUserException();
		// ottengo la lista degli username degli amici di 'user', rappresentata
		// da un Set di String
		// converto il set in una lista
		List<String> list = new ArrayList<String>(Edges.get(user.getUsername()).keySet());
		return list;
	}

	/**
	 * Restituisce l'array di nickname amici di quello passato come parametro
	 * 
	 * @param nickname
	 *            il nick dell'utente di cui voglio scoprire gli amici
	 * @return l'array di nickname di amici
	 * @throws NullPointerException
	 *             se nickname fosse null
	 * @throws UnknownUserException
	 *             se l'utente non esistesse
	 */
	public String[] getFriends(String nickname) throws NullPointerException, UnknownUserException {

		if (nickname == null)
			throw new NullPointerException();
		User u = this.Nodes.get(nickname);
		if (u == null)
			throw new UnknownUserException();
		return this.getEdges(u).toArray(new String[0]);
	}

	/**
	 * Ritorna la rappresentazione del grafo
	 */
	public String toString() {
		return Edges.toString();
	}

	/**
	 * Aggiunge una relazione di amicizia fra due utenti
	 * 
	 * @param user1
	 *            utente 1 richiedente amicizia
	 * @param user2
	 *            utente 2
	 * @throws NullPointerException
	 *             se un parametro e' null
	 * @throws UnknownUserException
	 *             se un utente non esiste
	 * @throws AlreadyAFriendException
	 *             se esiste gia' questa relazione di amicizia
	 */
	public void newFriendship(String user1, String user2)
			throws NullPointerException, UnknownUserException, AlreadyAFriendException {
		if (user1 == null || user2 == null)
			throw new NullPointerException();
		User u1 = Nodes.get(user1);
		User u2 = Nodes.get(user2);
		if (u1 == null || u2 == null)
			throw new UnknownUserException();
		if (user1.equals(user2))
			throw new AlreadyAFriendException();
		if (!this.areFriends(user1, user2))
			this.addEdge(u1, u2);
		else
			throw new AlreadyAFriendException();
	}

}
