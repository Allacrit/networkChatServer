package server;

import org.apache.log4j.Logger;
import server.handlers.ClientHandler;
import server.services.AuthenticationService;
import server.services.impl.SqlAuthenticationServiceImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {

    private final ServerSocket serverSocket;
    private final AuthenticationService authenticationService;
    private final ArrayList<ClientHandler> clients;
    private final Logger consoleLog;
    private final Logger fileLogSystem;
    private final Logger fileLogChat;

    public MyServer(int port) throws IOException {
        consoleLog = Logger.getLogger("console");
        fileLogSystem = Logger.getLogger("file");
        fileLogChat = Logger.getLogger("fileChat");

        serverSocket = new ServerSocket(port);
        authenticationService = new SqlAuthenticationServiceImpl();
        clients = new ArrayList<>();
    }

    public void start() {
        consoleLog.info("Сервер запущен");
        fileLogSystem.info("Сервер запущен");
        while (true) {
            try {
                waitAndProcessNewClientConnection();
            } catch (IOException e) {
                fileLogSystem.warn(e.getMessage());
                consoleLog.warn(e.getMessage());
            }
        }
    }

    private void waitAndProcessNewClientConnection() throws IOException {
        consoleLog.info("Ожидание клиентов");
        Socket socket = serverSocket.accept();
        consoleLog.info("Клиент подключился");
        processClientConnection(socket);
    }

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler handler = new ClientHandler(this, socket);
        handler.handle();
    }


    public synchronized void subscribe(ClientHandler handler) throws IOException {
        clients.add(handler);
        sendRefreshUserList();
    }

    public synchronized void unSubscribe(ClientHandler handler) throws IOException {
        clients.remove(handler);
        sendRefreshUserList();
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void stop() {
        consoleLog.info("ЗАВЕРШЕНИЕ РАБОТЫ");
        fileLogSystem.warn("ЗАВЕРШЕНИЕ РАБОТЫ");
        System.exit(0);
    }

    public synchronized void broadcastMessage(ClientHandler sender, String message) throws IOException {
        for (ClientHandler client : clients) {
            client.sendMessage(sender.getUsername(),message);
        }
    }

    public synchronized void broadcastSystemMessage(ClientHandler handler, String message) throws IOException {
        for (ClientHandler client : clients) {
            if (client == handler) {
                continue;
            }
            client.sendSystemMessage(String.format("%s %s",handler.getUsername(),message));
        }
    }

    public synchronized void sendPrivateMessage(String sender, String recipient, String privateMessage) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(recipient) || client.getUsername().equals(sender)) {
                client.sendPrivateMessage(sender,recipient,privateMessage);
            }
        }
    }

    private synchronized void sendRefreshUserList() throws IOException {
        String userString = "";
        for (ClientHandler client : clients) {
            for (ClientHandler clientHandler : clients) {
                if (clientHandler == client) {
                    continue;
                }
                userString += " " + clientHandler.getUsername();
            }
            client.sendRefreshUserList(userString);
            userString = "";
        }
    }

    public Logger getFileLogChat() {
        return fileLogChat;
    }

    public Logger getConsoleLog() {
        return consoleLog;
    }

    public Logger getFileLogSystem() {
        return fileLogSystem;
    }
}
