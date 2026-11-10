package cheng7.mediadesk.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class MediaDeskPrincipal extends User {
    private final Long id;
    private final String displayName;

    public MediaDeskPrincipal(
        Long id,
        String account,
        String passwordHash,
        String displayName,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(account, passwordHash, authorities);
        this.id = id;
        this.displayName = displayName;
    }

    public Long getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
