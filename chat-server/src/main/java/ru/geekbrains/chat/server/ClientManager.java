package ru.geekbrains.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public final static Map<String,ClientManager> clients = new HashMap<>();

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.put(name,this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }


    }

    @Override
    public void run() {
        String massageFromClient;

        while (socket.isConnected()) {
            try {
                massageFromClient = bufferedReader.readLine();
                /*if (massageFromClient == null){
                    // для  macOS
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }*/
                broadcastMessage(massageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void broadcastMessage(String message) {
        String[] word = message.split(" ");
        String str = String.join(" ", word);
        if (clients.containsKey(word[1].substring(1))&&!word[1].substring(1).equals(name)){
            try {
            clients.get(word[1].substring(1)).bufferedWriter.write(str.replaceAll(" " + word[1], ""));
            clients.get(word[1].substring(1)).bufferedWriter.newLine();
            clients.get(word[1].substring(1)).bufferedWriter.flush();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }else {
            for (Map.Entry<String, ClientManager> client : clients.entrySet()) {
                try {
                    if (!client.getKey().equals(name)) {
                        client.getValue().bufferedWriter.write(message);
                        client.getValue().bufferedWriter.newLine();
                        client.getValue().bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient(){
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

}
