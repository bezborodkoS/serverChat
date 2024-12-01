package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ChatServer {
    private  static final int PORT = 5000;
    private static Map<String, SocketChannel> clients = new HashMap<>();
    private static Map<SocketChannel,String> reverseClients = new HashMap<>();

    public static void starterServer(){
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            serverSocket.bind(new InetSocketAddress(PORT));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Сервер чата запущен на порту " + PORT);

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(selector, serverSocket);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleAccept(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientChannel = serverSocket.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        String welcomeMessage = "Добро пожаловать. Введите свое имя пользователя:\n";
        clientChannel.write(ByteBuffer.wrap(welcomeMessage.getBytes()));
        System.out.println("Новое соединение принято: " + clientChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            disconnectClient(clientChannel);
            return;
        }

        buffer.flip();
        String message = new String(buffer.array(), 0, bytesRead).trim();

        if (!reverseClients.containsKey(clientChannel)) {
            if (clients.containsKey(message)) {
                clientChannel.write(ByteBuffer.wrap("Имя пользователя уже занято. Попробуйте еще раз:\n".getBytes()));
            } else {
                clients.put(message, clientChannel);
                reverseClients.put(clientChannel, message);
                translationMessage("Пользователь " + message + " присоединился к чату.\n", null);
                System.out.println("Пользователь " + message + " зарегистрированный.");
            }
        }
    }

    private static void translationMessage(String message, String excludeUser) {
        for (Map.Entry<String, SocketChannel> client : clients.entrySet()) {
            if (!client.getKey().equals(excludeUser)) {
                try {
                    client.getValue().write(ByteBuffer.wrap(message.getBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void disconnectClient(SocketChannel clientChannel) {
        try {
            String username = reverseClients.get(clientChannel);
            if (username != null) {
                clients.remove(username);
                reverseClients.remove(clientChannel);
                translationMessage("Пользователь " + username + " вышел из чата.\n", null);
                System.out.println("Пользователь " + username + " отключился.");
            }
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
