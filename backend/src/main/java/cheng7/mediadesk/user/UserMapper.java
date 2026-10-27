package cheng7.mediadesk.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT id, account, password_hash AS passwordHash, display_name AS displayName
        FROM users
        WHERE account = #{account}
    """)
    UserAccount findByAccount(@Param("account") String account);
}
