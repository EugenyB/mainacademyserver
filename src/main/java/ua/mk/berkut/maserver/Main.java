package ua.mk.berkut.maserver;

import ua.mk.berkut.maserver.clients.ClientThread;
import ua.mk.berkut.maserver.dao.FriendDAO;
import ua.mk.berkut.maserver.dao.UserDAO;
import ua.mk.berkut.maserver.db.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {
    public final static String SEPARATOR = ";";

    Properties properties = new Properties();
    Connection connection;
    List<User> users;
    List<ClientThread> onlineUserThreads;
    UserDAO userDAO;
    FriendDAO friendDAO;

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private void run() throws Exception {
        startServer();
        users = userDAO.getAllUsers();
        onlineUserThreads = new ArrayList<>();
        printUsers(users);
        ServerSocket serverSocket = new ServerSocket(
                Integer.parseInt(properties.getProperty("port", "1234"))
        );
        for (; ; ) {
            Socket socket = serverSocket.accept();
            new ClientThread(socket, this).start();
        }
    }

    private void printUsers(List<User> users) {
        users.forEach(System.out::println);
    }

    private void startServer() throws IOException, SQLException {

        properties.load(Files.newBufferedReader(Paths.get("chat.cfg")));
        connection = DriverManager.getConnection(
                properties.getProperty("url"),
                properties);
        userDAO = new UserDAO(connection);
        friendDAO = new FriendDAO(connection);
    }

    public void remove(ClientThread clientThread) {
        //TODO реализовать отключение клиента, завершившего работу
        onlineUserThreads.remove(clientThread);
    }

    public User findUser(String login, String password) {
        User user = userDAO.findUser(login, password);
        if (user != null) {
            user.setFriendsIds(friendDAO.getFriedsFor(user.getId()));
        }
        return user;
    }

    public List<User> getOnlineUsers() {
        return onlineUserThreads
                .stream()
                .map(ClientThread::getUser)
                .collect(Collectors.toList());
    }

    public synchronized void processMessage(String message) {
        String[] split = message.split(SEPARATOR);
        if (split.length!=3) return;
        String receiver = split[0];
        String sender = split[1];
        String text = split[2];
        Optional<ClientThread> clientThread = onlineUserThreads
                .stream()
                .filter(t -> t.getUser().getLogin().equals(receiver))
                .findFirst();
        clientThread.ifPresent(thread -> thread.send(sender, text));
    }

    public synchronized void addToOnline(ClientThread clientThread) {
        onlineUserThreads.add(clientThread);
    }

    public User register(String line) {
        try {
            String[] s = line.split(" ");
            String login = s[1];
            String password = s[2];
            String username = s[3];
            String dateStr = s[4];
            String[] split = dateStr.split("\\D");
            LocalDate birthday = LocalDate.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
            String city = s[5];
            User user = new User(login, password, username, birthday, city, "");
            user = userDAO.addUser(user);
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
