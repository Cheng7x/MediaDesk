package cheng7.mediadesk.diagnostics;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatabaseProbeMapper {

    @Select("SELECT 1")
    int selectOne();
}
