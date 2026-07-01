package br.com.chatbot.chatbot_api.entity;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;

public class PGvectorUserType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        return x == y || (x != null && y != null && Arrays.equals(x.getVector(), y.getVector()));
    }

    @Override
    public int hashCode(PGvector x) {
        return Arrays.hashCode(x.getVector());
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        var value = rs.getString(position);
        if (value == null) {
            return null;
        }
        var cleaned = value.replaceAll("[\\[\\](){}]", "");
        var parts = cleaned.split(",");
        var vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return new PGvector(vector);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        var vectorStr = Arrays.toString(value.getVector());
        st.setObject(index, vectorStr, Types.OTHER);
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) return null;
        return new PGvector(value.getVector().clone());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        return deepCopy(value);
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        return deepCopy((PGvector) cached);
    }

    @Override
    public PGvector replace(PGvector detached, PGvector managed, Object owner) {
        return deepCopy(detached);
    }
}
