package ua.mk.berkut.maserver.clients;

import ua.mk.berkut.maserver.Main;
import ua.mk.berkut.maserver.db.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientThread extends Thread {
    PrintWriter out;
    BufferedReader in;
    Socket socket;
    Main main;
    User user;

    public ClientThread(Socket socket, Main main) {
        this.socket = socket;
        this.main = main;
    }

    public User getUser() {
        return user;
    }

    @Override
    public void run() {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            this.in = in; this.out = out;
            if (login()){
                main.addToOnline(this);
                List<User> onlineUsers;// = main.getOnlineUsers();
                //sendList(onlineUsers.stream().filter(u->user.getFriendsIds().contains(u.getId())).collect(Collectors.toList()));
                String message;
                while ((message = in.readLine())!=null) {
                    if ("<<<".equals(message)) { // Show online friends
                        onlineUsers = main.getOnlineUsers();
                        sendList(onlineUsers.stream().filter(u->user.getFriendsIds().contains(u.getId())).collect(Collectors.toList()));
                    } else if (message.startsWith("+++")) {
                        main.processAddFriend(message.substring(3));
                    } else if (">>>exit<<<".equals(message)) {
                        break;
                    } else {
                        main.processMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            main.remove(this);
        }
    }

    private void sendList(List<User> list) {
        out.println("<<<");
        for (User u : list) {
            out.println(u.getId()+";"+u.getUsername()+";"+u.getLogin());
        }
        out.println("<<<");
    }

    private boolean login() throws IOException {
        out.println("Server Ok");
        String line = in.readLine();
        if (line.startsWith("register")) {
            return register(line);
        }
        String[] s = line.split(";");
        // s[0] - "login"
        // s[1] === login
        // s[2] === password
        if (s.length!=3) {
            out.println("Login failed 1");
            return false;
        }
        if (!"login".equals(s[0])) {
            out.println("Login failed 2");
            return false;
        }
        String login = s[1];
        String password = s[2];
        User user = findUser(login, password);
        if (user==null) {
            out.println("Login failed 3");
            return false;
        }
        this.user = user;
        out.println("Login Ok");
        return true;
    }

    private User findUser(String login, String password) {
        return main.findUser(login, password);
    }

    private boolean register(String line) throws IOException {
        //TODO реализовать регистрацию на основе строки line
        User user = main.register(line);
        if (user==null) return false;
        this.user = user;
        return true;
    }

    public void send(String sender, String text) {
        out.println(">>>"+sender+">>>"+text);
    }
}
