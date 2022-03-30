package ch.akop.homesystem.services;


import io.reactivex.rxjava3.subjects.Subject;

public interface MessageService {

    MessageService sendMessageToUser(String message);

    Subject<String> getMessages();

}
