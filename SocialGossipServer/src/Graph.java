import java.util.List;

import exceptions.*;

/**
 * Graph<E> e' un tipo di dato astratto che rappresenta un grafo, contenente
 * oggetti di tipo <E> come nodi, 'linkati' fra loro da archi non orientati; un
 * grafo Graph<E> non puo' contenere nodi null o nodi uguali, ne' consente il
 * self-link dei nodi(amicizie con se' stessi), ma sono ammessi nodi isolati
 * (utente con nessun amico). Il grafo e' rappresentato da un insieme di nodi N,
 * e da un insieme di archi NxN, l'implementazione e' lasciata alla classe che
 * implementa l'interfaccia.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public interface Graph<E> {

	/**
	 * aggiunge un nodo al grafo, e' concesso inserire un nuovo nodo anche se
	 * esso non e' connesso a nessun altro(isolato)
	 * 
	 * @param node,
	 *            l'utente da aggiungere
	 * @throws NullPointerException,
	 *             se node e' null
	 * @throws NameAlreadyInUseException,
	 *             se il nodo utente e' gia presente nel grafo(utente gia'
	 *             iscritto)
	 */
	public void addNode(E node) throws NullPointerException, NameAlreadyInUseException;

	/**
	 * aggiunge un arco linkando i due nodi inseriti (arco non orientato), se
	 * presenti / aggiunge la relazione di amicizia fra nodo1 e nodo2
	 * 
	 * @param node1
	 * @param node2
	 * @throws IllegalArgumentException,
	 *             se la relazione di amicizia fra i due nodi e' gia' presente
	 *             oppure i due nodi sono uguali
	 * @throws NullPointerException,
	 *             se uno dei due nodi e' null
	 * @throws UnknownUserException,
	 *             se uno dei due utenti non appartiene al grafo (non e'
	 *             iscritto)
	 */
	public void addEdge(E node1, E node2) throws IllegalArgumentException, NullPointerException, UnknownUserException;

	/**
	 * rimuove un nodo contenuto nel grafo, inoltre rimuove tutti gli archi
	 * diretti verso il nodo eliminato (tutte le relazioni di amicizia)
	 *
	 * @param node,
	 *            il nodo da eliminare
	 * @throws UnknownUserException,
	 *             se non esiste un utente registrato con quel nickname / il
	 *             grafo non contiene quel nodo
	 * @throws NullPointerException,
	 *             se node e' null
	 */
	public void removeNode(E node) throws UnknownUserException, NullPointerException;

	/**
	 * Rimuove l'arco del grafo che unisce i nodi node1 e node2 (rimuove una
	 * relazione di amicizia)
	 * 
	 * @param node1
	 * @param node2
	 * @throws IllegalArgumentException,
	 *             se i due nodi coincidono
	 * @throws NullPointerException,
	 *             se uno dei due nodi e' null
	 * @throws UnknownUserException,
	 *             se uno dei due nodi non e' presente nel grafo
	 */
	public void removeEdge(E node1, E node2)
			throws IllegalArgumentException, NullPointerException, UnknownUserException;

	/**
	 * Ritorna il numero di nodi contenuti nel grafo, la size; siccome la
	 * struttura che contiene i nodi deve supportare la mutua esclusione, il
	 * valore di ritorno sara' "un'approssimazione" della size, in quanto
	 * potrebbe essere modificata in ogni istante (nuovi nodi aggiunti o
	 * rimossi)
	 *
	 * @return il numero di nodi contenuti nel grafo
	 */
	public int numberOfNodes();

	/**
	 * Ritorna tutti i nodi collegati ad uno specifico nodo (tutti i nickname
	 * degli amici di un utente)
	 * 
	 * @param node
	 * @return la lista dei nodi collegati a node
	 * @throws NullPointerException,
	 *             se node e' null
	 * @throws UnknownUserException,
	 *             se node non e' presente nel grafo
	 */
	public List<String> getEdges(E node) throws NullPointerException, UnknownUserException;

	/**
	 * Restituisce la rappresentazione astratta di Graph, mostrando tutti i nodi
	 * e gli archi del grafo
	 * 
	 * @return la stringa che rappresenta il grafo
	 */
	public String toString();

}
