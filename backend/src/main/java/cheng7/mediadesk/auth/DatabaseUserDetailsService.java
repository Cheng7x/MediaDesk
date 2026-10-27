package cheng7.mediadesk.auth;

import cheng7.mediadesk.user.UserAccount;
import cheng7.mediadesk.user.UserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;

    public DatabaseUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String account = username
            .replaceAll("^ +| +$", "")
            .toLowerCase(Locale.ROOT);

        if (!account.matches("[a-z0-9_]{3,32}")) {
            throw new UsernameNotFoundException("账号或密码错误");
        }

        UserAccount user = userMapper.findByAccount(account);

        if (user == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }

        return User.withUsername(user.getAccount())
            .password(user.getPasswordHash())
            .authorities(Collections.emptyList())
            .build();
    }
}
