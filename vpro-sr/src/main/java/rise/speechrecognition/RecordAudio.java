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

public class RecordAudio {

	// public static String filename =
	// "/home/nao/recordings/microphones/test.wav";
	public static String filename = "/home/nao/test.wav";

	public static void run(ALAudioRecorder ar, String mic_channel, int duration) {

		try {
			/// Configures the channels that need to be recorded.

			ArrayList<Integer> channels = new ArrayList<Integer>();

			switch (mic_channel) {
			case "Left":
				/* left channel */
				channels.add(1);
				channels.add(0);
				channels.add(0);
				channels.add(0);
				System.out.println("Left Channel");

				break;
			case "Right":
				/* right channel */
				channels.add(0);
				channels.add(1);
				channels.add(0);
				channels.add(0);
				System.out.println("Right Channel");

				break;
			case "Front":
				/* front channel */
				channels.add(0);
				channels.add(0);
				channels.add(1);
				channels.add(0);
				System.out.println("Front Channel");

				break;
			case "Rear":
				/* rear channel */
				channels.add(0);
				channels.add(0);
				channels.add(0);
				channels.add(1);
				System.out.println("Rear Channel");

				break;
			case "4ch":
				/* 4 channel */
				channels.add(1);
				channels.add(1);
				channels.add(1);
				channels.add(1);
				System.out.println("4 Channels");

				break;
			default:
				throw new IllegalArgumentException("Invalid channel choice: " + mic_channel);
			}

			System.out.println("Please say something...");

			System.out.println("Recording...");

			// Start recording
			ar.startMicrophonesRecording(filename, "wav", 16000, channels);

			// Thread.sleep(8000);
			Thread.sleep(duration * 1000);

			// Stops the recording and close the file after "duration"
			// seconds.
			ar.stopMicrophonesRecording();

			System.out.println("Recording Completed!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}