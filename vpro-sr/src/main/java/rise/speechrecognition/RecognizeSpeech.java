/**
* Speech Recognition based on Google Speech API v2
*
* @author  ADEM F. IDRIZ
* @version 1.0
* @since  2016 - TU Delft
*/

package rise.speechrecognition;

import java.io.File;

import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;

public class RecognizeSpeech {

	public static String Results = "";
	public static String ConfidenceScore = "";

	public static void Recognize(String WavFilePath, String Language, String APIKEY, int Alternatives) {

		// Define file which is send to Google Speech API v2
		File file = new File(WavFilePath);

		Recognizer recognizer;

		// Set Language and API Key
		if (Language.equals("English")) {
			recognizer = new Recognizer(Recognizer.Languages.ENGLISH_US, APIKEY);
		} else if (Language.equals("Dutch")) {
			recognizer = new Recognizer(Recognizer.Languages.DUTCH, APIKEY);
		} else {
			System.out.println("Language is AUTO Detected");
			recognizer = new Recognizer(Recognizer.Languages.AUTO_DETECT, APIKEY);
		}

		try {

			/* WAV */
			// GoogleResponse response =
			// recognizer.getRecognizedDataForWave(file, 4, 16000);
			//GoogleResponse response = recognizer.getRecognizedDataForWave(file, Alternatives, 16000);
			GoogleResponse response = recognizer.getRecognizedDataForWave(file, Alternatives);
			
			/* FLAC */
			// GoogleResponse response =
			// recognizer.getRecognizedDataForFlac(file, 4, 16000);

			// Print results 
			displayResponse(response);
			
		} catch (Exception ex) {
			System.out.println("ERROR: Google cannot be contacted");
			ex.printStackTrace();
		}

	}
	
	private static void displayResponse(GoogleResponse gr) {
		if (gr.getResponse() == null) {
			System.out.println((String) null);
			Results = "";
			ConfidenceScore = "0";
			return;
		}
		System.out.println("Google Response: " + gr.getResponse());
		System.out
				.println("Google is " + Double.parseDouble(gr.getConfidence()) * 100 + "% confident in" + " the reply");
		System.out.println("Other Possible responses are: ");

		for (String s : gr.getOtherPossibleResponses()) {
			System.out.println("\t" + s);
		}

		// Convert the ArrayList into a String.
		String others = String.join("&", gr.getOtherPossibleResponses());

		Results = gr.getResponse() + "&" + others;

		ConfidenceScore = gr.getConfidence();
	}
}