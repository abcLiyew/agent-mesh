package com.esdllm.agentmesh.config;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgreSqlJsonbTypeHandler extends JacksonTypeHandler {

    public PostgreSqlJsonbTypeHandler(Class<?> type) {
        super(type);
    }

    public PostgreSqlJsonbTypeHandler() {
        super(Object.class);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("jsonb");
        try {
            pGobject.setValue(super.toJson(parameter)); // 使用父类的 Jackson 序列化
        } catch (Exception e) {
            throw new SQLException("Error converting object to JSONB", e);
        }
        ps.setObject(i, pGobject);
    }
    
    // getNullableResult 方法通常不需要重写，因为 PGobject  toString() 会返回 JSON 字符串，
    // 父类 JacksonTypeHandler 能直接处理字符串反序列化。
    // 但如果数据库返回的是 PGobject 实例而非字符串，可能需要特殊处理：
    /*
    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object obj = rs.getObject(columnName);
        if (obj instanceof PGobject) {
            return super.parseObject(((PGobject) obj).getValue());
        }
        return super.getNullableResult(rs, columnName);
    }
    */
}