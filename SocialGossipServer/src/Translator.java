import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import exceptions.IllegalLanguageException;

/**
 * Classe che si occupa di prendere una stringa, due lingue e tradurre la
 * stringa richiesta, se possibile, dalla prima lingua alla seconda lingua
 * Sfrutta il servizio REST offerto da myMemory.Translated e puo' fare
 * traduzioni di piu' di 500 caratteri (limite massimo imposto dal servizio)
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class Translator {

	// il path del servizio a cui fare richiesta, senza i parametri della query
	private static final String pathName = "https://api.mymemory.translated.net/";

	// formato della query:
	// "pathname + get?q=stringadiprova'=it|en
	// ha compresi i segnaposto per stringa, lingua 1 e lingua 2
	private static final String format = pathName + "get?q=%s&langpair=%s|%s";

	public Translator() {
		// COSTRUTTORE
	}

	/**
	 * Traduce una stringa passata come parametro trasformandola da una lingua
	 * ad un altra connettendosi al servizio REST di mymemory.translated.net
	 * Funziona anche per stringhe superiori a 500 caratteri (limite massimo di
	 * caratteri per una query di questo servizio)
	 * 
	 * @param toTranslate
	 *            la stringa da trasformare
	 * @param mittLanguage
	 *            la lingua del client mittente
	 * @param destLanguage
	 *            la lingua del client destinatario
	 * @return la stringa tradotta
	 * @throws IllegalLanguageException
	 *             se una delle due lingue non esiste
	 * @throws NullPointerException
	 *             se un parametro fosse null
	 */
	public String Translate(String toTranslate, String mittLanguage, String destLanguage)
			throws IllegalLanguageException {

		// controllo subito la correttezza dei parametri
		if (toTranslate == null || mittLanguage == null || destLanguage == null)
			throw new NullPointerException();

		System.out.println(mittLanguage + " / " + destLanguage);

		// stessa lingua per mittende e destinatario, non devo far nulla
		if (mittLanguage.equals(destLanguage))
			return toTranslate;

		// la stringa potrebbe essere più lunga di 500 caratteri, quindi devo
		// creare un array di query di una lunghezza ragionevole per poterle
		// mandare una dopo l'altra e attendere i risultati
		// non devo pero' fare split a caso altrimenti rischio di mandare parole
		// non compiute.

		// alloco un array di query da 300 caratteri l'una (per stare tranquillo
		// sulla size della query)
		String[] query = new String[(toTranslate.length() / 300) + 1];
		// alloco un array di indici in cui fare split (sara' sugli spazi)
		int[] splittingPoints = new int[query.length + 1];
		// il primo punto sara' l'inizio della stringa da tradurre
		splittingPoints[0] = 0;
		// l'ultimo sara' la fine della stringa
		splittingPoints[splittingPoints.length - 1] = toTranslate.length();

		// trovo all'interno della stringa degli spazi ogni circa 300 caratteri
		// e mi salvo il loro indice, saranno i punti in cui dividero' la
		// stringa
		for (int i = 1; i < splittingPoints.length - 1; i++) {
			splittingPoints[i] = toTranslate.indexOf(' ', 300 * i);
			// se non ho trovato uno spazio oppure e' molto lontano, metto lo
			// splitting point dove mi pare
			if (splittingPoints[i] == -1 || splittingPoints[i] > 300 * (1 + i))
				splittingPoints[i] = 300 * i;
		}

		// adesso devo fare lo split effettivo e creare le query vere e proprie
		for (int i = 0; i < query.length; i++)
			query[i] = toTranslate.substring(splittingPoints[i], splittingPoints[i + 1]);

		// preparo lo string buffer in cui concateno i risultati delle query
		StringBuffer toReturn = new StringBuffer("");

		// adesso devo inviare una per una le query al servizio di traduzione
		for (int i = 0; i < query.length; i++) {
			// prima cosa: sostituisco ogni spazio con %20 per la URL
			query[i] = query[i].replaceAll(" ", "%20");
			// preparo la singola query i-Esima. format ha compreso il formato
			// corretto di una query.
			String toSend = String.format(format, query[i], mittLanguage, destLanguage);
			try {
				// creo la URL
				URL url = new URL(toSend);
				// apro la connessione verso il servizio
				URLConnection uc = url.openConnection();
				uc.connect();
				// mi preparo a leggere la risposta
				BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String reply = null;
				StringBuffer sb = new StringBuffer();
				while ((reply = in.readLine()) != null)
					sb.append(reply);

				// letta l'intera risposta, faccio il parsing JSON
				JSONObject obj = (JSONObject) new JSONParser().parse(sb.toString());
				// ottengo i dati della risposta
				JSONObject results = (JSONObject) obj.get("responseData");
				if (results == null) // non mi ha fatto la traduzione
					return toTranslate; // ritorno il messaggio non tradotto
				else // ho tradotto un pezzo, me lo salvo nello StringBuffer
					toReturn.append((String) results.get("translatedText"));

			} catch (MalformedURLException e) {
				// L'errore e' probabilmente nella stringa
				throw new IllegalLanguageException();
			} catch (IOException e) {
				System.out.println("IoException: Unable to Translate");
				return toTranslate;
			} catch (ParseException e) {
				System.out.println("Error: Unable to Translate");
				return toTranslate;
			}
		}
		// traduzione finita con successo, posso ritornare la stringa tradotta
		return toReturn.toString();
	}

}
