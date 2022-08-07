package ch.akop.homesystem.services;


import io.reactivex.rxjava3.subjects.Subject;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.List;

public interface MessageService {

    MessageService sendMessageToUser(@Nullable String message, @NonNull List<String> chatIds);

    Subject<String> getMessages();

    MessageService sendMessageToMainChannel(@Nullable String message);

    MessageService sendMessageToUser(@Nullable String message, @NonNull String chatId);
}
