package condivise;

import java.util.HashMap;
import java.util.Map;

import exceptions.NoSuchCodeException;

/**
 * I codici possibili di risposta del server alle richieste dei client
 * Questa enum gestisce anche una correlazione fra l'identificatore del
 * codice di risposta ed un suo codice numerico specifico.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public enum ResponseCode {

	OP_OK(200), 
	NICK_ALREADY_TAKEN(201), 
	NICKNAME_UNKNOWN(202), 
	LANGUAGE_NOT_SUPPORTED(203), 
	OP_FAIL(204), 
	USER_OFFLINE(205), 
	ALREADY_ONLINE(206), 
	NOT_A_FRIEND(207), 
	ALREADY_A_FRIEND(208),
	ALREADY_IN_CHATROOM(209),
	NO_ONE_ONLINE(210),
	CHATROOM_UNKNOWN(211);

	// il codice relativo ad ogni opzione
	private int code;

	// la map che associa al field il relativo codice
	private static Map<Integer, ResponseCode> map = new HashMap<Integer, ResponseCode>();

	// inizializzo tutte le associazioni ResponseCode -> Codice, e lo faccio una
	// sola volta
	static {
		for (ResponseCode code : ResponseCode.values()) {
			map.put(code.code, code);
		}
	}

	private ResponseCode(final int intCode) {
		// COSTRUTTORE
		code = intCode;
	}
	
	
	/**
	 * restituisce il codice della enum
	 * @return il codice della risposta
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
	public static ResponseCode DescriptionOf(int codeNo) throws NoSuchCodeException {
		ResponseCode toRet = map.get(codeNo);
		if (toRet == null)
			throw new NoSuchCodeException();
		return toRet;
	}

}
