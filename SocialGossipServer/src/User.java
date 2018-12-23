import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe che definisce l'utente, i suoi attributi e le sue caratteristiche; e'
 * la rappresentazione utilizzata dal server per mantenere le informazioni sugli
 * utenti. Ogni utente appartenente alla rete di Social Gossip verra'
 * identificato univocamente da un suo oggetto User, istanziato alla sua
 * registrazione e mantenuto dal server in una struttura dati (SocialGraph).
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class User {

	// nickname, identificativo univoco del client, definisce l'username
	// dell'utente su SocialGossip
	private String username;

	// linguaggio che l'utente intende utilizzare su SocialGossip, ricevendo
	// cosi esclusivamente messaggi tradotti nella lingua scelta
	private String language;

	// associazione lingua -> codice. un italiano dovrà inserire la lingua
	// come "italiano", un francese come "français" ...
	private static final Map<String, String> languagesMap;

	// staticamente inizializzo la map delle lingue con le lingue accettate
	static {
		// chiave "nomelinguaesteso" -> valore "codiceIso"
		Map<String, String> temp = new HashMap<>();
		temp.put("Italiano", "it");
		temp.put("English", "en");
		temp.put("Francais", "fr");
		temp.put("Deutsch", "de");
		temp.put("Espanol", "es");
		temp.put("Chinese", "zh");
		languagesMap = Collections.unmodifiableMap(temp);
	}

	public String getUsername() {
		return username;
	}

	public String getLanguage() {
		return language;
	}

	/**
	 * costruttore, crea un nuovo oggetto utente con username e language passati
	 * se la lingua non e' riconosciuta la lingua di default e' l'italiano
	 * 
	 * @param username,
	 *            la stringa contente il nickname del nuovo utente
	 * @param language,
	 *            il linguaggio scelto dall'utente al momento della
	 *            registrazione
	 */
	public User(String username, String language) {
		this.username = username;
		// data la lingua scritta in modo esteso, ottengo il codice iso
		String lang = languagesMap.get(language);
		// non ho trovato il codice, metto italiano di default
		if (lang == null)
			this.language = "it";
		// altrimenti metto il codice ottenuto
		else
			this.language = lang;
	}

	/**
	 * Confronta l'utente corrente con quello passato come parametro: due utenti
	 * sono uguali se i loro username sono gli stessi, essendo l'username un
	 * identificatore unico dell'oggetto
	 * 
	 * @param u,
	 *            l'utente con cui confronteremo l'oggetto corrente(this)
	 * @return true, se i due utenti coincidono, false altrimenti
	 */
	public boolean equals(User u) {
		return this.getUsername().equals(u.getUsername());
	}

	/**
	 * Rappresentazione astratta dell'oggetto utente
	 */
	public String toString() {
		if (language == null)
			return "Utente: ".concat(username);
		else
			return "Utente: ".concat(username).concat(", con linguaggio: ").concat(language);
	}

}
