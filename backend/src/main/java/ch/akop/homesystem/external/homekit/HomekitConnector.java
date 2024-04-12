package ch.akop.homesystem.external.homekit;

import io.github.hapjava.server.HomekitAuthInfo;
import io.github.hapjava.server.impl.HomekitServer;
import io.github.hapjava.server.impl.crypto.HAPSetupCodeUtils;
import io.quarkus.runtime.Startup;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
@Startup
public class HomekitConnector {

  @PostConstruct
  @Transactional
  void setupWebSocketListener() throws IOException {
    var homekit = new HomekitServer(InetAddress.getByName("192.168.1.105"), 9123);
    var setupUrl = HAPSetupCodeUtils.getSetupURI("1234", "1234", 2);
    log.info(setupUrl);
    var test = new HomekitAuthInfo() {
      @Override
      public String getPin() {
        return "1234";
      }

      @Override
      public String getMac() {
        return "1234";
      }

      @Override
      public BigInteger getSalt() {
        return BigInteger.ONE;
      }

      @Override
      public byte[] getPrivateKey() {
        return new byte[0];
      }

      @Override
      public void removeUser(String username) {

      }

      @Override
      public byte[] getUserPublicKey(String username) {
        return new byte[0];
      }
    };
    var bridge = homekit.createBridge(test, "test", 1, "me", "pc", "abc", "abc", "abc");
    bridge.start();
    bridge.refreshAuthInfo();
  }

}
