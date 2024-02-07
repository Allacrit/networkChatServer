import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import server.MyServer;

import java.io.IOException;

public class ServerApp {
    private static final int DEFAULT_PORT = 8186;

    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
        Logger fileLogSystem = Logger.getLogger("file");
        Logger consoleLog = Logger.getLogger("console");
        try {
            new MyServer(DEFAULT_PORT).start();
        } catch (IOException e) {
            fileLogSystem.fatal(e.getMessage());
            consoleLog.fatal(e.getMessage());
        }
    }
}


