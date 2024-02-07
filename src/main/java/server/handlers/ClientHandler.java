package server.handlers;

import server.MyServer;
import server.services.AuthenticationService;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientHandler {
    private static final String AUTH_CMD_PREFIX = "/auth"; // + login + password
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // + username
    private static final String AUTHERR_CMD_PREFIX = "/autheer"; // +error message
    private static final String CLIENT_MSG_CMD_PREFIX = "/cMsg"; // + msg
    private static final String SERVER_MSG_CMD_PREFIX = "/sMsg"; // + msg
    private static final String PRIVATE_MSG_CMD_PREFIX = "/pm"; // + username + msg
    private static final String STOP_SERVER_CMD_PREFIX = "/stop";
    private static final String END_CLIENT_CMD_PREFIX = "/end";
    private static final String REFRESH_USER_LIST_PREFIX = "/refresh";
    private static final String REG_USER_CMD_PREFIX = "/reg"; // + login + password + username
    private static final String REGOK_USER_CMD_PREFIX = "/regok"; // + ок
    private static final String REGERR_USER_CMD_PREFIX = "/regerr"; // + error registration

    private MyServer myServer;
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private AuthenticationService auth;

    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        clientSocket = socket;
    }

    public void handle() throws IOException {
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());

        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                myServer.getFileLogSystem().warn(e.getMessage());
                myServer.getConsoleLog().warn(e.getMessage());
            }
        }).start();
    }

    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();
            myServer.getFileLogChat().info(String.format("%s : %s",username,message));
            String typeMessage = message.split("\\s+")[0];

            switch (typeMessage) {
                case STOP_SERVER_CMD_PREFIX -> processingStopServer();

                case END_CLIENT_CMD_PREFIX -> closeConnection();

                case PRIVATE_MSG_CMD_PREFIX -> processingPrivateMessage(message);

                case CLIENT_MSG_CMD_PREFIX -> processingClientMessage(message);

                default -> errorData(message);
            }
        }
    }

    private void closeConnection() throws IOException {
        myServer.broadcastSystemMessage(this, "покинул чат");
        myServer.getFileLogChat().info(String.format("Пользователь %s покинул чат", username));
        myServer.getConsoleLog().info(String.format("Пользователь %s покинул чат", username));
        myServer.unSubscribe(this);
        clientSocket.close();
    }

    private void processingStopServer() {
        myServer.stop();
        myServer.getFileLogSystem().warn(String.format("Пользователь %s завершил работу сервера", username));
        myServer.getConsoleLog().warn(String.format("Пользователь %s завершил работу сервера", username));
    }

    private void processingPrivateMessage(String message) throws IOException {
        String[] privateMessage = message.split("\\s+", 3);
        myServer.sendPrivateMessage(username, privateMessage[1], privateMessage[2]);
        myServer.getConsoleLog().info(String.format("Приватное сообщение| %s -> %s: %s", username, privateMessage[1], privateMessage[2]));
    }

    private void processingClientMessage(String message) throws IOException {
        String[] generalMessage = message.split("\\s+", 2);
        myServer.broadcastMessage(this, generalMessage[1]);
        myServer.getConsoleLog().info(String.format("Сообщение чата|  %s: %s  |", username, generalMessage[1]));
    }

    private void errorData(String message) {
        myServer.getFileLogSystem().trace(String.format("Данные от %s: %s", username, message));
    }

    public void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", CLIENT_MSG_CMD_PREFIX, sender, message));
    }

    public void sendPrivateMessage(String sender, String recipient, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s %s", PRIVATE_MSG_CMD_PREFIX, sender, recipient, message));
    }

    public void sendSystemMessage(String message) throws IOException {
        out.writeUTF(String.format("%s %s", SERVER_MSG_CMD_PREFIX, message));
    }

    public void sendRefreshUserList(String client) throws IOException {
        out.writeUTF(String.format("%s %s", REFRESH_USER_LIST_PREFIX, client));
    }

    private void authentication() throws IOException {

        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthentication(message);
                if (isSuccessAuth) {
                    break;
                }
            } else if (message.startsWith(END_CLIENT_CMD_PREFIX)) {
                clientSocket.close();
                myServer.getConsoleLog().info("Не авторизованный клиент отключился");
            }else if (message.startsWith(REG_USER_CMD_PREFIX)) {
                processRegistration(message);
            } else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Неверная команда аутентификации");
                myServer.getConsoleLog().error("Неверная команда аутентификации");
            }
        }
    }

    private void processRegistration(String message) throws IOException {
        String[] parts = message.split("\\s+");
        if (parts.length != 4) {
            out.writeUTF(REGERR_USER_CMD_PREFIX + " Пробелов быть не должно");
            myServer.getConsoleLog().error("Пробелы в логине, пароле, имени");
        }
        auth = myServer.getAuthenticationService();
        String registration = auth.checkCreateNewUser(parts);

        if (("login").equals(registration)) {
            out.writeUTF(String.format("%s %s",REGERR_USER_CMD_PREFIX,"Логин занят"));
            myServer.getConsoleLog().warn("Ошибка при регистрации. Логин занят");

        } else if (("name").equals(registration)) {
            out.writeUTF(String.format("%s %s",REGERR_USER_CMD_PREFIX,"Имя занято"));
            myServer.getConsoleLog().warn("Ошибка при регистрации. Имя занято");
        } else {
            out.writeUTF(REGOK_USER_CMD_PREFIX);
        }
    }

    private boolean processAuthentication(String message) throws IOException {
        String[] parts = message.split("\\s+");

        if (parts.length != 3) {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Пробелов быть не должно");
            myServer.getConsoleLog().warn("Пробелы в логине и пароле");
            return false;
        }

        String login = parts[1];
        String password = parts[2];
        auth = myServer.getAuthenticationService();
        username = auth.getUsernameByLoginAndPassword(login, password);

        if (username != null) {

            if (myServer.isUsernameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " " + "Пользователь уже в сети");
                myServer.getConsoleLog().warn(String.format("Попытка входа авторизованным пользователем: %s", username));
                return false;
            }
            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.subscribe(this);
            myServer.getConsoleLog().info(String.format("Пользователь %s подключился к чату", username));
            myServer.getFileLogChat().info(String.format("Пользователь %s подключился к чату", username));
            myServer.broadcastSystemMessage(this, "подключился к чату");
            return true;

        } else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Проверьте логин и пароль");
            myServer.getConsoleLog().warn("Неверная команда аутентификации");
            return false;
        }
    }

    public String getUsername() {
        return username;
    }
}
