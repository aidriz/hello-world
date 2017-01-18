/**
* Speech Recognition based on Google Speech API v2
*
* @author  ADEM F. IDRIZ
* @version 1.0
* @since  2016 - TU Delft
*/

package rise.speechrecognition;

import java.util.ArrayList;

import com.aldebaran.qi.helper.proxies.ALAudioRecorder;

import rise.connectors.nao.NAOConnector;
import rise.core.utils.tecs.EventHandler;
import rise.core.utils.tecs.GetSpeech;
import rise.core.utils.tecs.Speech;

public class SpeechRecognition extends Thread {

	// Google Speech API v2 Key
	static String apikey = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";
	// Microphone Channel on NAO . Default Front
	private static String mic = "Front";

	// Default
	public static int RecordingLength = 0;
	public static String RecordingLanguage = "English";
	public static int MaxAlternativeNumber = 0;

	private ALAudioRecorder alALAudioRecorder;

	ArrayList<Integer> channels = new ArrayList<Integer>();

	 protected static boolean shouldRun = false;
//	protected static boolean shouldRun = true;
	protected static boolean getSR = false;
	
	
	// Constructor
	public SpeechRecognition(com.aldebaran.qi.Session session) {
		try {

			this.alALAudioRecorder = new ALAudioRecorder(session);

			shouldRun = true;

		} catch (Exception e) {
			System.out.println("Error creating Speech Recognition thread");
			e.printStackTrace();
		}
	}

	public void run() {

		System.out.println("*************SpeechRecognition*******************");

		while (shouldRun) {
			try{
				sleep(50);
				@SuppressWarnings("unused")	
				String workingDir = System.getProperty("user.dir");
	
				if (getSR) {
	
					// record audio file
					RecordAudio.run(alALAudioRecorder, mic, RecordingLength);
	
	//				/*Off-line test purpose*/
	//				 String filepath= workingDir+"/testrear.wav";
	//				 String filepath= workingDir+"/testleft.wav";
	//				 String filepath = workingDir + "/testdutch.wav";
	//				 
	//				RecognizeSpeech.Recognize(filepath, RecordingLanguage, apikey, MaxAlternativeNumber);
	
					// send audio file to Google Speech API
					RecognizeSpeech.Recognize(RecordAudio.filename, RecordingLanguage, apikey, MaxAlternativeNumber);
	
					// send Recognized Speech result to TECS Server
					sendRecognizedSpeech(RecognizeSpeech.Results,
							Double.parseDouble(RecognizeSpeech.ConfidenceScore) * 100);
	
				}
			} catch (Exception e){System.out.println(e.toString());}
		}

	}

	public void stopListening() {
		try {
			alALAudioRecorder.exit();
			shouldRun = false;
		} catch (Exception e) {
			System.out.println("Error closing Speech Recognition thread");
			e.printStackTrace();
		}
	}

	/* Sending Recognized Speech to server */

	public void sendRecognizedSpeech(String results, double confidence) {

		NAOConnector.sendToServer(new Speech(101, results, confidence));

		System.out.println("Recognized Speech is transmitted");

		// Disable SpeechRecognition
		getSR = false;

	}

	public static // Handle type of getSpeech message
	EventHandler<GetSpeech> getSpeechEventHandler = new EventHandler<GetSpeech>() {
		public void handleEvent(GetSpeech event) {

			// Parsing Messages
			RecordingLength = ((GetSpeech) event).duration;

			RecordingLanguage = ((GetSpeech) event).language;

			MaxAlternativeNumber = ((GetSpeech) event).alternatives;

			System.out.println("Recording Length: " + RecordingLength + "  -  " + "Recording Language: "
					+ RecordingLanguage + "  -  " + "Number of Alternatives: " + MaxAlternativeNumber);

			// Enable SpeechRecognition
			getSR = true;

		}
	};

}
