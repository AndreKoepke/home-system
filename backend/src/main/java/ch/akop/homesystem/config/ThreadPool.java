package ch.akop.homesystem.config;

import io.quarkus.runtime.Startup;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;

public class ThreadPool {

  @Startup // Instanciated at start-time
  @ApplicationScoped // Only one instance for all your app (Singleton)
  public ScheduledExecutorService managedCustomExecutor() {
    return Executors.newScheduledThreadPool(2);
  }
}
