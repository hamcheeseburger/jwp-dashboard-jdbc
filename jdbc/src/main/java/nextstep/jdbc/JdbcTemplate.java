package nextstep.jdbc;

import nextstep.jdbc.callback.PreparedStatementCallback;
import nextstep.jdbc.exception.DataAccessException;
import nextstep.jdbc.exception.EmptyResultException;
import nextstep.jdbc.exception.ResultSizeExceedException;
import nextstep.jdbc.setter.ArgumentPreparedStatementSetter;
import nextstep.jdbc.setter.PreparedStatementSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(String sql, Object... args) {
        executeUpdate(connection -> connection.prepareStatement(sql), new ArgumentPreparedStatementSetter(args));
    }

    public void update(String sql, Object... args) {
        executeUpdate(connection -> connection.prepareStatement(sql), new ArgumentPreparedStatementSetter(args));
    }

    public void delete(String sql, Object... args) {
        executeUpdate(connection -> connection.prepareStatement(sql), new ArgumentPreparedStatementSetter(args));
    }

    private void executeUpdate(PreparedStatementCallback pstmtCallback, PreparedStatementSetter pstmtSetter) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = pstmtCallback.makePrepareStatement(conn)) {
            pstmtSetter.setValues(pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("executeUpdate Database Access Failed", e);
            throw new DataAccessException("executeUpdate Database Access Failed");
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
        return executeQuery(connection -> connection.prepareStatement(sql),
                new ArgumentPreparedStatementSetter(args), new RowMapperResultExtract<>(rowMapper));
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
        List<T> results = query(sql, rowMapper, args);
        validateSingleResult(results);
        return results.get(0);
    }

    private <T> void validateSingleResult(List<T> results) {
        if (results.isEmpty()) {
            log.error("queryForObject Result is Empty");
            throw new EmptyResultException("queryForObject Result is Empty");
        }
        if (results.size() > 1) {
            log.error("queryForObject Result Size Over than 1, result size is {}", results.size());
            throw new ResultSizeExceedException("queryForObject Result Size Over than 1");
        }
    }

    private <T> List<T> executeQuery(PreparedStatementCallback pstmtCallback, PreparedStatementSetter pstmtSetter,
                                     RowMapperResultExtract<T> rowMapperResultExtract) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = pstmtCallback.makePrepareStatement(conn)) {
            pstmtSetter.setValues(pstmt);
            return rowMapperResultExtract.execute(pstmt);
        } catch (SQLException e) {
            log.error("executeQuery Data Access Failed!!", e);
            throw new DataAccessException("executeQuery Data Access Failed!!");
        }
    }
}
