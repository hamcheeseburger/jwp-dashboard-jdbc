package nextstep.jdbc.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import nextstep.jdbc.exception.DataAccessException;
import nextstep.jdbc.support.ResultSetExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcTemplateTest {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private JdbcTemplate jdbcTemplate;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws SQLException {
        this.dataSource = mock(DataSource.class);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    void update로_값을_삽입할_수_있다() throws SQLException {
        // given
        final String sql = "insert into test values(?, ?, ?)";

        when(preparedStatement.executeUpdate()).thenReturn(1);

        // when
        final int actual = jdbcTemplate.update(sql, 1, 2, 3);

        // then
        assertAll(
                () -> assertThat(actual).isOne(),
                () -> verify(this.preparedStatement).setObject(1, 1),
                () -> verify(this.preparedStatement).setObject(2, 2),
                () -> verify(this.preparedStatement).setObject(3, 3),
                () -> verify(this.preparedStatement).close(),
                () -> verify(this.connection).close()
        );
    }

    @Test
    void queryForObject로_값을_불러올_수_있다() throws SQLException {
        // given
        final String sql = "select id, account from test where id=?";

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("account")).thenReturn("corinne");

        // when
        final TestUser actual = jdbcTemplate.queryForObject(sql, getResultSetExecutor(), 1L);

        // then
        assertAll(
                () -> assertThat(actual).usingRecursiveComparison()
                        .isEqualTo(new TestUser(1L, "corinne")),
                () -> verify(preparedStatement).setObject(1, 1L),
                () -> verify(resultSet).close(),
                () -> verify(preparedStatement).close(),
                () -> verify(connection).close()
        );
    }

    @Test
    void queryForObject_수행시_레코드가_없으면_null을_반환한다() throws SQLException {
        // given
        final String sql = "select id, account from test";

        when(resultSet.next()).thenReturn(false);

        // when
        final TestUser testUser = jdbcTemplate.queryForObject(sql, getResultSetExecutor());

        // then
        assertThat(testUser).isNull();
    }

    @Test
    void queryForObject_수행_시_레코드가_2개_이상이면_예외를_반환한다() throws SQLException {
        // given
        final String sql = "select id, account from test";

        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("account")).thenReturn("corinne");

        // when, then
        final ResultSetExtractor<TestUser> resultSetExecutor = getResultSetExecutor();
        assertThatThrownBy(() -> jdbcTemplate.queryForObject(sql, resultSetExecutor))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void queryList로_값을_불러올_수_있다() throws SQLException {
        // given
        final String sql = "select id, account from test where account=?";

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("account")).thenReturn("corinne");

        // when
        final List<TestUser> actual = jdbcTemplate.queryForList(sql, getResultSetExecutor(), "corinne");

        // then
        assertAll(
                () -> assertThat(actual).hasSize(1),
                () -> verify(preparedStatement).setObject(1, "corinne"),
                () -> verify(resultSet).close(),
                () -> verify(preparedStatement).close(),
                () -> verify(connection).close()
        );
    }

    @Test
    void 커넥션_점유_실패시_예외가_발생한다() throws SQLException {
        // given
        final String sql = "select id, account from test where account=?";

        when(dataSource.getConnection())
                .thenThrow(new DataAccessException());
        final ResultSetExtractor<TestUser> resultSetExecutor = getResultSetExecutor();

        // when, then
        assertAll(
                () -> assertThatThrownBy(() -> jdbcTemplate.queryForObject(sql, resultSetExecutor, "corinne"))
                        .isInstanceOf(DataAccessException.class),
                () -> verify(preparedStatement, never()).setString(1, "corinne"),
                () -> verify(preparedStatement, never()).close(),
                () -> verify(connection, never()).close()
        );
    }

    private ResultSetExtractor<TestUser> getResultSetExecutor() {
        return rs -> {
            final long id = rs.getLong("id");
            final String account = rs.getString("account");
            return new TestUser(id, account);
        };
    }
}
