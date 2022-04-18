package ch.akop.homesystem.services;


import io.reactivex.rxjava3.subjects.Subject;

import java.util.List;

public interface MessageService {

    MessageService sendMessageToUser(String message, List<String> chatIds);

    Subject<String> getMessages();

    MessageService sendMessageToMainChannel(String message);

    MessageService sendMessageToUser(String message, String chatId);
}
