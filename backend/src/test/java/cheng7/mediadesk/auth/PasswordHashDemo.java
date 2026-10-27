package cheng7.mediadesk.auth;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;

public class PasswordHashDemo {

    public static void main(String[] args) {
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String hash = encoder.encode("MediaDesk-demo-27!");
        System.out.println(hash);
    }
}
