package rise.connectors.pepper;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.EventCallback;
import com.aldebaran.qi.helper.proxies.*;
import com.aldebaran.qi.AnyObject;

import rise.connectors.pepper.TECSconnect;

import rise.core.utils.tecs.*;


import java.util.List;


public class PepperConnector {

    // Pepper specific
    private String pepperIPAddress;
    private int pepperPort = 9559;
    private String TECSserverIP="127.0.0.1";
    private Integer TECSport =9000;
    private String clientName = "PepperConnector";

    private Application pepperApplication = null;
    private Session session = null;

    private void initializeArguments(String TECSserverIP, Integer TECSport, String pepperIP, int pepperPort) {
        this.TECSserverIP = TECSserverIP;
        this.TECSport = TECSport;
        this.pepperIPAddress = pepperIP;
        this.pepperPort = pepperPort;
    }


    public static void main(final String[] args) throws Exception {
        // Create pepperConnector object
        PepperConnector pepperConnector = new PepperConnector();
        TECSconnect tecsConnect = new TECSconnect();
        tecsConnect.connectToTECS(pepperConnector.TECSserverIP, pepperConnector.clientName, 9000);

        // Set arguments to variables
        String url = "tcp://" + "192.168.0.186" + ":" + 9559;
        if (args.length > 3) {
            pepperConnector.initializeArguments(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
            url = "tcp://" + args[2] + ":" + args[3];
        }

        // Initialize connection
        Application application = new Application(args, url);
        application.start();
        pepperConnector.session = application.session();
        System.out.println("Connected to the pepper: " + pepperConnector.session.isConnected());

//        connectToTECS(riseConstants.NAOConnector);


        // Run current application
//        pepperConnector.runBehavior("DELFT_Marnix_Shake-hand-nosensor");
        pepperConnector.runVoiceCommand("Goodmorning");
        pepperConnector.runVoiceCommand(TECSconnect.text2speech);
        pepperConnector.runBehavior(TECSconnect.behavior);

//        application.stop();
//        System.out.println("Application stopped. Connection closed.");
    }

    public void runBehavior(String behavior) {
//        goToPosture("StandZero", 1.0);
        ALBehaviorManager behaviorManager = null;
        try {
            behaviorManager = new ALBehaviorManager(this.session);
            List<String> names = behaviorManager.getInstalledBehaviors();
            System.out.println(names.get(0));
            if(behaviorManager.isBehaviorInstalled(behavior)) {
                behaviorManager.startBehavior(behavior);
            } else {
                System.out.println("You Suck, " + behavior + " is not installed!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void runVoiceCommand(String textToSay) {
//        goToPosture("StandZero", 1.0);
        ALTextToSpeech textToSpeech = null;
        try {
            textToSpeech = new ALTextToSpeech(this.session);
//            List<String> names = textToSpeech.getInstalledBehaviors();
//            System.out.println(names.get(0));
//            if(textToSpeech.isBehaviorInstalled(behavior)) {
//                textToSpeech.startBehavior(behavior);
//            } else {
//                System.out.println("You Suck, " + behavior + " is not installed!");
//            }

            textToSpeech.say(textToSay);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void goToPosture(String newPosture, double speed) {
        try {
            ALRobotPosture posture = new ALRobotPosture(this.session);
            posture.goToPosture(newPosture, (float) speed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

