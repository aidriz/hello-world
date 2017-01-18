/**
* Speech Recognition based on Google Speech API v2
*
* @author  ADEM F. IDRIZ
* @version 1.0
* @since  2016 - TU Delft
*/

package vpro.sr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;

import vpro.sr.SRGUI;

public class RecognizeSpeech {

	static String newline = System.getProperty("line.separator");
	public static String Transcript = "";
	public static String Results = "";
	public static String ConfidenceScore = "";
	public static String CompactResults = "";
	static List<String> OtherPossibleResponses = new ArrayList<String>();
	private static int max_alt = 0;

	public static void Recognize(File file, String Language, String APIKEY, int Alternatives, int SampleRate) {

		// Define file which is send to Google Speech API v2
		// File file = new File(WavFilePath);

		max_alt = Alternatives;

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
			// GoogleResponse response =
			// recognizer.getRecognizedDataForWave(file, Alternatives, 16000);

			/* FLAC */
			// GoogleResponse response =
			// recognizer.getRecognizedDataForFlac(file, 4, 16000);

			GoogleResponse response = recognizer.getRecognizedDataForFlac(file, Alternatives, SampleRate);

			// Print results
			displayResponse(response);

			file.deleteOnExit(); // Deletes the file as it is no longer
									// necessary.

		} catch (Exception ex) {
			System.out.println("ERROR: Google cannot be contacted");
			ex.printStackTrace();
		}

	}

	private static void displayResponse(GoogleResponse gr) {
		if (gr.getResponse() == null) {
			System.out.println((String) null);
			Results = "";
			Transcript="";
			ConfidenceScore = "0";
			OtherPossibleResponses.clear();
			SRGUI.NotificationsTextArea.append(null + newline);
			return;
		}

		SRGUI.NotificationsTextArea.append("Google Response: '" + gr.getResponse() + "'" + newline);

		Transcript = gr.getResponse();

		System.out.println("Google Response: '" + gr.getResponse() + "'");
		System.out.println("Google is " + Double.parseDouble(gr.getConfidence()) * 100 + " % confident in the reply");

		SRGUI.NotificationsTextArea.append(
				"Google is " + Double.parseDouble(gr.getConfidence()) * 100 + "% confident in the reply" + newline);

		List<String> dummyOthers = gr.getOtherPossibleResponses();

		if (dummyOthers.size() > max_alt) {
			// Crop
			dummyOthers = gr.getOtherPossibleResponses().subList(0, max_alt);

		}

		if (dummyOthers.size() != 0) {
			System.out.println("Other Possible Response(s): ");

			SRGUI.NotificationsTextArea.append("Other Possible Response(s): " + newline);

			for (String s : dummyOthers) {
				System.out.println("\t" + s);
				SRGUI.NotificationsTextArea.append(s + newline);
			}
		}
		// Convert the ArrayList into a String.
		String others = String.join("&", dummyOthers);

		OtherPossibleResponses = dummyOthers;

		Results = gr.getResponse() + "&" + others;

		ConfidenceScore = gr.getConfidence();

		CompactResults = gr.getResponse() + "&" + ConfidenceScore + "&" + others;
	}
}