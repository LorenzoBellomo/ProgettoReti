package condivise;

import java.util.HashMap;
import java.util.Map;

import exceptions.NoSuchCodeException;

/**
 * Le possibili richieste di un client al server. Questa enum gestisce anche
 * una correlazione fra l'identificatore del codice di risposta ed un suo
 * codice numerico specifico.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public enum RequestCode {

	REGISTER(100), 
	LOGIN(101), 
	LOOKUP(102), 
	FRIENDSHIP(103), 
	FRIEND_LIST(104), 
	FILE2FRIEND(105), 
	MSG2FRIEND(106), 
	CHATROOM_MSG(107), 
	CREATE_CHATROOM(108), 
	ADD_TO_CHATROOM(109), 
	CHATROOM_LIST(110), 
	CLOSE_CHAT(111),
	OPEN_P2PCONN(300);

	// il codice relativo ad ogni opzione
	private int code;

	// la map che associa al field il relativo codice
	private static Map<Integer, RequestCode> map = new HashMap<>();

	// inizializzo tutte le associazioni RequestCode -> Codice, e lo faccio una
	// sola volta
	static {
		for (RequestCode code : RequestCode.values()) {
			map.put(code.code, code);
		}
	}

	private RequestCode(final int intCode) {
		// COSTRUTTORE
		code = intCode;
	}
	
	
	/**
	 * restituisce il codice della enum
	 * @return il codice della richiesta
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Restituisce la spiegazione a parole del codice di risposta passato come
	 * parametro
	 * 
	 * @param codeNo
	 *            il codice di cui voglio la descrizione
	 * @return Il codice a parole relativo all'intero passato
	 * @throws NoSuchCodeException
	 *             se il valore passato come parametro non e' un codice valido
	 */
	public static RequestCode DescriptionOf(int codeNo) throws NoSuchCodeException {
		RequestCode toRet = map.get(codeNo);
		if (toRet == null)
			throw new NoSuchCodeException();
		return toRet;
	}

}
